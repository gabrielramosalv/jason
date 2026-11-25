import { API_CONTEXT } from '@/config/api-context'
import type { ApiErrorPayload, ApiResponse, QueryParams } from './types'

export class ApiClientError extends Error {
  readonly status: number
  readonly payload?: ApiErrorPayload

  constructor(status: number, message: string, payload?: ApiErrorPayload) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.payload = payload
  }
}

interface RequestOptions {
  query?: QueryParams
  body?: unknown
  etag?: string | null
  signal?: AbortSignal
}

class HttpClient {
  constructor(private readonly baseUrl: string) {}

  get<T>(path: string, options: RequestOptions = {}) {
    return this.request<T>('GET', path, options)
  }

  put<T>(path: string, body: unknown, options: RequestOptions = {}) {
    return this.request<T>('PUT', path, { ...options, body })
  }

  post<T>(path: string, body?: unknown, options: RequestOptions = {}) {
    return this.request<T>('POST', path, { ...options, body })
  }

  delete<T>(path: string, options: RequestOptions = {}) {
    return this.request<T>('DELETE', path, options)
  }

  private async request<T>(method: string, path: string, options: RequestOptions): Promise<ApiResponse<T>> {
    const headers: Record<string, string> = { Accept: 'application/json' }
    // D-155: toda escrita leva application/json, inclusive o POST de poda sem corpo.
    if (method !== 'GET') headers['Content-Type'] = 'application/json'
    if (options.etag) headers['If-None-Match'] = options.etag

    let response: Response
    try {
      response = await fetch(this.buildUrl(path, options.query), {
        method,
        headers,
        body: method === 'GET' ? undefined : (options.body === undefined ? '' : JSON.stringify(options.body)),
        signal: options.signal,
      })
    } catch (cause) {
      if (cause instanceof DOMException && cause.name === 'AbortError') throw cause
      throw new ApiClientError(0, 'Sem conexão com o Mind API.')
    }

    const etag = response.headers.get('ETag')
    if (response.status === 304) return { data: null, status: 304, etag: options.etag ?? etag, notModified: true }

    const text = await response.text()
    const parsed = text ? safeParse(text) : null
    if (!response.ok) {
      const payload = (parsed ?? undefined) as ApiErrorPayload | undefined
      throw new ApiClientError(response.status, payload?.error ?? payload?.message ?? `Erro ${response.status}`, payload)
    }
    return { data: parsed as T, status: response.status, etag, notModified: false }
  }

  private buildUrl(path: string, query?: QueryParams) {
    const base = this.baseUrl.replace(/\/$/, '')
    const resource = path.replace(/^\//, '')
    const search = new URLSearchParams()
    for (const [key, value] of Object.entries(query ?? {})) {
      if (value !== null && value !== undefined && value !== '') search.set(key, String(value))
    }
    const qs = search.toString()
    return `${base}/${resource}${qs ? `?${qs}` : ''}`
  }
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

export const httpClient = new HttpClient(API_CONTEXT)
