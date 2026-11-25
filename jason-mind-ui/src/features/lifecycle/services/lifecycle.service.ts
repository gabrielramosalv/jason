import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { LifecycleResponse } from '../types/lifecycle.types'

export const lifecycleService = {
  /**
   * Ciclo de vida de uma entidade, em páginas de 20 entradas [D-127]. Sem `after`, da mais recente para a mais antiga
   * (`before` continua do ponto em que parou: a poda tira as antigas); com `after`, as entradas mais novas que ele, da mais
   * antiga para a mais nova.
   */
  get: (agent: string, id: string, cursor?: { before?: number | string | null; after?: number | string | null }): Promise<ApiResponse<LifecycleResponse>> =>
    httpClient.get<LifecycleResponse>(`agents/${encodeURIComponent(agent)}/entities/${encodeURIComponent(id)}/lifecycle`, {
      query: cursor?.after != null ? { after: cursor.after, limit: 20, order: 'asc' } : { before: cursor?.before, limit: 20, order: 'desc' },
    }),
}
