import type { Plan } from '@/features/inspector/types/state.types'
import type { EntityRef, PhaseName, ReasoningStep } from '../types/reasoning.types'
import { STEPS } from './step-catalog'
import type { StepInfo } from './step-catalog'
import { chipsOf, refChip } from './step-chips'
import type { StepChip } from './step-chips'

/** Onde a entidade apareceu no ciclo: o chip, o passo e a ocorrência do passo. */
export interface GraphOccurrence {
  chip: StepChip
  info: StepInfo
  step: ReasoningStep
}

/** O nó de uma entidade: o chip junta o que aconteceu com ela nos passos do ciclo. */
export interface GraphChipNode {
  kind: 'chip'
  id: string
  chip: StepChip
  /** A última ação que a entidade recebeu no ciclo (escolhida, criada, mudada ou removida): o clique no chip abre o Life cycle nela. */
  last: GraphOccurrence | null
  first: GraphOccurrence
  /** A posição em que a entidade apareceu no ciclo, na ordem em que as coisas aconteceram. */
  order: number
  width: number
  height: number
}

/**
 * O nó de um passo. Numa seleção (mensagem, evento, plano, intenção) ele aponta para o escolhido e guarda os candidatos,
 * que o clique lista; nos outros passos aparece só quando algo mudou sem uma entidade como causa.
 */
export interface GraphStepNode {
  kind: 'step'
  id: string
  title: string
  phase: PhaseName
  info: StepInfo
  step: ReasoningStep
  /** Todos os candidatos da seleção, o escolhido primeiro; vazio fora de seleção. */
  options: StepChip[]
  /** Quantos candidatos o passo não escolheu. */
  rejected: number
  /** A posição em que a etapa apareceu no ciclo. */
  order: number
  width: number
  height: number
}

export type GraphNode = GraphChipNode | GraphStepNode

/**
 * O papel da linha: `chosen` sai de uma etapa de seleção para o escolhido, `cause` liga a entidade que causou à que foi criada,
 * mudada ou removida, `step` liga a etapa ao que mudou sem outra entidade como causa e `subject` leva o evento à busca de plano.
 */
export type EdgeRole = 'chosen' | 'cause' | 'step' | 'subject'

/** A ocorrência do Life cycle que um ícone de linha abre: a entidade, o passo e o tipo (nasceu, mudou, terminou; nulo para o escolhido). */
export interface LifecycleTarget {
  entity: { id: string; type: string; label: string }
  step: ReasoningStep
  kind: 'START' | 'CHANGE' | 'END' | null
}

/** O que a linha diz: escolheu, criou, mudou ou removeu, e a ocorrência do Life cycle a que o ícone leva. */
export interface EdgeBadge {
  icon: 'verdict-ok' | 'badge-new' | 'badge-change' | 'badge-removed'
  label: string
  at: LifecycleTarget
}

/** Uma linha é um fato com um verbo só: escolher, criar, mudar ou remover acontecem em momentos diferentes e têm linhas próprias. */
export interface GraphEdge {
  id: string
  source: string
  target: string
  role: EdgeRole
  /** A posição da linha na ordem em que as coisas aconteceram. */
  order: number
  /** O verbo da linha; nulo na que só leva o evento à busca de plano. */
  badge: EdgeBadge | null
  /** A posição desta linha entre as que ligam o mesmo par de nós, e quantas elas são. */
  index: number
  count: number
}

