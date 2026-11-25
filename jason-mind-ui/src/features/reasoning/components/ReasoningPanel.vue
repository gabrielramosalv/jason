<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { CONCEPT_COLORS, CONCEPT_ICON } from '@/shared/domain/entities'
import MoreDialog from '@/shared/components/MoreDialog.vue'
import type { MoreItem } from '@/shared/components/MoreDialog.vue'
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { useLifecycleStore } from '@/stores/lifecycle.store'
import { useSessionStore } from '@/stores/session.store'
import { useProgram } from '@/features/inspector/composables/useProgram'
import { useReasoning } from '../composables/useReasoning'
import { PHASES, STEPS } from '../domain/step-catalog'
import type { StepInfo } from '../domain/step-catalog'
import { chipsOf } from '../domain/step-chips'
import type { StepChip } from '../domain/step-chips'
import type { PhaseName, ReasoningStep } from '../types/reasoning.types'
import ExplainDialog from './ExplainDialog.vue'

/** Corpo do painel Reasoning: o ciclo em três fases e nove passos, com zoom, arraste e explicação por chip. */
const session = useSessionStore()
const layout = useLayoutStore()
const lifecycle = useLifecycleStore()
const { selectedAgent, shown } = storeToRefs(session)

const cycle = computed(() => shown.value?.cycle ?? null)
const enabled = computed(() => layout.visible.reason)
const { data, error } = useReasoning(selectedAgent, cycle, enabled)

const showIdle = ref(true)
const iteration = reactive<Record<string, number | undefined>>({})
const occurrence = reactive<Record<string, number | undefined>>({})
const explained = ref<{ chip: StepChip; step: StepInfo } | null>(null)
const anchor = ref({ left: 12, bottom: 12, maxHeight: 400 })

const steps = computed<ReasoningStep[]>(() => data.value?.steps ?? [])
watch([selectedAgent, cycle], () => {
  for (const k of Object.keys(iteration)) delete iteration[k]
  for (const k of Object.keys(occurrence)) delete occurrence[k]
  explained.value = null
})

const iterationsOf = (p: PhaseName) => [...new Set(steps.value.filter((s) => s.phase === p).map((s) => s.iteration))].sort((a, b) => a - b)
const currentIteration = (p: PhaseName) => iteration[p] ?? iterationsOf(p).at(-1) ?? 0
const occurrencesOf = (info: StepInfo) => steps.value.filter((s) => s.step === info.step && s.iteration === currentIteration(info.phase)).sort((a, b) => a.ordinal - b.ordinal)
const selectedStep = (info: StepInfo) => {
  const list = occurrencesOf(info)
  return list.find((s) => s.ordinal === occurrence[info.step]) ?? list.at(-1) ?? null
}

const { plans } = useProgram()
const plansById = computed(() => new Map(plans.value.map((p) => [p.id, p])))

const phases = computed(() =>
  PHASES.map((p) => ({
    ...p,
    its: iterationsOf(p.phase),
    steps: STEPS.filter((s) => s.phase === p.phase)
      .map((info) => {
        const step = selectedStep(info)
        const chips = step ? chipsOf(step, plansById.value) : []
        return { info, step, chips, occs: occurrencesOf(info), idle: !chips.length }
      })
      .filter((s) => showIdle.value || !s.idle),
  })).filter((p) => p.steps.length),
)

// zoom e arraste
const scroller = ref<HTMLElement | null>(null)
const canvas = ref<HTMLElement | null>(null)
const zoom = ref(1)
const manual = ref(false)
const natural = reactive({ w: 0, h: 0 })
let observer: ResizeObserver | null = null

function fit() {
  const sc = scroller.value
  const cv = canvas.value
  if (!sc || !cv) return
  // todas as caixas de etapa têm a largura da maior: mede cada uma no tamanho natural e aplica a comum
  cv.style.removeProperty('--step-w')
  cv.style.removeProperty('--step-h')
  const boxes = [...cv.querySelectorAll<HTMLElement>('.mi-stepbox')]
  const widest = Math.max(0, ...boxes.map((el) => el.offsetWidth))
  if (widest) cv.style.setProperty('--step-w', `${widest}px`)
  // e a altura da maior, para os passos formarem uma fileira regular
  const tallest = Math.max(0, ...boxes.map((el) => el.offsetHeight))
  if (tallest) cv.style.setProperty('--step-h', `${tallest}px`)
  measureLanes()
  // o svg dos conectores ainda tem o tamanho da medida anterior e inflaria o scrollWidth: mede sem ele
  const svg = cv.querySelector<SVGElement>('.mi-conn')
  if (svg) svg.style.display = 'none'
  natural.w = cv.scrollWidth
  natural.h = cv.scrollHeight
  if (svg) svg.style.display = ''
  if (!manual.value && natural.w) zoom.value = Math.min(1, (sc.clientWidth - 24) / natural.w)
}
onMounted(() => {
  observer = new ResizeObserver(fit)
  if (scroller.value) observer.observe(scroller.value)
  if (canvas.value) observer.observe(canvas.value)
})
onBeforeUnmount(() => observer?.disconnect())
watch(phases, async () => {
  await nextTick()
  fit()
})

