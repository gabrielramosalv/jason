<script setup lang="ts">
import { computed } from 'vue'
import type { Plan } from '@/features/inspector/types/state.types'
import type { GraphStepNode } from '../domain/reasoning-graph'
import ExplainCard from './ExplainCard.vue'

/** O que aparece ao passar o mouse numa etapa: o porquê da escolha (numa seleção) ou a regra do passo. */
const props = defineProps<{ node: GraphStepNode; plans?: ReadonlyMap<string, Plan> }>()
const chosen = computed(() => props.node.options.find((c) => c.chosen) ?? props.node.options[0] ?? null)
</script>

<template>
  <div class="mi-ghover" role="tooltip">
    <div style="font-size: 12.8px; font-weight: 500; margin-bottom: var(--sp-8)">{{ node.info.title }}</div>
    <ExplainCard v-if="chosen" :chip="chosen" :step="node.info" :plans="plans" />
    <span v-else style="font-size: 11.5px; line-height: 16px; color: var(--c-5b5f6b)">{{ node.info.rule }}</span>
  </div>
</template>
