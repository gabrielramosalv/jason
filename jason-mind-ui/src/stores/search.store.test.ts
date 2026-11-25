import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSearchStore } from './search.store'

describe('search store: hitCycle', () => {
  beforeEach(() => setActivePinia(createPinia()))

  function withHits(ranges: [number, number | null][]) {
    const s = useSearchStore()
    s.result = { agent: 'a', query: 'x', window: { oldestCycle: 0, newestCycle: 100 }, total: ranges.length, hits: ranges.map(([a, b], i) => ({ type: 'belief', entityId: `b${i}`, label: 'x', firstCycle: a, lastCycle: b })) }
    return s
  }

  it('dentro de um intervalo de resultado anda de ciclo em ciclo', () => {
    const s = withHits([[10, 20]])
    expect(s.hitCycle(12, 1)).toBe(13)
    expect(s.hitCycle(12, -1)).toBe(11)
  })

  it('fora do intervalo salta para o próximo ou o anterior resultado', () => {
    const s = withHits([[10, 12], [40, 45]])
    expect(s.hitCycle(12, 1)).toBe(40)
    expect(s.hitCycle(40, -1)).toBe(12)
    expect(s.hitCycle(45, 1)).toBeNull()
    expect(s.hitCycle(10, -1)).toBeNull()
  })

  it('sem resultado não há para onde saltar', () => {
    expect(useSearchStore().hitCycle(5, 1)).toBeNull()
  })
})