const setZoom = (z: number) => {
  manual.value = true
  zoom.value = Math.min(2.5, Math.max(0.2, z))
}
function onWheel(e: WheelEvent) {
  e.preventDefault()
  setZoom(zoom.value * Math.exp(-e.deltaY * 0.0015))
}
let drag: { x: number; y: number; l: number; t: number } | null = null
function onDown(e: PointerEvent) {
  const sc = scroller.value
  if (!sc || (e.target as HTMLElement).closest('button')) return
  drag = { x: e.clientX, y: e.clientY, l: sc.scrollLeft, t: sc.scrollTop }
  sc.setPointerCapture(e.pointerId)
}
function onMove(e: PointerEvent) {
  const sc = scroller.value
  if (!drag || !sc) return
  sc.scrollLeft = drag.l - (e.clientX - drag.x)
  sc.scrollTop = drag.t - (e.clientY - drag.y)
}

/** O indicador do canto do chip reúne tudo o que aconteceu com ele no passo: criado, modificado, gerou ou mexeu em outros. */
function badgesOf(c: StepChip): { icon: 'badge-new' | 'badge-change' | 'badge-effect'; label: string }[] {
  const out: { icon: 'badge-new' | 'badge-change' | 'badge-effect'; label: string }[] = []
  if (c.isNew) out.push({ icon: 'badge-new', label: 'created' })
  if (c.changed) out.push({ icon: 'badge-change', label: 'modified' })
  if (c.effects.length) out.push({ icon: 'badge-effect', label: 'acted on other entities' })
  return out
}

/** Até 6 chips por passo, o escolhido sempre em primeiro; o resto fica atrás do chip Show more, que abre a lista completa. */
const CHIP_LIMIT = 6
const visibleChips = (chips: StepChip[]) => {
  const chosen = chips.findIndex((c) => c.chosen)
  const ordered = chosen > 0 ? [chips[chosen]!, ...chips.filter((_, i) => i !== chosen)] : chips
  return ordered.slice(0, CHIP_LIMIT)
}
const more = ref<{ info: StepInfo; chips: StepChip[]; anchor: { left: number; bottom: number; maxHeight: number } } | null>(null)
const moreItems = computed<MoreItem[]>(() =>
  (more.value?.chips ?? []).map((c) => ({ key: c.key, text: c.text, bg: c.idle ? 'var(--chip-idle-bg)' : CONCEPT_COLORS[c.kind].bg, color: c.idle ? 'var(--c-8a8f99)' : CONCEPT_COLORS[c.kind].fg, tip: c.tip, chosen: c.chosen, removed: c.removed, badge: c.badge, kind: c.kind })),
)
function openMore(info: StepInfo, chips: StepChip[], ev: MouseEvent) {
  const r = (ev.currentTarget as HTMLElement).getBoundingClientRect()
  more.value = { info, chips, anchor: { left: Math.max(8, r.left), bottom: Math.max(8, window.innerHeight - r.top + 6), maxHeight: Math.max(220, r.top - 16) } }
}
function pickMore(key: string) {
  const m = more.value
  const chip = m?.chips.find((c) => c.key === key)
  if (!m || !chip) return
  more.value = null
  if (chip.candidate) {
    anchor.value = { left: 24, bottom: 24, maxHeight: 420 }
    explained.value = { chip, step: m.info }
  } else if (chip.id && session.selectedAgent) lifecycle.open({ agent: session.selectedAgent, id: chip.id, type: chip.entityType, label: chip.text })
}

// Conectores e filas (as filas são decoração: o modelo não tem dado de fila, D-113). Medidas do protótipo: seta de 50 px no vão
// de 90 px entre fases, ligações curtas entre passos encadeados e filas centradas no vão, 34 px abaixo das caixas.
interface Lane {
  label: string
  cx: number
  top: number
}
const lanes = ref<Lane[]>([])
const connectors = ref<string[]>([])
/** Pontos de partida das setas entre fases (círculo cheio de raio 3). */
const arrowDots = ref<{ x: number; y: number }[]>([])

