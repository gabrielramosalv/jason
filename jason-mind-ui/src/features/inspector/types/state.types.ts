export type StatePart = 'beliefs' | 'intentions' | 'events'

export type EntityChange = 'new' | 'removed' | null

export interface Belief {
  id: string
  content: string
  annotations: string[]
  categories: string[]
  change: EntityChange
}

export interface IntentionLevel {
  intendedMeansId: string
  kind: string
  trigger: string
}

export interface Intention {
  id: string
  state: string
  executedInCycle: boolean
  waitingOn: { type: string; id: string; label: string } | null
  levels: IntentionLevel[]
}

export interface Binding {
  name: string
  value: string
}

export interface IntendedMeans {
  id: string
  intentionId: string
  planId: string
  currentDeed: number | null
  bindings: Binding[]
}

export interface AgentEvent {
  id: string
  trigger: string
  atomic: boolean
  place: string
  state: string
  change: EntityChange
}

export interface AgentState {
  agent: string
  cycle: number
  clamped: boolean
  oldestCycle: number
  newestCycle: number
  sequenceEnd: number
  beliefs?: Belief[]
  intentions?: Intention[]
  intendedMeans?: IntendedMeans[]
  events?: AgentEvent[]
}

export interface PlanDeed {
  type: string
  content: string
  line: number
  parent: number
}

export interface Plan {
  id: string
  label: string
  trigger: { operator: string; type: string; literal: string }
  context: string[]
  source: { path: string; line: number }
  annotations: string[]
  orderKey: number
  alive: { fromCycle: number; toCycle: number | null }
  body: PlanDeed[]
}

export interface PlansResponse {
  agent: string
  version: number
  plans: Plan[]
}

export interface SourceFile {
  path: string
  content: string
}

export interface SourcesResponse {
  agent: string
  files: SourceFile[]
}
