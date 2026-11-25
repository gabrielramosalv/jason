export interface AgentSummary {
  name: string
  active: boolean
  ended: boolean
  oldestCycle: number | null
  newestCycle: number | null
}

export interface MasInfo {
  mas: string | null
  versions: { mind: string; jason: string }
  agents: AgentSummary[]
}
