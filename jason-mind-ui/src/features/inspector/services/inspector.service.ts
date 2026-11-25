import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { AgentState, PlansResponse, SourcesResponse, StatePart } from '../types/state.types'

const enc = encodeURIComponent

export const inspectorService = {
  /** Estado do agente num ciclo (omitido = o mais novo), só as partes pedidas [D-124]. */
  state(
    agent: string,
    options: { cycle?: number | null; include: StatePart[]; etag?: string | null },
  ): Promise<ApiResponse<AgentState>> {
    return httpClient.get<AgentState>(`agents/${enc(agent)}`, {
      query: { cycle: options.cycle, include: options.include.join(',') },
      etag: options.etag,
    })
  },
  sources: (agent: string, etag?: string | null): Promise<ApiResponse<SourcesResponse>> =>
    httpClient.get<SourcesResponse>(`agents/${enc(agent)}/sources`, { etag }),
  plans: (agent: string, etag?: string | null): Promise<ApiResponse<PlansResponse>> =>
    httpClient.get<PlansResponse>(`agents/${enc(agent)}/plans`, { etag }),
}