interface Box {
  x: number
  y: number
  w: number
  h: number
}
function boxOfEl(el: HTMLElement | null): Box | null {
  if (!el || !canvas.value) return null
  let x = 0
  let y = 0
  let node: HTMLElement | null = el
  while (node && node !== canvas.value) {
    x += node.offsetLeft
    y += node.offsetTop
    node = node.offsetParent as HTMLElement | null
  }
  return { x, y, w: el.offsetWidth, h: el.offsetHeight }
}
const boxOf = (title: string) => boxOfEl(canvas.value?.querySelector<HTMLElement>(`[data-step="${title}"]`) ?? null)
/** O chip que representa o passo na cadeia: o escolhido, quando há, senão o primeiro. Passo sem chip de verdade (só o aviso apagado) não liga a nada. */
const chainChip = (title: string) => {
  const chips = canvas.value?.querySelectorAll<HTMLElement>(`[data-step="${title}"] button.mi-tchip:not([title^="Show all"])`)
  return boxOfEl((chips && ([...chips].find((c) => c.title === 'Chosen') ?? chips[0])) ?? null)
}
const f = (n: number) => Math.round(n * 10) / 10

function measureLanes() {
  const out: string[] = []
  const dots: { x: number; y: number }[] = []
  const ls: Lane[] = []
  const phaseEdges = PHASES.map((p) => {
    const boxes = STEPS.filter((s) => s.phase === p.phase).map((s) => boxOf(s.title)).filter((b): b is Box => !!b)
    return boxes.length ? { first: boxes[0]!, last: boxes[boxes.length - 1]! } : null
  })
  // setas entre fases
  const present = phaseEdges.filter((e): e is NonNullable<typeof e> => !!e)
  for (let i = 0; i < present.length - 1; i++) {
    const a = present[i]!
    const y = a.last.y + a.last.h / 2
    const x1 = a.last.x + a.last.w + 20
    const x2 = x1 + 50
    dots.push({ x: f(x1), y: f(y) })
    out.push(`M${f(x1)} ${f(y)} L${f(x2)} ${f(y)}`, `M${f(x2 - 6)} ${f(y - 4)} L${f(x2)} ${f(y)} L${f(x2 - 6)} ${f(y + 4)}`)
  }
  // ligações curtas entre passos encadeados
  const chain: [string, string][] = [
    ['Select event', 'Select plan'],
    ['Select plan', 'Create intention'],
    ['Select intention', 'Execute step'],
    ['Execute step', 'Clear intention'],
  ]
  // a linha liga o chip de um passo ao chip relacionado do passo seguinte, no meio da altura dos chips
  for (const [from, to] of chain) {
    const a = chainChip(from)
    const b = chainChip(to)
    if (!a || !b) continue
    const y = a.y + a.h / 2
    out.push(`M${f(a.x + a.w)} ${f(y)} L${f(b.x)} ${f(b.y + b.h / 2)}`)
  }
  // filas: centradas no vão entre fases, com curvas que descem das caixas e sobem às caixas seguintes
  const defs: { label: string; between: [number, number]; from: string[]; to: string }[] = [
    { label: 'Event queue', between: [0, 1], from: ['Perceive environment', 'Read message'], to: 'Select event' },
    { label: 'Intention queue', between: [1, 2], from: ['Create intention'], to: 'Select intention' },
  ]
  const curve = (x1: number, y1: number, x2: number, y2: number) =>
    `M${f(x1)} ${f(y1)} C${f(x1)} ${f(y1 + (y2 - y1) * 0.75)} ${f(x1 + (x2 - x1) * 0.35)} ${f(y2)} ${f(x2)} ${f(y2)}`
  const orphans = new Set<string>()
  for (const d of defs) {
    const a = phaseEdges[d.between[0]]
    const b = phaseEdges[d.between[1]]
    const sources = d.from.map(boxOf).filter((x): x is Box => !!x)
    const target = boxOf(d.to)
    if (!sources.length && !target) continue
    if (sources.length && target && a && b) {
      ls.push({ label: d.label, cx: (a.last.x + a.last.w + b.first.x) / 2, top: a.last.y + a.last.h + 34 })
      continue
    }
    // só um lado da fila está na tela (a outra fase está oculta): a fila fica sob a caixa que existe, presa a ela por um traço reto
    const anchor = (target ?? sources[0])!
    const cx = anchor.x + anchor.w / 2
    const top = anchor.y + anchor.h + 34
    orphans.add(d.label)
    ls.push({ label: d.label, cx, top })
    out.push(`M${f(cx)} ${f(anchor.y + anchor.h + 2)} L${f(cx)} ${f(top)}`)
  }
  lanes.value = ls
  // as larguras das filas só existem depois do desenho: mede e traça as curvas
  void nextTick(() => {
    for (const d of defs) {
      if (orphans.has(d.label)) continue
      const el = canvas.value?.querySelector<HTMLElement>(`[data-lane="${d.label}"]`)
      const lane = ls.find((l) => l.label === d.label)
      const to = boxOf(d.to)
      if (!el || !lane || !to) continue
      const left = lane.cx - el.offsetWidth / 2
      const right = lane.cx + el.offsetWidth / 2
      const cy = lane.top + el.offsetHeight / 2
      d.from.forEach((title, i, arr) => {
        const a = boxOf(title)
        if (a) out.push(curve(a.x + a.w / 2, a.y + a.h, left - 1, cy + (arr.length > 1 ? (i === 0 ? 6 : -6) : 0)))
      })
      const x2 = to.x + to.w / 2
      const y2 = to.y + to.h + 2
      out.push(`M${f(right)} ${f(cy)} C${f(right + (x2 - right) * 0.6)} ${f(cy)} ${f(x2)} ${f(y2 + (cy - y2) * 0.6)} ${f(x2)} ${f(y2)}`)
    }
    connectors.value = out
    arrowDots.value = dots
  })
}

