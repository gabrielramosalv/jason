export interface ConsoleLine {
  agent: string
  cycle: number
  sequence: number
  timeMilliseconds: number
  text: string
}

export interface ConsoleResponse {
  lines: ConsoleLine[]
  previousCursor: number | string | null
  nextCursor: number | string | null
}

export interface ConsoleQuery {
  agents?: string[]
  until?: number | null
  after?: number | string | null
  before?: number | string | null
  limit: number
}
