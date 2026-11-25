import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, ref } from 'vue'
import type { MasMessage, MessagesResponse } from '../types/message.types'
import { LIVE_PAGES_PER_TICK, LIVE_PAGE_LIMIT, useMessages } from './useMessages'
import type { Row } from './useMessages'
import { messagesService } from '../services/messages.service'

vi.mock('../services/messages.service', () => ({ messagesService: { list: vi.fn() } }))

const list = vi.mocked(messagesService.list)

const msg = (n: number): MasMessage => ({
  id: `m${n}`,
  sender: 'a',
  receiver: 'b',
  performative: 'tell',
  content: `c${n}`,
  inReplyTo: null,
  status: 'read',
  sent: { cycle: n, timeMilliseconds: n },
  arrival: null,
  read: null,
})

const page = (from: number, count: number, total: number): { data: MessagesResponse; status: number; etag: null; notModified: false } => ({
  status: 200,
  etag: null,
  notModified: false,
  data: {
    messages: Array.from({ length: count }, (_, i) => msg(from + i)),
    counts: [{ agent: 'a', sent: total, received: 0 }],
    previousCursor: `prev-${from}`,
    nextCursor: `next-${from + count - 1}`,
  },
})

function mountMessages() {
  const live = ref(true)
  let api!: ReturnType<typeof useMessages>
  const app = createApp(
    defineComponent({
      setup() {
        api = useMessages({ agents: ref<string[]>([]), until: ref<number | null>(null), live, enabled: ref(true) })
        return () => null
      },
    }),
  )
  app.mount(document.createElement('div'))
  return { api, unmount: () => app.unmount() }
}

const messageIds = (rows: Row[]) => rows.flatMap((r) => (r.kind === 'message' ? [r.message.id] : []))

describe('useMessages ao vivo (D-156)', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    list.mockReset()
  })
  afterEach(() => vi.useRealTimers())

  it('alcança o fluxo moderado sem lacuna', async () => {
    list.mockResolvedValueOnce(page(1, 5, 5)) // leitura inicial
    list.mockResolvedValueOnce(page(6, 3, 8)) // tick: página incompleta, alcançou
    const { api, unmount } = mountMessages()
    await vi.advanceTimersByTimeAsync(10)
    expect(messageIds(api.rows.value)).toHaveLength(5)

    await vi.advanceTimersByTimeAsync(1100)
    expect(messageIds(api.rows.value)).toHaveLength(8)
    expect(api.rows.value.some((r) => r.kind === 'gap')).toBe(false)
    expect(list).toHaveBeenCalledTimes(2)
    unmount()
  })

  it('pula para o fim e avisa a lacuna quando 3 páginas cheias não bastam', async () => {
    list.mockResolvedValueOnce(page(1, 5, 5))
    for (let i = 0; i < LIVE_PAGES_PER_TICK; i++) list.mockResolvedValueOnce(page(100 + i * LIVE_PAGE_LIMIT, LIVE_PAGE_LIMIT, 5 + (i + 1) * LIVE_PAGE_LIMIT))
    // consulta sem cursor: as mais recentes, depois de 1 000 mensagens novas no total
    list.mockResolvedValueOnce(page(5000, LIVE_PAGE_LIMIT, 1005))
    const { api, unmount } = mountMessages()
    await vi.advanceTimersByTimeAsync(10)
    await vi.advanceTimersByTimeAsync(1100)

    expect(list).toHaveBeenCalledTimes(1 + LIVE_PAGES_PER_TICK + 1)
    const rows = api.rows.value
    const gap = rows.find((r) => r.kind === 'gap')
    expect(gap).toBeDefined()
    // 1 005 enviadas no total, 605 já mostradas (5 + 600), 200 novas: faltam 200 omitidas
    expect(gap && gap.kind === 'gap' && gap.omitted).toBe(200)
    const gapAt = rows.findIndex((r) => r.kind === 'gap')
    expect(rows[gapAt + 1]).toMatchObject({ kind: 'message', message: { id: 'm5000' } })
    unmount()
  })

  it('carrega a lacuna por before e a remove ao completar', async () => {
    list.mockResolvedValueOnce(page(1, 5, 5))
    for (let i = 0; i < LIVE_PAGES_PER_TICK; i++) list.mockResolvedValueOnce(page(100 + i * LIVE_PAGE_LIMIT, LIVE_PAGE_LIMIT, 5 + (i + 1) * LIVE_PAGE_LIMIT))
    list.mockResolvedValueOnce(page(5000, LIVE_PAGE_LIMIT, 1005))
    const { api, unmount } = mountMessages()
    await vi.advanceTimersByTimeAsync(10)
    await vi.advanceTimersByTimeAsync(1100)

    const gap = api.rows.value.find((r) => r.kind === 'gap')
    if (!gap || gap.kind !== 'gap') throw new Error('sem lacuna')
    list.mockResolvedValueOnce(page(4800, 200, 1005))
    await api.fillGap(gap)

    expect(api.rows.value.some((r) => r.kind === 'gap')).toBe(false)
    expect(messageIds(api.rows.value)).toContain('m4800')
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ before: gap.beforeCursor }))
    unmount()
  })
})
