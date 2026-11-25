import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { Metrics, PruneResponse, Settings, SettingsUpdate } from '../types/settings.types'

const enc = encodeURIComponent

export const settingsService = {
  get: (): Promise<ApiResponse<Settings>> => httpClient.get<Settings>('settings'),
  update: (body: SettingsUpdate): Promise<ApiResponse<Settings>> => httpClient.put<Settings>('settings', body),
  updateAgent: (agent: string, retentionCycles: number): Promise<ApiResponse<Settings>> =>
    httpClient.put<Settings>(`agents/${enc(agent)}/settings`, { retentionCycles }),
  resetAgent: (agent: string): Promise<ApiResponse<Settings>> => httpClient.delete<Settings>(`agents/${enc(agent)}/settings`),
  /** A poda só registra o pedido; a thread do agente a aplica no fim do próximo ciclo [D-149]. */
  pruneAgent: (agent: string, until: number): Promise<ApiResponse<PruneResponse>> =>
    httpClient.post<PruneResponse>(`agents/${enc(agent)}/prune`, undefined, { query: { until } }),
  pruneAll: (until: number): Promise<ApiResponse<PruneResponse>> =>
    httpClient.post<PruneResponse>('prune', undefined, { query: { until } }),
  metrics: (): Promise<ApiResponse<Metrics>> => httpClient.get<Metrics>('metrics'),
}