function open(chip: StepChip, info: StepInfo, ev: MouseEvent, step?: ReasoningStep) {
  if (chip.candidate) {
    const r = (ev.currentTarget as HTMLElement).closest('[data-k="reason"]')?.getBoundingClientRect()
    anchor.value = { left: (r?.left ?? 12) + 12, bottom: window.innerHeight - (r?.bottom ?? window.innerHeight) + 12, maxHeight: Math.max(220, (r?.height ?? 400) + 280) }
    explained.value = { chip, step: info }
    return
  }
  if (chip.id && session.selectedAgent) {
    const cycle = session.shown?.cycle
    const highlight = (chip.changed || chip.isNew || chip.removed || chip.effects.length) && step && cycle != null ? { cycle, step: step.step, iteration: step.iteration, ordinal: step.ordinal } : null
    lifecycle.open({ agent: session.selectedAgent, id: chip.id, type: chip.entityType, label: chip.text }, undefined, highlight)
  }
}
</script>

<template>
  <div class="mi-tracer-wrap" style="position: relative; flex: 1 1 0; min-height: 0; display: flex; flex-direction: column">
    <p v-if="error" class="mi-warnbox">{{ error }}</p>
    <div class="mi-zoomdock">
      <div class="mi-zoombox" role="group" aria-label="Zoom" style="opacity: 1">
        <button v-if="manual" type="button" class="mi-zbtn" title="Fit to width" aria-label="Fit to width" @click="manual = false; fit()"><Icon name="fit" /></button>
        <button type="button" class="mi-zbtn" title="Zoom out" aria-label="Zoom out" @click="setZoom(zoom / 1.25)"><Icon name="minimize" /></button>
        <button type="button" class="mi-zbtn" title="Zoom in" aria-label="Zoom in" @click="setZoom(zoom * 1.25)"><Icon name="zoom-in" /></button>
      </div>
      <div class="mi-idle" style="padding: var(--sp-4); background: var(--bg-card); border: 1px solid var(--c-e6e8ec); border-radius: 10px">
        <button type="button" class="mi-btn" :title="showIdle ? 'Hide idle steps' : 'Show idle steps'" :aria-pressed="showIdle" style="gap: var(--sp-4); border-color: transparent; padding: 0 var(--sp-12)" @click="showIdle = !showIdle"><Icon :name="showIdle ? 'eye' : 'eye-off'" /><span style="font-size: 12.8px">Idle steps</span></button>
      </div>
    </div>
    <div v-if="!phases.length" class="mi-emptybig" role="status" style="margin: auto">{{ showIdle ? 'No reasoning phase in this cycle' : 'No active reasoning step in this cycle. Turn on Idle steps to see all of them.' }}</div>
    <div v-show="phases.length" ref="scroller" class="mi-tracer-scroll" @wheel="onWheel" @pointerdown="onDown" @pointermove="onMove" @pointerup="drag = null">
      <div :style="{ position: 'relative', flexShrink: 0, margin: 'auto', width: `${natural.w * zoom}px`, height: `${natural.h * zoom}px` }">
        <div ref="canvas" class="mi-trcanvas" :style="{ transform: `scale(${zoom})` }">
          <div class="mi-phases">
            <section v-for="p in phases" :key="p.phase" class="mi-phase">
              <header class="mi-phasehead">
                <Icon :name="p.icon" />{{ p.title }}
                <span v-if="p.its.length > 1" class="mi-iters mi-iters-ph" role="group" aria-label="Iterations">
                  <button v-for="it in p.its" :key="it" type="button" class="mi-iterbtn" :title="`Iteration ${it + 1}`" :aria-pressed="it === currentIteration(p.phase)" @click="iteration[p.phase] = it">{{ it + 1 }}</button>
                </span>
              </header>
              <div class="mi-steps">
                <div v-for="s in p.steps" :key="s.info.step" class="mi-stepbox" :class="s.idle && 'mi-idlestep'" :data-step="s.info.title">
                  <div class="mi-steptitle">
                    <span>{{ s.info.title }}</span>
                    <span v-if="s.occs.length > 1" class="mi-iters" role="group" aria-label="Occurrences">
                      <button v-for="o in s.occs" :key="o.ordinal" type="button" class="mi-iterbtn" :title="`Occurrence ${o.ordinal}`" :aria-pressed="o.ordinal === s.step?.ordinal" @click="occurrence[s.info.step] = o.ordinal">{{ o.ordinal }}</button>
                    </span>
                  </div>
                  <div class="mi-chiplist">
                    <span v-if="s.idle" class="mi-tchip mi-tchip-flow" :style="{ background: 'var(--chip-idle-bg)', color: 'var(--c-8a8f99)' }">{{ s.info.idle }}</span>
                    <button
                      v-for="c in visibleChips(s.chips)"
                      :key="c.key"
                      type="button"
                      class="mi-tchip mi-tchip-flow"
                      :title="c.tip"
                      :style="{ background: c.idle ? 'var(--chip-idle-bg)' : CONCEPT_COLORS[c.kind].bg, color: c.idle ? 'var(--c-8a8f99)' : CONCEPT_COLORS[c.kind].fg, cursor: 'pointer', border: 'none', font: 'inherit', fontSize: '11.5px', textAlign: 'left' }"
                      @click="open(c, s.info, $event, s.step ?? undefined)"
                    >
                      <Icon :name="CONCEPT_ICON[c.kind]" />
                      
                      <span v-if="badgesOf(c).length" class="mi-new mi-badges" role="img" :aria-label="badgesOf(c).map((b) => b.label).join(', ')" :title="badgesOf(c).map((b) => b.label).join(', ')"><Icon v-for="b in badgesOf(c)" :key="b.icon" :name="b.icon" /></span>
                      <span class="mi-tchip-t" :style="c.removed ? { textDecoration: 'line-through' } : undefined">{{ c.text }}</span>
                      <span v-if="c.badge" class="mi-pill" :style="{ background: 'var(--ov-08)', color: c.idle ? 'var(--c-8a8f99)' : CONCEPT_COLORS[c.kind].fg, marginLeft: 'auto', flexShrink: 0 }">#{{ c.badge }}</span>
                    </button>
                    <button v-if="s.chips.length > CHIP_LIMIT" type="button" class="mi-tchip mi-tchip-flow" :title="`Show all ${s.chips.length}`" style="background: transparent; color: var(--c-8a8f99); cursor: pointer; border: none; font: inherit; font-size: 11.5px; text-align: left" @click="openMore(s.info, s.chips, $event)">Show more (+{{ s.chips.length - CHIP_LIMIT }})</button>
                  </div>
                </div>
              </div>
            </section>
          </div>
          <svg class="mi-conn" :width="natural.w" :height="natural.h" aria-hidden="true">
            <path v-for="(d, i) in connectors" :key="i" :d="d" fill="none" stroke="var(--arw)" stroke-width="1" stroke-linecap="round" />
            <circle v-for="(c, i) in arrowDots" :key="`dot-${i}`" :cx="c.x" :cy="c.y" r="3" fill="var(--arw)" />
          </svg>
          <span v-for="l in lanes" :key="l.label" class="mi-lane" :data-lane="l.label" :style="{ left: `${l.cx}px`, top: `${l.top}px` }"><Icon name="lane" />{{ l.label }}</span>
        </div>
      </div>
    </div>
    <MoreDialog v-if="more" :title="more.info.title" searchable :items="moreItems" :anchor="more.anchor" @close="more = null" @pick="pickMore" />
    <ExplainDialog v-if="explained" :chip="explained.chip" :step="explained.step" :anchor="anchor" @close="explained = null" />
  </div>
</template>
