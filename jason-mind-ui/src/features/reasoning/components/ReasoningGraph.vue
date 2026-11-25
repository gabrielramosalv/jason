<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { MarkerType, VueFlow } from '@xyflow/vue'
import type { Edge, Node, NodeMouseEvent } from '@xyflow/vue'
import '@xyflow/vue/base.css'
import type { Plan } from '@/features/inspector/types/state.types'
import { Icon } from '@/shared/ui'
import { useInspectorStore } from '@/stores/inspector.store'
import { layoutGraph } from '../domain/graph-layout'
import { buildGraph } from '../domain/reasoning-graph'
import type { GraphChipNode, GraphEdge as GraphEdgeModel, GraphNode, GraphStepNode, LifecycleTarget } from '../domain/reasoning-graph'
import { STEPS } from '../domain/step-catalog'
import type { ReasoningStep } from '../types/reasoning.types'
import GraphChip from './GraphChip.vue'
import GraphEdge from './GraphEdge.vue'
import GraphStep from './GraphStep.vue'
import GraphTools from './GraphTools.vue'
import StepHover from './StepHover.vue'

/**
 * O ciclo como grafo: uma entidade por chip; uma etapa de seleção aponta para o escolhido (passar o mouse nela explica a
 * escolha e o clique lista os outros candidatos); as linhas entre chips são causas, uma por verbo.
 * O clique num chip abre o Life cycle na última ação que ele recebeu no ciclo; o clique no ícone de uma linha, na ocorrência dela.
 * O botão Flow percorre o ciclo na ordem em que as coisas aconteceram, uma linha ou um chip por vez.
 */
const props = defineProps<{ steps: ReasoningStep[]; plans?: ReadonlyMap<string, Plan>; focusEntity?: string | null }>()
const emit = defineEmits<{
  lifecycle: [node: GraphChipNode]
  lifecycleAt: [target: LifecycleTarget]
  candidates: [node: GraphStepNode, anchor: DOMRect]
}>()

const inspector = useInspectorStore()
const graph = computed(() => buildGraph(props.steps, props.plans, (goal) => inspector.intentionOfGoal(goal)))
const positions = computed(() => layoutGraph(graph.value))
const signature = computed(() => graph.value.nodes.map((n) => n.id).join('|'))

// ---- o fluxo, passo a passo
/** Um passo da animação: uma linha (com as pontas dela) ou um chip que não tem linha. */
type Frame = { order: number; edge?: GraphEdgeModel; node?: GraphNode }
const frames = computed<Frame[]>(() => {
  const linked = new Set(graph.value.edges.flatMap((e) => [e.source, e.target]))
  return [
    ...graph.value.edges.map((edge) => ({ order: edge.order, edge })),
    ...graph.value.nodes.filter((n) => !linked.has(n.id)).map((node) => ({ order: node.order, node })),
  ].sort((a, b) => a.order - b.order)
})
const cursor = ref(-1)
const playing = ref(false)
const active = computed(() => cursor.value >= 0)
const STEP_MS = 1100
let timer: number | undefined

const stop = () => {
  playing.value = false
  cursor.value = -1
}
const advance = () => {
  if (cursor.value < frames.value.length - 1) cursor.value++
  else playing.value = false
}
const start = () => {
  if (!frames.value.length) return
  cursor.value = 0
  playing.value = true
}
const toggle = () => {
  if (!playing.value && cursor.value >= frames.value.length - 1) cursor.value = 0
  playing.value = !playing.value
}
const step = (delta: 1 | -1) => {
  playing.value = false
  cursor.value = Math.min(frames.value.length - 1, Math.max(0, cursor.value + delta))
}
watch(playing, (on) => {
  window.clearInterval(timer)
  if (on) timer = window.setInterval(advance, STEP_MS)
})
// outro ciclo, outro grafo: a animação recomeça do zero
watch(signature, stop)
onBeforeUnmount(() => window.clearInterval(timer))

const nowFrame = computed(() => (active.value ? frames.value[cursor.value] : undefined))
const reached = computed(() => {
  const nodes = new Set<string>()
  const edges = new Set<string>()
  if (!active.value) return { nodes, edges }
  for (const f of frames.value.slice(0, cursor.value + 1)) {
    if (f.edge) {
      edges.add(f.edge.id)
      nodes.add(f.edge.source)
      nodes.add(f.edge.target)
    } else if (f.node) nodes.add(f.node.id)
  }
  return { nodes, edges }
})
type Flow = 'future' | 'past' | 'now' | null
const flowOfNode = (id: string): Flow => {
  if (!active.value) return null
  const f = nowFrame.value
  if (f?.edge ? f.edge.source === id || f.edge.target === id : f?.node?.id === id) return 'now'
  return reached.value.nodes.has(id) ? 'past' : 'future'
}
const flowOfEdge = (id: string): Flow => {
  if (!active.value) return null
  if (nowFrame.value?.edge?.id === id) return 'now'
  return reached.value.edges.has(id) ? 'past' : 'future'
}

