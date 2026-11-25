import { ApiClientError } from './http-client'

/** Texto para o usuário; 403 e 415 são a restrição de origem das escritas [D-155]. */
export function describeError(cause: unknown): string {
  if (cause instanceof ApiClientError) {
    if (cause.status === 403 || cause.status === 415) return 'O servidor recusou a origem do pedido.'
    if (cause.status === 0) return cause.message
    return cause.message
  }
  return cause instanceof Error ? cause.message : 'Falha inesperada.'
}
