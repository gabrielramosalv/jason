export interface ApiErrorPayload {
  error?: string
  message?: string
}

export type QueryValue = string | number | boolean | null | undefined
export type QueryParams = Record<string, QueryValue>

/** Resultado de uma leitura: `notModified` marca o 304 (o corpo vem nulo). */
export interface ApiResponse<T> {
  data: T | null
  status: number
  etag: string | null
  notModified: boolean
}
