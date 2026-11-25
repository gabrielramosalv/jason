export interface LiveBeyondHorizon {
  agent: string
  count: number
  oldest: { type: string; entityId: string; label: string; sinceCycle: number } | null
}

export interface AgentSettings {
  agent: string
  retentionCycles: number
  overridden: boolean
}

export interface Settings {
  retentionCycles: number
  memoryBudgetBytes: number
  agents: AgentSettings[]
  liveBeyondHorizon: LiveBeyondHorizon[]
  pruneAppliesAt: string | null
}

export interface SettingsUpdate {
  retentionCycles?: number
  memoryBudgetBytes?: number
}

export interface PruneRequest {
  agent: string
  until: number
}

export interface PruneResponse {
  requests: PruneRequest[]
}

export interface AgentMetrics {
  name: string
  retainedCycles: number
  estimatedHistoryBytes: number
  meanCaptureMicros: number
  entities: number
  timelineEntries: number
  effects: number
}

export interface Metrics {
  heapUsedBytes: number
  heapMaxBytes: number
  agents: AgentMetrics[]
}
