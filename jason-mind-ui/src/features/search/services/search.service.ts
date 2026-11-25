import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { SearchResponse, SearchType } from '../types/search.types'

const base = (agent: string) => `agents/${encodeURIComponent(agent)}`

export const searchService = {
  /** Busca por texto nas entidades da janela retida [D-120, D-131]. */
  text: (agent: string, q: string, types: SearchType[], limit = 200): Promise<ApiResponse<SearchResponse>> =>
    httpClient.get<SearchResponse>(`${base(agent)}/search`, {
      query: { q, types: types.length ? types.join(',') : undefined, limit },
    }),
  /** Ciclos em que uma deed foi a atual [D-121]. */
  deed: (agent: string, plan: string, deedIndex: number): Promise<ApiResponse<SearchResponse>> =>
    httpClient.get<SearchResponse>(`${base(agent)}/search/deed`, { query: { plan, deedIndex } }),
}
