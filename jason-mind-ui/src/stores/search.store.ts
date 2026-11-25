import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { searchService } from '@/features/search/services/search.service'
import type { SearchHit, SearchResponse, SearchType } from '@/features/search/types/search.types'
import { useSessionStore } from './session.store'

export const ALL_TYPES: SearchType[] = ['belief', 'intention', 'event', 'message', 'action']

/** Busca no histórico retido do agente selecionado: por texto [D-120] ou por deed [D-121]. */
export const useSearchStore = defineStore('search', () => {
  const session = useSessionStore()

  const query = ref('')
  const types = ref<SearchType[]>([...ALL_TYPES])
  const result = ref<SearchResponse | null>(null)
  const index = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)
  /** Origem dos resultados: texto digitado ou a deed escolhida na lupa do código. */
  const mode = ref<'text' | 'deed'>('text')
  /** Com a busca ativa, os botões de ciclo anterior e próximo andam só pelos ciclos com resultado. */
  const onlyHits = ref(false)
  let generation = 0

  const hits = computed<SearchHit[]>(() => result.value?.hits ?? [])
  const total = computed(() => result.value?.total ?? 0)
  const current = computed(() => hits.value[index.value] ?? null)
  const active = computed(() => result.value !== null)

  async function run() {
    const agent = session.selectedAgent
    const q = query.value.trim()
    mode.value = 'text'
    if (!agent || !q) {
      clear(false)
      return
    }
    const mine = ++generation
    loading.value = true
    try {
      const res = await searchService.text(agent, q, types.value)
      if (mine !== generation || !res.data) return
      result.value = res.data
      index.value = 0
      error.value = null
    } catch (cause) {
      if (mine === generation) error.value = cause instanceof Error ? cause.message : 'Falha na busca.'
    } finally {
      if (mine === generation) loading.value = false
    }
  }

  async function runDeed(plan: string, deedIndex: number, content: string) {
    const agent = session.selectedAgent
    if (!agent) return
    const mine = ++generation
    query.value = content
    mode.value = 'deed'
    loading.value = true
    try {
      const res = await searchService.deed(agent, plan, deedIndex)
      if (mine !== generation || !res.data) return
      result.value = res.data
      index.value = 0
      error.value = null
    } catch (cause) {
      if (mine === generation) error.value = cause instanceof Error ? cause.message : 'Falha na busca.'
    } finally {
      if (mine === generation) loading.value = false
    }
  }

  function clear(resetQuery = true) {
    generation++
    if (resetQuery) query.value = ''
    mode.value = 'text'
    result.value = null
    index.value = 0
    error.value = null
    loading.value = false
  }

  function go(delta: number) {
    if (!hits.value.length) return
    index.value = (index.value + delta + hits.value.length) % hits.value.length
    const hit = hits.value[index.value]
    if (hit) session.goToCycle(hit.firstCycle)
  }

  function select(i: number) {
    index.value = i
    const hit = hits.value[i]
    if (hit) session.goToCycle(hit.firstCycle)
  }

  /** Ciclo seguinte (+1) ou anterior (-1) com resultado, a partir de `from`; nulo se não houver. */
  function hitCycle(from: number, direction: 1 | -1): number | null {
    const ranges = hits.value.map((h) => [h.firstCycle, h.lastCycle ?? Number.MAX_SAFE_INTEGER] as const)
    const covers = (c: number) => ranges.some(([a, b]) => c >= a && c <= b)
    if (!ranges.length) return null
    const starts = ranges.map(([a]) => a).sort((x, y) => x - y)
    if (direction === 1) {
      if (covers(from + 1)) return from + 1
      return starts.find((a) => a > from) ?? null
    }
    if (covers(from - 1)) return from - 1
    const ends = ranges.map(([, b]) => b).filter((b) => b < from).sort((x, y) => y - x)
    return ends[0] ?? null
  }

  function toggleType(type: SearchType) {
    types.value = types.value.includes(type) ? types.value.filter((t) => t !== type) : [...types.value, type]
  }

  return { query, types, result, hits, total, current, index, loading, error, mode, onlyHits, hitCycle, active, run, runDeed, clear, go, select, toggleType }
})
