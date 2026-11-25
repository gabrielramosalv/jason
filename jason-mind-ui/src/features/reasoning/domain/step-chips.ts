import { chipText, conceptOf } from '@/shared/domain/entities'
import type { ConceptKind } from '@/shared/domain/entities'
import { numericId } from '@/shared/domain/ids'
import type { Plan } from '@/features/inspector/types/state.types'
import type { Candidate, EntityRef, ReasoningStep, StepEffect } from '../types/reasoning.types'
import { planText } from './plan-text'

/** A zona do passo onde o chip aparece: o que o passo trata e decide (`nature`) ou o que ele fez ao modelo (`result`). */
export type ChipZone = 'nature' | 'result'

/** Um chip do diagrama: o texto, o conceito (cor e ícone), se foi o escolhido e a entidade de origem. */
export interface StepChip {
  key: string
  zone: ChipZone
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

const fromRef = (e: EntityRef, zone: ChipZone, extra: Partial<StepChip> = {}): StepChip => ({
  key: `${zone}-${e.type}-${e.id}-${extra.tip ?? ''}`,
  zone,
  text: chipText(e.type, e.label),
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

/** O que o passo trata e decide: os candidatos, quando há, senão o que entrou (com o escolhido marcado). */
function natureChips(step: ReasoningStep, plans?: ReadonlyMap<string, Plan>): StepChip[] {
  // Select plan mostra só planos: sem plano relevante, o evento de entrada não entra no lugar deles
  if (step.step === 'FIND_OPTION' && !step.candidates.length) {
    if (!step.inputs.length) return []
    return [
      {
        key: 'nature-no-relevant-plan',
        zone: 'nature',
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
    return step.candidates.map((c) => fromRef(c, 'nature', { key: `nature-cand-${c.id}`, text: planLabelOf(c, plans), idle: !c.chosen, chosen: c.chosen, candidate: c, tip: c.chosen ? 'Chosen' : 'Not chosen' }))
  }
  const seen = new Set<string>()
  return step.inputs
    .filter((e) => !seen.has(e.id) && !!seen.add(e.id))
    .map((e) => fromRef(e, 'nature', { chosen: step.chosen?.id === e.id, tip: step.chosen?.id === e.id ? 'Chosen' : 'Candidate' }))
}

/** O que o passo fez ao modelo: o que nasceu, mudou ou saiu. Várias mudanças da mesma entidade se acumulam num chip só. */
function resultChips(step: ReasoningStep): StepChip[] {
  const out: StepChip[] = []
  const seen = new Set<string>()
  const push = (c: StepChip) => {
    if (seen.has(c.key)) return
    seen.add(c.key)
    out.push(c)
  }
  step.created.forEach((e) => push(fromRef(e, 'result', { isNew: true, tip: 'Created in this step' })))
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
    } else push(fromRef(ref, 'result', { changed: true, tip }))
  })
  // removida: mantém a cor do conceito; só o indicador diz que saiu
  step.removed.forEach((e) => push(fromRef(e, 'result', { removed: true, tip: 'Removed in this step' })))
  return out
}

/** Os chips de um passo, cada um na sua zona; o que um chip fez a outras entidades entra no indicador dele (primeiro a natureza, depois o resultado). */
export function chipsOf(step: ReasoningStep, plans?: ReadonlyMap<string, Plan>): StepChip[] {
  const out = [...natureChips(step, plans), ...resultChips(step)]
  const VERB = { GENERATES: 'Generated', MODIFIES: 'Modified', REMOVES: 'Removed' } as const
  const actors = new Set<string>()
  for (const chip of out) {
    if (!chip.id || actors.has(chip.id)) continue
    const mine = (step.effects ?? []).filter((e) => e.by === chip.id)
    if (!mine.length) continue
    actors.add(chip.id)
    chip.effects = mine
    chip.tip = `${chip.tip}. ${mine.map((e) => `${VERB[e.kind]} ${e.entity.label}`).join('; ')}`
  }
  return out
}

/** O chip de uma entidade que o passo usou sem ela ser candidata (por exemplo, o evento para o qual se busca plano). */
export const refChip = (e: EntityRef): StepChip => fromRef(e, 'nature', { tip: 'Used by the step' })

/** Separa os chips nas duas zonas, mantendo a ordem. */
export const splitZones = (chips: StepChip[]) => ({
  nature: chips.filter((c) => c.zone === 'nature'),
  result: chips.filter((c) => c.zone === 'result'),
})

/** O indicador do canto do chip reúne tudo o que aconteceu com ele no passo: criado, modificado, gerou ou mexeu em outros. */
export function badgesOf(c: StepChip): { icon: 'badge-new' | 'badge-change' | 'badge-effect' | 'badge-removed'; label: string }[] {
  const out: { icon: 'badge-new' | 'badge-change' | 'badge-effect' | 'badge-removed'; label: string }[] = []
  if (c.isNew) out.push({ icon: 'badge-new', label: 'created' })
  if (c.removed) out.push({ icon: 'badge-removed', label: 'removed' })
  if (c.changed) out.push({ icon: 'badge-change', label: 'modified' })
  if (c.effects.length) out.push({ icon: 'badge-effect', label: 'acted on other entities' })
  return out
}
