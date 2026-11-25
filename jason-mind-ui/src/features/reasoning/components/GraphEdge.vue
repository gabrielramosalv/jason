<script setup lang="ts">
import { computed } from 'vue'
import { BaseEdge, EdgeLabelRenderer, getBezierPath } from '@xyflow/vue'
import type { EdgeProps } from '@xyflow/vue'
import { Icon } from '@/shared/ui'
import type { EdgeBadge } from '../domain/reasoning-graph'

/** Uma linha, um verbo: o ícone no meio diz o que a origem fez ao destino, e o clique nele abre essa ocorrência no Life cycle. */
const props = defineProps<EdgeProps>()
const badge = computed(() => (props.data?.badge ?? null) as EdgeBadge | null)
const flow = computed(() => (props.data?.flow as string | null) ?? null)
/** Linhas entre o mesmo par de nós se afastam na vertical para não ficarem uma sobre a outra. */
const shift = computed(() => {
  const count = Number(props.data?.count ?? 1)
  const index = Number(props.data?.index ?? 0)
  return (index - (count - 1) / 2) * 12
})
const bezier = computed(() =>
  getBezierPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY + shift.value,
    sourcePosition: props.sourcePosition,
    targetX: props.targetX,
    targetY: props.targetY + shift.value,
    targetPosition: props.targetPosition,
  }),
)
const open = () => (props.data?.open as (() => void) | undefined)?.()
</script>

<template>
  <BaseEdge :id="id" :path="bezier[0]" :marker-end="markerEnd" />
  <EdgeLabelRenderer v-if="badge">
    <button
      type="button"
      class="mi-gbadge nodrag nopan"
      :class="flow && `mi-gflow-${flow}`"
      :aria-label="`${badge.label}: open in Life cycle`"
      :title="`${badge.label}: open in Life cycle`"
      :style="{ transform: `translate(-50%, -50%) translate(${bezier[1]}px, ${bezier[2]}px)` }"
      @click.stop="open"
    >
      <Icon :name="badge.icon" />
    </button>
  </EdgeLabelRenderer>
</template>
