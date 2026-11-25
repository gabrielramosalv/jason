<script setup lang="ts">
import { computed } from 'vue'
import { CONCEPT_COLORS } from '@/shared/domain/entities'
import { Icon } from '@/shared/ui'
import type { StepInfo } from '../domain/step-catalog'
import { ruleText } from '../domain/step-catalog'
import type { StepChip } from '../domain/step-chips'
import type { PlanTerm } from '../types/reasoning.types'

/** Cartão de explicação de um chip: o status (escolhido ou não), o porquê e, num plano, cada termo do contexto. */
const props = defineProps<{ chip: StepChip; step: StepInfo; anchor: { left: number; bottom: number; maxHeight: number } }>()
const emit = defineEmits<{ close: [] }>()

const colors = computed(() => CONCEPT_COLORS[props.chip.kind])
const terms = computed<PlanTerm[]>(() => props.chip.candidate?.terms ?? [])
const rule = computed(() => ruleText(props.chip.candidate?.rule ?? null))
const verdictIcon = (v: string) => (v === 'TRUE' ? 'verdict-ok' : v === 'FALSE' ? 'verdict-no' : 'verdict-skip')
const noteOf = (t: PlanTerm) => t.bindings.map((b) => `${b.name} = ${b.value}`).join(', ')
const verdict = computed(() => {
  if (!props.chip.candidate) return null
  const failed = terms.value.findIndex((t) => t.verdict === 'FALSE')
  if (props.chip.chosen) return 'Applicable and selected.'
  if (failed >= 0) return `Not applicable: term ${failed + 1} is false. Later terms were not checked.`
  return 'Applicable, but an earlier applicable plan was selected.'
})
</script>

<template>
  <Teleport to=".mi-root">
    <div style="position: fixed; inset: 0; z-index: 36" @click="emit('close')" />
    <div role="dialog" aria-label="Explanation" class="mi-dialog" :data-theme-root="true" :style="{ position: 'fixed', zIndex: 37, left: `${anchor.left}px`, bottom: `${anchor.bottom}px`, width: '360px', maxHeight: `${anchor.maxHeight}px` }">
      <div class="mi-ph"><h2>{{ step.title }}</h2><button type="button" class="mi-btn mi-icon" title="Close" aria-label="Close" style="margin-left: auto; border-color: transparent; color: var(--c-8a8f99)" @click="emit('close')"><Icon name="close" /></button></div>
      <div style="flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 0 var(--sp-12) var(--sp-12); display: flex; flex-direction: column; gap: var(--sp-12)">
        <div style="display: flex; align-items: center; gap: var(--sp-8); min-width: 0">
          <span :style="{ minWidth: 0, overflowWrap: 'anywhere', padding: 'var(--sp-4) var(--sp-8)', borderRadius: '8px', fontSize: '11.5px', lineHeight: '18px', background: colors.bg, color: colors.fg, boxShadow: 'inset 0 0 0 1px var(--ov-08)' }">{{ chip.text }}</span>
          <span style="flex-shrink: 0; margin-left: auto; font-size: 11.5px; color: var(--c-8a8f99)">{{ chip.chosen ? 'Chosen' : chip.idle ? 'Not chosen' : chip.tip }}</span>
          <Icon v-if="chip.chosen" name="chosen" />
        </div>
        <div v-if="rule" style="display: flex; flex-direction: column; gap: var(--sp-2)"><span style="font-size: 11.5px; color: var(--c-8a8f99)">Why</span><span style="font-size: 12.8px; line-height: 18px">{{ rule }}</span></div>
        <div v-if="terms.length" style="display: flex; flex-direction: column; gap: var(--sp-4)">
          <span style="font-size: 11.5px; color: var(--c-8a8f99)">Trigger and context</span>
          <div v-for="t in terms" :key="t.index" style="display: flex; align-items: flex-start; gap: var(--sp-8)">
            <span style="flex-shrink: 0; width: 14px; height: 18px; display: inline-flex; align-items: center; justify-content: center"><Icon :name="verdictIcon(t.verdict)" /></span>
            <div style="display: flex; flex-direction: column; min-width: 0">
              <span class="mi-src" style="font-size: 11.5px; line-height: 18px; overflow-wrap: anywhere">Term {{ t.index + 1 }}: {{ t.verdict.toLowerCase().replace(/_/g, ' ') }}</span>
              <span v-if="noteOf(t)" style="font-size: 11.5px; line-height: 15px; color: var(--c-8a8f99); overflow-wrap: anywhere">{{ noteOf(t) }}</span>
            </div>
          </div>
          <span v-if="verdict" style="font-size: 12.8px; line-height: 18px; padding-top: var(--sp-2)">{{ verdict }}</span>
        </div>
        <div style="display: flex; flex-direction: column; gap: var(--sp-4); border-top: 1px solid var(--bd-card); padding-top: var(--sp-12)"><span style="font-size: 11.5px; color: var(--c-8a8f99)">Rule of this step</span><span style="font-size: 11.5px; line-height: 16px; color: var(--c-5b5f6b)">{{ step.rule }}</span></div>
      </div>
    </div>
  </Teleport>
</template>
