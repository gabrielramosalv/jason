import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { CyclesResponse } from '../types/cycle.types'

export const cyclesService = {
  /** Atividade por fase de um intervalo de ciclos, sem estado [D-125]. */
  range: (agent: string, from: number, to: number): Promise<ApiResponse<CyclesResponse>> =>
    httpClient.get<CyclesResponse>(`agents/${encodeURIComponent(agent)}/cycles`, { query: { from, to } }),
}
