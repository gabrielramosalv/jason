import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { ConsoleQuery, ConsoleResponse } from '../types/console.types'

export const consoleService = {
  list: (query: ConsoleQuery): Promise<ApiResponse<ConsoleResponse>> =>
    httpClient.get<ConsoleResponse>('console', {
      query: {
        agents: query.agents?.length ? query.agents.join(',') : undefined,
        until: query.until,
        after: query.after,
        before: query.before,
        limit: query.limit,
      },
    }),
}
