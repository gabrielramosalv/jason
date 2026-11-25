import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { ReasoningResponse } from '../types/reasoning.types'

export const reasoningService = {
  get: (agent: string, cycle: number): Promise<ApiResponse<ReasoningResponse>> =>
    httpClient.get<ReasoningResponse>(`agents/${encodeURIComponent(agent)}/cycles/${cycle}/reasoning`),
}
