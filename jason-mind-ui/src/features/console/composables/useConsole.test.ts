import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, ref } from 'vue'
import { consoleService } from '../services/console.service'
import type { ConsoleLine, ConsoleResponse } from '../types/console.types'
import { useConsole } from './useConsole'

vi.mock('../services/console.service', () => ({ consoleService: { list: vi.fn() } }))

const list = vi.mocked(consoleService.list)
/** Respostas em fila para as leituras de página; a sondagem de linhas mais antigas (`limit` 1) responde vazio. */
const queue: ReturnType<typeof page>[] = []
const enqueue = (p: ReturnType<typeof page>) => void queue.push(p)

const line = (n: number): ConsoleLine => ({ agent: 'a', cycle: n, sequence: n, timeMilliseconds: n, text: `t${n}` })

const page = (from: number, count: number) => ({
  status: 200,
  etag: null,
  notModified: false as const,
  data: {
    lines: Array.from({ length: count }, (_, i) => line(from + i)),
    previousCursor: from,
    nextCursor: from + count - 1,
  } satisfies ConsoleResponse,
})

function mountConsole() {
  let api!: ReturnType<typeof useConsole>
  const app = createApp(
    defineComponent({
      setup() {
        api = useConsole({ agents: ref<string[]>([]), until: ref<number | null>(null), live: ref(true), enabled: ref(true) })
        return () => null
      },
    }),
  )
  app.mount(document.createElement('div'))
  return { api, unmount: () => app.unmount() }
}

describe('useConsole ao vivo', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    list.mockReset()
    queue.length = 0
    list.mockImplementation(async (q) => (q.limit === 1 ? page(0, 0) : queue.shift()!))
  })
  afterEach(() => vi.useRealTimers())

  it('acrescenta as linhas novas sem lacuna quando alcança o fluxo', async () => {
    enqueue(page(1, 5))
    enqueue(page(6, 3))
    const { api, unmount } = mountConsole()
    await vi.advanceTimersByTimeAsync(10)
    await vi.advanceTimersByTimeAsync(1100)
    expect(api.lines.value).toHaveLength(8)
    expect(api.gaps.value).toHaveLength(0)
    unmount()
  })

  it('pula para o fim com lacuna depois de 3 páginas cheias e a fecha ao carregar o trecho', async () => {
    enqueue(page(1, 5))
    enqueue(page(100, 200))
    enqueue(page(300, 200))
    enqueue(page(500, 200))
    enqueue(page(5000, 200)) // consulta sem cursor: as mais recentes
    const { api, unmount } = mountConsole()
    await vi.advanceTimersByTimeAsync(10)
    await vi.advanceTimersByTimeAsync(1100)

    expect(api.gaps.value).toEqual([{ sequence: 5000, beforeCursor: 5000 }])
    enqueue(page(4900, 100)) // página incompleta: chegou ao passado carregado
    await api.fillGap(api.gaps.value[0]!)
    expect(api.gaps.value).toHaveLength(0)
    expect(api.lines.value.some((l) => l.sequence === 4900)).toBe(true)
    unmount()
  })
})
