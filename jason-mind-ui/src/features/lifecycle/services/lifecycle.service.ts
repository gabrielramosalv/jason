import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { LifecycleResponse } from '../types/lifecycle.types'

export const lifecycleService = {
  /** Ciclo de vida de uma entidade, em páginas de 20 entradas [D-127]. */
  get: (agent: string, id: string, after?: number | string | null): Promise<ApiResponse<LifecycleResponse>> =>
    httpClient.get<LifecycleResponse>(`agents/${encodeURIComponent(agent)}/entities/${encodeURIComponent(id)}/lifecycle`, {
      query: { after, limit: 20 },
    }),
}
