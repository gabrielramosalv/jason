export type EntityType = 'message' | 'event' | 'plan' | 'intention' | 'intendedMeans' | 'action' | 'belief' | string

export interface EntityRef {
  type: EntityType
  id: string
  label: string
}

export interface TermBinding {
  name: string
  value: string
}

export interface PlanTerm {
  index: number
  verdict: 'TRUE' | 'FALSE' | 'NOT_CHECKED' | string
  bindings: TermBinding[]
}

export interface Candidate extends EntityRef {
  rule: string | null
  chosen: boolean
  terms: PlanTerm[] | null
}

/** A entidade criada ou removida num passo, com a sequência da ocorrência (a ordem total dos fatos dentro do agente). */
export interface OccurrenceRef extends EntityRef {
  sequence?: number
}

export interface EntityChangeRef extends EntityRef {
  timeline: string
  value: string
  sequence?: number
}

export type PhaseName = 'SENSE' | 'DELIBERATE' | 'ACT'

/** O que uma entidade do passo fez a outra: gerou, modificou ou removeu. */
export interface StepEffect {
  kind: 'GENERATES' | 'MODIFIES' | 'REMOVES'
  by: string
  entity: EntityRef
  sequence?: number
}

export interface ReasoningStep {
  step: string
  phase: PhaseName
  iteration: number
  ordinal: number
  searchMethod: string | null
  inputs: EntityRef[]
  chosen: EntityRef | null
  candidates: Candidate[]
  created: OccurrenceRef[]
  changed: EntityChangeRef[]
  /** Ausente em servidores antigos. */
  effects?: StepEffect[]
  removed: OccurrenceRef[]
}

export interface ReasoningResponse {
  agent: string
  cycle: number
  steps: ReasoningStep[]
}
