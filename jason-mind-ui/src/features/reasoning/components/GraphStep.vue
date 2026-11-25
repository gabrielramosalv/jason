<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@xyflow/vue'
import type { NodeProps } from '@xyflow/vue'
import { Icon } from '@/shared/ui'
import { PHASES } from '../domain/step-catalog'
import type { GraphStepNode } from '../domain/reasoning-graph'

/** O nó de um passo do ciclo: o que ele usou chega pela esquerda e o que ele fez sai pela direita. */
const props = defineProps<NodeProps>()
const node = computed(() => props.data.node as GraphStepNode)
const flow = computed(() => (props.data.flow as string | null) ?? null)
const icon = computed(() => PHASES.find((p) => p.phase === node.value.phase)?.icon ?? 'phase-deliberate')
</script>

<template>
  <div class="mi-gnode" :class="flow && `mi-gflow-${flow}`" :style="{ width: `${node.width}px` }">
    <Handle type="target" :position="Position.Left" class="mi-ghandle" />
    <span class="mi-gstep" :class="node.options.length > 0 && 'mi-gstep-pick'" :title="node.options.length ? `${node.title}: ${node.options.length} candidates, click to list them` : node.title"><Icon :name="icon" />{{ node.title }}<span v-if="node.rejected" class="mi-pill" style="background: var(--ov-08); margin-left: var(--sp-4)">+{{ node.rejected }}</span></span>
    <Handle type="source" :position="Position.Right" class="mi-ghandle" />
  </div>
</template>
