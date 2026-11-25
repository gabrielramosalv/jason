export type SearchType = 'belief' | 'intention' | 'event' | 'message' | 'action'

export interface SearchHit {
  type: SearchType | 'deed' | string
  entityId: string | null
  label: string
  firstCycle: number
  lastCycle: number | null
}

export interface SearchResponse {
  agent: string
  query: string | null
  window: { oldestCycle: number; newestCycle: number }
  total: number
  hits: SearchHit[]
}
