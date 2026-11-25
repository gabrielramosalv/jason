import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { MasInfo } from '../types/mas.types'

export const masService = {
  /** Lista leve de agentes, com ETag (REQ-007, D-123). */
  get: (etag?: string | null): Promise<ApiResponse<MasInfo>> => httpClient.get<MasInfo>('mas', { etag }),
}