export interface ReasoningGraph {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

const ROW_HEIGHT = 24

/** A largura do chip antes de ele existir na tela: o layout precisa dela, e o texto passa de 270 px com ellipsis. */
const widthOf = (c: StepChip) => Math.min(270, Math.round(34 + c.text.length * 6.3 + (c.badge ? 34 : 0)))

export const selectionKey = (s: ReasoningStep) => `${s.iteration}:${s.step}:${s.ordinal}`

/** O título do passo, com a iteração e a ocorrência quando o passo se repete no ciclo. */
const stepTitle = (info: StepInfo, s: ReasoningStep) => `${info.title}${s.ordinal > 1 ? ` ${s.ordinal}` : ''}${s.iteration > 0 ? ` · it ${s.iteration + 1}` : ''}`

/** O tipo da ocorrência do Life cycle que o chip teve no passo; nulo quando só foi escolhido ou usado. */
export const lifecycleKind = (c: StepChip): LifecycleTarget['kind'] => (c.removed ? 'END' : c.isNew ? 'START' : c.changed ? 'CHANGE' : null)

const targetOf = (c: StepChip, step: ReasoningStep, kind: LifecycleTarget['kind']): LifecycleTarget => ({
  entity: { id: c.id ?? '', type: c.entityType, label: c.text },
  step,
  kind,
})

const changeBadges = (c: StepChip, step: ReasoningStep): EdgeBadge[] => [
  ...(c.isNew ? [{ icon: 'badge-new' as const, label: 'created', at: targetOf(c, step, 'START') }] : []),
  ...(c.changed ? [{ icon: 'badge-change' as const, label: 'modified', at: targetOf(c, step, 'CHANGE') }] : []),
  ...(c.removed ? [{ icon: 'badge-removed' as const, label: 'removed', at: targetOf(c, step, 'END') }] : []),
]

/**
 * Monta o grafo do ciclo exibido. Os nós são as entidades (um chip por entidade) e os passos que aparecem.
 * - Uma etapa de seleção aponta para o escolhido (`✓`); os outros candidatos não viram nós, ficam na lista da etapa.
 * - Uma entidade que causou outra liga-se a ela com o que fez (criou, mudou, removeu), segundo os `effects` do passo.
 * - O que mudou sem entidade como causa liga-se à etapa que o mudou.
 * - Quem age é o objetivo (o `IntendedMeans` do topo da pilha), não a intenção: o objetivo liga-se ao que criou, mudou ou removeu,
 *   e a intenção que o contém liga-se a ele por uma linha tracejada sem verbo (`ownerOf` diz qual é a intenção).
 */
export function buildGraph(steps: readonly ReasoningStep[], plans: ReadonlyMap<string, Plan> | undefined, ownerOf?: (goalId: string) => string | null): ReasoningGraph {
  const nodes = new Map<string, GraphNode>()
  const pending: { source: string; target: string; role: EdgeRole; badge: EdgeBadge | null; order: number }[] = []
  /** Uma linha por verbo: sem verbos (a do assunto da busca de plano) sai uma linha sem ícone. */
  /**
   * A ordem de uma linha é (posição do passo no ciclo, sequência da ocorrência): os passos já vêm na ordem do ciclo e a
   * sequência é a ordem real dos fatos dentro do passo. O que não é ocorrência (a escolha `✓`, o assunto da busca) vem logo
   * depois da pílula do passo e antes dos fatos.
   */
  const STEP_SPAN = 1e12
  let base = 0
  let created = new Map<string, number>()
  let changedAt = new Map<string, number>()
  let removedAt = new Map<string, number>()
  const orderOf = (target: string, badge: EdgeBadge | null): number => {
    const own = badge?.icon === 'badge-new' ? created.get(target) : badge?.icon === 'badge-change' ? changedAt.get(target) : badge?.icon === 'badge-removed' ? removedAt.get(target) : undefined
    return base + (own ?? 1)
  }
  const link = (source: string, target: string, role: EdgeRole, badges: readonly EdgeBadge[] = []) => {
    if (!badges.length) pending.push({ source, target, role, badge: null, order: orderOf(target, null) })
    for (const badge of badges) pending.push({ source, target, role, badge, order: orderOf(target, badge) })
  }

  /** `acts`: a entidade recebeu uma ação no passo (foi escolhida ou criada, mudada, removida), e não só usada. */
  const putChip = (chip: StepChip, id: string, info: StepInfo, step: ReasoningStep, acts = true) => {
    const occurrence: GraphOccurrence = { chip, info, step }
    const existing = nodes.get(id)
    if (existing?.kind === 'chip') {
      const merged = existing.chip
      merged.isNew ||= chip.isNew
      merged.changed ||= chip.changed
      merged.removed ||= chip.removed
      merged.chosen ||= chip.chosen
      merged.idle &&= chip.idle
      if (acts) existing.last = occurrence
      existing.width = Math.max(existing.width, widthOf(merged))
    } else {
      nodes.set(id, {
        kind: 'chip',
        id,
        chip: { ...chip, effects: [...chip.effects] },
        last: acts ? occurrence : null,
        first: occurrence,
        order: base + (created.get(id) ?? changedAt.get(id) ?? removedAt.get(id) ?? 1),
        width: widthOf(chip),
        height: ROW_HEIGHT,
      })
    }
  }

  /** Objetivos que agiram e a intenção que os contém (a linha de contenção só entra se nada liga os dois). */
  const containment: [string, string | null][] = []

  let stepIndex = 0
  for (const step of steps) {
    stepIndex++
    const info = STEPS.find((s) => s.step === step.step)
    if (!info) continue
    const chips = chipsOf(step, plans)
    if (!chips.length) continue
    base = stepIndex * STEP_SPAN
    const firstOf = (list: readonly { id: string; sequence?: number }[]) => {
      const m = new Map<string, number>()
      for (const e of list) if (e.sequence != null) m.set(e.id, Math.min(m.get(e.id) ?? Infinity, e.sequence))
      return m
    }
    created = firstOf(step.created)
    changedAt = firstOf(step.changed)
    removedAt = firstOf(step.removed)
    const sel = selectionKey(step)
    const stepId = `step:${sel}`
    const title = stepTitle(info, step)
    const nature = chips.filter((c) => c.zone === 'nature')
    const result = chips.filter((c) => c.zone === 'result')
    const isSelection = !!step.chosen || step.candidates.length > 0
    const idOf = (c: StepChip) => c.id ?? `${sel}:${c.key}`

    // o nó da etapa nasce na primeira vez que ele é preciso
    const stepNode = () => {
      if (!nodes.has(stepId)) {
        const options = isSelection ? [...nature].sort((a, b) => Number(b.chosen) - Number(a.chosen)) : []
        nodes.set(stepId, {
          kind: 'step',
          id: stepId,
          title,
          phase: info.phase,
          info,
          step,
          options,
          rejected: options.filter((c) => !c.chosen).length,
          order: base,
          width: Math.round(48 + title.length * 6.6),
          height: 26,
        })
      }
      return stepId
    }

    const chosenIds = new Set<string>()
    if (isSelection) {
      stepNode()
      for (const chip of nature) {
        if (!chip.chosen || !chip.id) continue
        putChip(chip, chip.id, info, step)
        chosenIds.add(chip.id)
        link(stepId, chip.id, 'chosen', [{ icon: 'verdict-ok', label: 'selected', at: targetOf(chip, step, null) }])
      }
    } else {
      // o que a etapa usou sem escolher (a intenção que executa) pode ser a causa do que ela fez
      for (const chip of nature) {
        putChip(chip, idOf(chip), info, step, false)
        if (chip.entityType === 'intendedMeans' && chip.id) containment.push([chip.id, ownerOf?.(chip.id) ?? null])
        // sem plano relevante a busca não tem candidatos: o aviso sai da etapa, ligado a ela
        if (step.step === 'FIND_OPTION') link(stepNode(), idOf(chip), 'step')
      }
    }

    // o evento para o qual se busca plano não é um dos candidatos (são planos), mas é o assunto da etapa
    if (step.step === 'FIND_OPTION') {
      const event: EntityRef | undefined = step.inputs.find((i) => i.type === 'event')
      if (event) {
        putChip(refChip(event), event.id, info, step, false)
        link(event.id, stepNode(), 'subject')
      }
    }

    const causesOf = new Map<string, Set<string>>()
    for (const e of step.effects ?? []) causesOf.set(e.entity.id, (causesOf.get(e.entity.id) ?? new Set()).add(e.by))
    // o meio pretendido e a intenção nascem do evento tratado, que é a causa dos efeitos do passo
    const eventCauses = step.step === 'ADD_INTENDED_MEANS' ? new Set((step.effects ?? []).map((e) => e.by)) : new Set<string>()

    for (const chip of result) {
      const id = idOf(chip)
      putChip(chip, id, info, step)
      const badges = changeBadges(chip, step)
      if (chosenIds.has(id)) {
        // escolher já implica a contabilidade do estado (virar "selecionado"), então só criar e remover ganham linha própria
        const own = badges.filter((b) => b.icon !== 'badge-change')
        if (own.length) link(stepId, id, 'step', own)
        continue
      }
      const causes = [...(causesOf.get(id) ?? (chip.isNew ? eventCauses : []))].filter((by) => by !== id && nodes.has(by))
      if (causes.length) for (const by of causes) link(by, id, 'cause', badges)
      else link(stepNode(), id, 'step', badges)
    }
  }

  for (const [goal, owner] of containment) {
    if (owner && !pending.some((p) => (p.source === owner && p.target === goal) || (p.source === goal && p.target === owner))) pending.push({ source: owner, target: goal, role: 'subject', badge: null, order: (nodes.get(goal)?.order ?? 0) + 0.5 })
  }

  const edges: GraphEdge[] = []
  const seen = new Set<string>()
  const pairs = new Map<string, number>()
  for (const e of pending) {
    if (e.source === e.target || !nodes.has(e.source) || !nodes.has(e.target)) continue
    const id = `${e.source}>${e.target}:${e.badge?.icon ?? 'none'}`
    if (seen.has(id)) continue
    seen.add(id)
    const pair = `${e.source}>${e.target}`
    const index = pairs.get(pair) ?? 0
    pairs.set(pair, index + 1)
    edges.push({ id, ...e, index, count: 0 })
  }
  for (const e of edges) e.count = pairs.get(`${e.source}>${e.target}`) ?? 1
  return { nodes: [...nodes.values()], edges }
}
