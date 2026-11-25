export interface Moment {
  cycle: number
  iteration: number
  phase: string
  step: string
  ordinal: number
}

export interface LifecycleCause {
  by?: { type: string; id: string; label: string } | null
  sender?: string | null
  location?: { plan: string | null; deed: number | null; line: number | null } | null
}

export interface EntityLabel {
  type: string
  id: string
  label: string
}

export type LifecycleKind = 'START' | 'CHANGE' | 'END' | 'GENERATES' | 'MODIFIES' | 'REMOVES'

export interface LifecycleEntry {
  sequence: number
  kind: LifecycleKind
  code: string | null
  timeline: string | null
  value: string | null
  moment: Moment
  cause: LifecycleCause | null
  entity: EntityLabel | null
  into: EntityLabel | null
  /** A entidade para a qual o valor novo aponta (o evento que a intenção espera, o objetivo empilhado). */
  valueEntity?: EntityLabel | null
}

export interface LifecycleResponse {
  id: string
  type: string
  label: string
  entries: LifecycleEntry[]
  nextCursor: number | string | null
}
