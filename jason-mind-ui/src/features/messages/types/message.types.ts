export type MessageStatus = 'inFlight' | 'arrived' | 'read' | 'failed'

export interface Occurrence {
  cycle: number
  timeMilliseconds: number
}

export interface MasMessage {
  id: string
  sender: string
  receiver: string
  performative: string
  content: string
  inReplyTo: string | null
  status: MessageStatus
  sent: Occurrence | null
  arrival: Occurrence | null
  read: Occurrence | null
}

export interface MessageCount {
  agent: string
  sent: number
  received: number
}

export interface MessagesResponse {
  messages: MasMessage[]
  counts: MessageCount[]
  previousCursor: string | null
  nextCursor: string | null
}

export interface MessageQuery {
  agents?: string[]
  until?: number | null
  after?: string | null
  before?: string | null
  limit: number
}
