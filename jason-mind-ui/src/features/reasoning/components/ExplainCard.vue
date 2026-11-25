<script setup lang="ts">
import { computed } from 'vue'
import { CONCEPT_COLORS } from '@/shared/domain/entities'
import { Icon } from '@/shared/ui'
import type { Plan } from '@/features/inspector/types/state.types'
import { contextTerm } from '../domain/plan-text'
import type { StepInfo } from '../domain/step-catalog'
import { ruleText } from '../domain/step-catalog'
import type { StepChip } from '../domain/step-chips'
import type { PlanTerm } from '../types/reasoning.types'

/** O conteúdo da explicação de um chip: o status (escolhido ou não), o porquê e, num plano, cada termo do contexto. */
const props = defineProps<{ chip: StepChip; step: StepInfo; plans?: ReadonlyMap<string, Plan> }>()

const colors = computed(() => CONCEPT_COLORS[props.chip.kind])
const terms = computed<PlanTerm[]>(() => props.chip.candidate?.terms ?? [])
const rule = computed(() => ruleText(props.chip.candidate?.rule ?? null))
const verdictIcon = (v: string) => (v === 'TRUE' ? 'verdict-ok' : v === 'FALSE' ? 'verdict-no' : 'verdict-skip')
const noteOf = (t: PlanTerm) => t.bindings.map((b) => `${b.name} = ${b.value}`).join(', ')
const plan = computed(() => (props.chip.id ? props.plans?.get(props.chip.id) : undefined))
/** O termo como está escrito no plano; sem o programa carregado, só o número dele. */
const termText = (t: PlanTerm) => (plan.value ? contextTerm(plan.value, t.index) : null) ?? `Term ${t.index + 1}`
const verdictText = (t: PlanTerm) => t.verdict.toLowerCase().replace(/_/g, ' ')
</script>

<template>
  <div style="display: flex; flex-direction: column; gap: var(--sp-12)">
    <div style="display: flex; align-items: center; gap: var(--sp-8); min-width: 0">
      <span :style="{ minWidth: 0, overflowWrap: 'anywhere', padding: 'var(--sp-4) var(--sp-8)', borderRadius: '8px', fontSize: '11.5px', lineHeight: '18px', background: colors.bg, color: colors.fg, boxShadow: 'inset 0 0 0 1px var(--ov-08)' }">{{ chip.text }}</span>
      <span v-if="chip.chosen" style="flex-shrink: 0; margin-left: auto; display: inline-flex; color: var(--c-8a8f99)" role="img" aria-label="Chosen" title="Chosen"><Icon name="verdict-ok" /></span>
      <span v-else style="flex-shrink: 0; margin-left: auto; font-size: 11.5px; color: var(--c-8a8f99)">{{ chip.idle ? 'Not chosen' : chip.tip }}</span>
    </div>
    <div v-if="rule" style="display: flex; flex-direction: column; gap: var(--sp-2)"><span style="font-size: 11.5px; color: var(--c-8a8f99)">Why</span><span style="font-size: 12.8px; line-height: 18px">{{ rule }}</span></div>
    <div v-if="terms.length" style="display: flex; flex-direction: column; gap: var(--sp-4)">
      <span style="font-size: 11.5px; color: var(--c-8a8f99)">Context evaluation</span>
      <div v-for="t in terms" :key="t.index" style="display: flex; align-items: flex-start; gap: var(--sp-8)">
        <span style="flex-shrink: 0; width: 14px; height: 18px; display: inline-flex; align-items: center; justify-content: center" role="img" :aria-label="verdictText(t)" :title="verdictText(t)"><Icon :name="verdictIcon(t.verdict)" /></span>
        <div style="display: flex; flex-direction: column; min-width: 0">
          <span class="mi-src" style="font-size: 11.5px; line-height: 18px; overflow-wrap: anywhere">{{ termText(t) }}</span>
          <span v-if="noteOf(t)" style="font-size: 11.5px; line-height: 15px; color: var(--c-8a8f99); overflow-wrap: anywhere">{{ noteOf(t) }}</span>
        </div>
      </div>
    </div>
    <div style="display: flex; flex-direction: column; gap: var(--sp-4); border-top: 1px solid var(--bd-card); padding-top: var(--sp-12)"><span style="font-size: 11.5px; color: var(--c-8a8f99)">Rule of this step</span><span style="font-size: 11.5px; line-height: 16px; color: var(--c-5b5f6b)">{{ step.rule }}</span></div>
  </div>
</template>
