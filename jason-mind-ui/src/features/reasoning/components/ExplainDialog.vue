<script setup lang="ts">
import { Icon } from '@/shared/ui'
import type { Plan } from '@/features/inspector/types/state.types'
import type { StepInfo } from '../domain/step-catalog'
import type { StepChip } from '../domain/step-chips'
import ExplainCard from './ExplainCard.vue'

/** Cartão de explicação de um chip numa janela: o status (escolhido ou não), o porquê e, num plano, cada termo do contexto. */
defineProps<{ chip: StepChip; step: StepInfo; plans?: ReadonlyMap<string, Plan>; anchor: { left: number; bottom: number; maxHeight: number } }>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to=".mi-root">
    <div style="position: fixed; inset: 0; z-index: 36" @click="emit('close')" />
    <div role="dialog" aria-label="Explanation" class="mi-dialog" :data-theme-root="true" :style="{ position: 'fixed', zIndex: 37, left: `${anchor.left}px`, bottom: `${anchor.bottom}px`, width: '360px', maxHeight: `${anchor.maxHeight}px` }">
      <div class="mi-ph"><h2>{{ step.title }}</h2><button type="button" class="mi-btn mi-icon" title="Close" aria-label="Close" style="margin-left: auto; border-color: transparent; color: var(--c-8a8f99)" @click="emit('close')"><Icon name="close" /></button></div>
      <div style="flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 0 var(--sp-12) var(--sp-12)">
        <ExplainCard :chip="chip" :step="step" :plans="plans" />
      </div>
    </div>
  </Teleport>
</template>
