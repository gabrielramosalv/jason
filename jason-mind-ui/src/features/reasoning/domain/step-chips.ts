import { conceptOf } from '@/shared/domain/entities'
import type { ConceptKind } from '@/shared/domain/entities'
import { numericId } from '@/shared/domain/ids'
import type { Plan } from '@/features/inspector/types/state.types'
import type { Candidate, EntityRef, ReasoningStep, StepEffect } from '../types/reasoning.types'
import { planText } from './plan-text'

/** Um chip do diagrama: o texto, o conceito (cor e ícone), se foi o escolhido e a entidade de origem. */
export interface StepChip {
  key: string
  text: string
  kind: ConceptKind
  entityType: string
  id: string | null
  badge: string | null
  /** Candidato não escolhido: aparece apagado. */
  idle: boolean
  chosen: boolean
  /** Entidade removida neste passo: apagada e com o nome riscado. */
  removed: boolean
  /** Teve mudanças neste passo: o chip mostra só o lápis, e o detalhe fica no Life cycle. */
  changed: boolean
  /** O que este chip fez a outras entidades no passo (gerou, modificou, removeu). */
  effects: StepEffect[]
  isNew: boolean
  candidate: Candidate | null
  tip: string
}

const fromRef = (e: EntityRef, extra: Partial<StepChip> = {}): StepChip => ({
  key: `${e.type}-${e.id}-${extra.tip ?? ''}`,
  text: e.label,
  kind: conceptOf(e.type),
  entityType: e.type,
  id: e.id,
  badge: e.type === 'intention' || e.type === 'intendedMeans' || e.type === 'event' ? numericId(e.id) : null,
  idle: false,
  removed: false,
  changed: false,
  effects: [],
  chosen: false,
  isNew: false,
  candidate: null,
  tip: e.label,
  ...extra,
})

/** O plano candidato aparece pelo gatilho e contexto (o rótulo `p__1` só vale como reserva, sem o programa carregado). */
const planLabelOf = (c: Candidate, plans?: ReadonlyMap<string, Plan>) => {
  const plan = plans?.get(c.id)
  return plan ? planText(plan) : c.label
}

/** Os chips de um passo: os candidatos, quando há, senão o que entrou, mudou ou saiu. */
export function chipsOf(step: ReasoningStep, plans?: ReadonlyMap<string, Plan>): StepChip[] {
  // Select plan mostra só planos: sem plano relevante, o evento de entrada não entra no lugar deles
  if (step.step === 'FIND_OPTION' && !step.candidates.length) {
    if (!step.inputs.length) return []
    return [
      {
        key: 'no-relevant-plan',
        text: 'No relevant plan',
        kind: 'plan',
        entityType: 'plan',
        id: null,
        badge: null,
        idle: true,
        chosen: false,
        removed: false,
        changed: false,
        effects: [],
        isNew: false,
        candidate: null,
        tip: 'No plan has this event as trigger: the event is discarded',
      },
    ]
  }
  if (step.candidates.length) {
    return step.candidates.map((c) => fromRef(c, { key: `cand-${c.id}`, text: planLabelOf(c, plans), idle: !c.chosen, chosen: c.chosen, candidate: c, tip: c.chosen ? 'Chosen' : 'Not chosen' }))
  }
  const out: StepChip[] = []
  const seen = new Set<string>()
  const push = (c: StepChip) => {
    if (seen.has(c.key)) return
    seen.add(c.key)
    out.push(c)
  }
  step.inputs.forEach((e) => push(fromRef(e, { chosen: step.chosen?.id === e.id, tip: 'Input of the step' })))
  step.created.forEach((e) => push(fromRef(e, { isNew: true, tip: 'Created in this step' })))
  // várias mudanças da mesma entidade no passo se acumulam num chip só
  const changes = new Map<string, { ref: EntityRef; lines: string[] }>()
  step.changed.forEach((e) => {
    const group = changes.get(e.id) ?? { ref: e, lines: [] }
    group.lines.push(`${e.timeline} → ${e.value}`)
    changes.set(e.id, group)
  })
  changes.forEach(({ ref, lines }, id) => {
    const tip = `Changed in this step: ${lines.join('; ')}`
    const existing = out.find((c) => c.id === id)
    if (existing) {
      existing.changed = true
      existing.tip = `${existing.tip}. ${tip}`
    } else push(fromRef(ref, { changed: true, tip }))
  })
  step.removed.forEach((e) => push(fromRef(e, { idle: true, removed: true, tip: 'Removed in this step' })))
  // o que cada chip fez a outras entidades no passo entra no mesmo indicador
  const VERB = { GENERATES: 'Generated', MODIFIES: 'Modified', REMOVES: 'Removed' } as const
  for (const chip of out) {
    const mine = (step.effects ?? []).filter((e) => e.by === chip.id)
    if (!mine.length) continue
    chip.effects = mine
    chip.tip = `${chip.tip}. ${mine.map((e) => `${VERB[e.kind]} ${e.entity.label}`).join('; ')}`
  }
  return out
}