const labelOf = (id: string) => {
  const n = graph.value.nodes.find((x) => x.id === id)
  const text = n?.kind === 'step' ? n.title : (n?.chip.text ?? id)
  return text.length > 34 ? `${text.slice(0, 33)}…` : text
}
const caption = computed(() => {
  const f = nowFrame.value
  if (!f) return ''
  if (f.node) return labelOf(f.node.id)
  const e = f.edge!
  const step = STEPS.find((s) => s.step === (e.badge?.at.step.step ?? (graph.value.nodes.find((n) => n.id === e.target) as GraphStepNode | undefined)?.step.step))?.title
  // quando quem age já é a etapa, o nome dela abre a frase e não se repete
  const prefix = step && !e.source.startsWith('step:') ? `${step}: ` : ''
  return `${prefix}${labelOf(e.source)} ${e.badge?.label ?? 'is the subject of'} ${labelOf(e.target)}`
})

const nodes = computed<Node[]>(() =>
  graph.value.nodes.map((n) => ({
    id: n.id,
    type: n.kind === 'step' ? 'step' : 'chip',
    position: positions.value.get(n.id) ?? { x: 0, y: 0 },
    data: { node: n, focused: n.kind === 'chip' && n.id === props.focusEntity, flow: flowOfNode(n.id) },
    draggable: false,
    selectable: false,
    connectable: false,
  })),
)
const edges = computed<Edge[]>(() =>
  graph.value.edges.map((e) => {
    const flow = flowOfEdge(e.id)
    return {
      id: e.id,
      source: e.source,
      target: e.target,
      type: 'badged',
      data: { badge: e.badge, index: e.index, count: e.count, flow, open: e.badge ? () => emit('lifecycleAt', e.badge!.at) : undefined },
      class: `mi-gedge-${e.role}${flow ? ` mi-gflow-${flow}` : ''}`,
      selectable: false,
      markerEnd: { type: MarkerType.ArrowClosed, color: '#9aa1ae', width: 14, height: 14 },
    }
  }),
)

const wrap = ref<HTMLElement | null>(null)
const hover = ref<{ node: GraphStepNode; x: number; y: number } | null>(null)

function onNodeClick({ event, node }: NodeMouseEvent) {
  const n = node.data.node as GraphNode
  const e = event as MouseEvent
  if (n.kind === 'step') {
    const target = e.target as HTMLElement
    if (n.options.length) emit('candidates', n, (target.closest('.vue-flow__node') ?? target).getBoundingClientRect())
    return
  }
  emit('lifecycle', n)
}

/** O cartão de explicação aparece ao lado da etapa e não passa da borda do painel (uns 360 x 320 px). */
function onEnter({ event, node }: NodeMouseEvent) {
  const n = node.data.node as GraphNode
  if (n.kind !== 'step') return
  const box = ((event as MouseEvent).target as HTMLElement).closest('.vue-flow__node')?.getBoundingClientRect()
  const r = wrap.value?.getBoundingClientRect()
  if (!box || !r) return
  const x = Math.min(box.right - r.left + 8, r.width - 372)
  const y = Math.min(Math.max(4, box.top - r.top), Math.max(4, r.height - 330))
  hover.value = { node: n, x: Math.max(4, x), y }
}
</script>

<template>
  <div ref="wrap" class="mi-graph" style="position: relative; flex: 1 1 0; min-height: 0" @pointerdown.stop>
    <div v-if="!graph.nodes.length" class="mi-emptybig" role="status" style="margin: auto">No reasoning in this cycle</div>
    <VueFlow
      v-else
      :nodes="nodes"
      :edges="edges"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :min-zoom="0.2"
      :max-zoom="2"
      fit-view-on-init
      attribution-position="bottom-left"
      @node-click="onNodeClick"
      @node-mouse-enter="onEnter"
      @node-mouse-leave="hover = null"
      @move-start="hover = null"
    >
      <template #node-chip="p"><GraphChip v-bind="p" /></template>
      <template #node-step="p"><GraphStep v-bind="p" /></template>
      <template #edge-badged="p"><GraphEdge v-bind="p" /></template>
      <GraphTools :signature="signature" />
    </VueFlow>
    <div v-if="graph.nodes.length" class="mi-gflowbar" role="group" aria-label="Flow">
      <button v-if="!active" type="button" class="mi-btn" title="Walk through the cycle in the order things happened" style="gap: var(--sp-4); border-color: transparent; padding: 0 var(--sp-8)" @click="start"><Icon name="play" />Flow</button>
      <template v-else>
        <button type="button" class="mi-btn mi-icon mi-zbtn" title="Previous" aria-label="Previous" :disabled="cursor <= 0" @click="step(-1)"><Icon name="previous" /></button>
        <button type="button" class="mi-btn mi-icon mi-zbtn" :title="playing ? 'Pause' : 'Play'" :aria-label="playing ? 'Pause' : 'Play'" @click="toggle"><Icon :name="playing ? 'pause' : 'play'" /></button>
        <button type="button" class="mi-btn mi-icon mi-zbtn" title="Next" aria-label="Next" :disabled="cursor >= frames.length - 1" @click="step(1)"><Icon name="next" /></button>
        <span class="mi-gflowcount">{{ cursor + 1 }} / {{ frames.length }}</span>
        <span class="mi-gflowcap" :title="caption">{{ caption }}</span>
        <button type="button" class="mi-btn mi-icon mi-zbtn" title="Close flow" aria-label="Close flow" @click="stop"><Icon name="close-2" /></button>
      </template>
    </div>
    <div v-if="hover" class="mi-ghovercard" :style="{ left: `${hover.x}px`, top: `${hover.y}px` }"><StepHover :node="hover.node" :plans="plans" /></div>
  </div>
</template>
