import { httpClient } from '@/shared/api'
import type { ApiResponse } from '@/shared/api'
import type { MessageQuery, MessagesResponse } from '../types/message.types'

export const messagesService = {
  list: (query: MessageQuery): Promise<ApiResponse<MessagesResponse>> =>
    httpClient.get<MessagesResponse>('messages', {
      query: {
        agents: query.agents?.length ? query.agents.join(',') : undefined,
        until: query.until,
        after: query.after,
        before: query.before,
        limit: query.limit,
      },
    }),
}
