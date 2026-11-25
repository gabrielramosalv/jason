<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@xyflow/vue'
import type { NodeProps } from '@xyflow/vue'
import { CONCEPT_COLORS, CONCEPT_ICON } from '@/shared/domain/entities'
import { Icon } from '@/shared/ui'
import { useInspectorStore } from '@/stores/inspector.store'
import type { GraphNode } from '../domain/reasoning-graph'

/** O nó do grafo: o mesmo chip do diagrama de passos, com um Handle invisível de cada lado para as linhas. */
const props = defineProps<NodeProps>()
const node = computed(() => props.data.node as GraphNode)
const focused = computed(() => !!props.data.focused)
/** Na animação do fluxo: ainda não aconteceu (`future`), já aconteceu (`past`) ou está acontecendo (`now`). */
const flow = computed(() => (props.data.flow as string | null) ?? null)
const inspector = useInspectorStore()
const chip = computed(() => (node.value.kind === 'chip' ? node.value.chip : null))
/** O objetivo que abriu a intenção leva a bandeira; os chamados por ele, as camadas. */
const kind = computed(() => (chip.value?.entityType === 'intendedMeans' ? inspector.goalKindOf(chip.value.id) : (chip.value?.kind ?? 'plan')))
</script>

<template>
  <div class="mi-gnode" :class="flow && `mi-gflow-${flow}`" :style="{ width: `${node.width}px` }">
    <Handle type="target" :position="Position.Left" class="mi-ghandle" />
    <button
      v-if="chip"
      type="button"
      class="mi-tchip mi-tchip-flow"
      :class="focused && 'mi-chipfocus'"
      :title="chip.tip"
      :style="{ width: '100%', background: chip.idle ? 'var(--chip-idle-bg)' : CONCEPT_COLORS[kind].bg, color: chip.idle ? 'var(--c-8a8f99)' : CONCEPT_COLORS[kind].fg, cursor: 'pointer', border: 'none', font: 'inherit', fontSize: '11.5px', textAlign: 'left' }"
    >
      <Icon :name="CONCEPT_ICON[kind]" />
      <span class="mi-tchip-t">{{ chip.text }}</span>
      <span v-if="chip.badge" class="mi-pill" :style="{ background: 'var(--ov-08)', color: chip.idle ? 'var(--c-8a8f99)' : CONCEPT_COLORS[kind].fg, marginLeft: 'auto', flexShrink: 0 }">#{{ chip.badge }}</span>
    </button>
    <Handle type="source" :position="Position.Right" class="mi-ghandle" />
  </div>
</template>
