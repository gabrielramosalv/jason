export interface CyclePhases {
  sense: boolean
  deliberate: boolean
  act: boolean
}

export interface CycleActivity {
  cycle: number
  phases: CyclePhases
}

export interface CyclesResponse {
  agent: string
  oldestCycle: number
  newestCycle: number
  from: number
  to: number
  cycles: CycleActivity[]
}
