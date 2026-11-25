import { shallowRef, watch } from 'vue'
import type { Ref } from 'vue'
import { usePolling } from '@/shared/composables/usePolling'
import { consoleService } from '../services/console.service'
import type { ConsoleLine } from '../types/console.types'

const PAGE_LIMIT = 200
const PAGES_PER_TICK = 3
const TICK_MS = 1000
const MAX_LINES = 5000

/** Ponto onde o console pulou para o fim por causa do atraso; `beforeCursor` carrega o trecho omitido. */
export interface ConsoleGap {
  sequence: number
  beforeCursor: number | string | null
}

interface Options {
  agents: Ref<string[]>
  until: Ref<number | null>
  live: Ref<boolean>
  enabled: Ref<boolean>
}

/**
 * Console do MAS. Ao vivo pede `after` (ciclos inteiros, D-129) e repete enquanto a página vier cheia; no
 * passado usa `until` (o corte da viagem no tempo) e uma leitura só.
 */
export function useConsole(options: Options) {
  const lines = shallowRef<ConsoleLine[]>([])
  const error = shallowRef<string | null>(null)
  const loading = shallowRef(false)
  const behind = shallowRef(false)
  /** Há linhas mais antigas que as carregadas (o `previousCursor` vem preenchido mesmo sem nada antes, então se pergunta à API). */
  const hasOlder = shallowRef(false)
  /** Lacunas abertas por pulos: a linha que vem logo depois de cada uma é a de `sequence` igual. */
  const gaps = shallowRef<ConsoleGap[]>([])

  let cursor: number | string | null = null
  let initialized = false
  let generation = 0

  function reset() {
    generation++
    lines.value = []
    cursor = null
    initialized = false
    behind.value = false
    hasOlder.value = false
    gaps.value = []
    error.value = null
  }

  /** Pergunta à API se existe ao menos uma linha antes da primeira carregada. */
  async function refreshHasOlder(mine: number) {
    const first = lines.value[0]
    if (!first) {
      hasOlder.value = false
      return
    }
    const res = await consoleService.list({ agents: options.agents.value, limit: 1, before: first.sequence, until: options.live.value ? null : options.until.value })
    if (mine === generation) hasOlder.value = (res.data?.lines.length ?? 0) > 0
  }

  function append(list: ConsoleLine[]) {
    if (!list.length) return
    const merged = [...lines.value, ...list]
    lines.value = merged.length > MAX_LINES ? merged.slice(merged.length - MAX_LINES) : merged
  }

  async function tick() {
    if (!options.enabled.value) return
    const mine = generation
    const base = { agents: options.agents.value, limit: PAGE_LIMIT, until: options.live.value ? null : options.until.value }
    try {
      if (!initialized) {
        loading.value = true
        const res = await consoleService.list(base)
        if (mine !== generation || !res.data) return
        initialized = true
        lines.value = res.data.lines
        cursor = res.data.nextCursor
        void refreshHasOlder(mine)
        return
      }
      if (!options.live.value) return
      for (let page = 0; page < PAGES_PER_TICK; page++) {
        const res = await consoleService.list({ ...base, after: cursor })
        if (mine !== generation || !res.data) return
        append(res.data.lines)
        if (res.data.nextCursor !== null) cursor = res.data.nextCursor
        behind.value = res.data.lines.length >= PAGE_LIMIT
        if (!behind.value) break
      }
      if (behind.value) await jumpToEnd(mine)
      error.value = null
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : 'Falha ao ler o console.'
      throw cause
    } finally {
      loading.value = false
    }
  }

  /** Mesma regra da D-156 para o console: 3 páginas cheias não bastaram, então pula para o fim e marca a lacuna. */
  async function jumpToEnd(mine: number) {
    const res = await consoleService.list({ agents: options.agents.value, limit: PAGE_LIMIT })
    if (mine !== generation || !res.data) return
    const known = new Set(lines.value.map((l) => l.sequence))
    const fresh = res.data.lines.filter((l) => !known.has(l.sequence))
    const first = fresh[0]
    if (first) gaps.value = [...gaps.value, { sequence: first.sequence, beforeCursor: res.data.previousCursor }]
    append(fresh)
    if (res.data.nextCursor !== null) cursor = res.data.nextCursor
    behind.value = false
  }

  /** Carrega um trecho da lacuna por `before` e a fecha quando a página vier incompleta. */
  async function fillGap(gap: ConsoleGap) {
    const res = await consoleService.list({ agents: options.agents.value, limit: PAGE_LIMIT, before: gap.beforeCursor })
    if (!res.data) return
    const known = new Set(lines.value.map((l) => l.sequence))
    const older = res.data.lines.filter((l) => !known.has(l.sequence))
    lines.value = [...lines.value, ...older].sort((a, b) => a.sequence - b.sequence)
    const reachedPast = res.data.lines.length < PAGE_LIMIT || res.data.previousCursor === null
    gaps.value = gaps.value.flatMap((g) => (g !== gap ? [g] : reachedPast ? [] : [{ sequence: older[0]?.sequence ?? gap.sequence, beforeCursor: res.data!.previousCursor }]))
  }

  async function loadOlder() {
    const first = lines.value[0]
    if (!first) return
    const res = await consoleService.list({ agents: options.agents.value, limit: PAGE_LIMIT, before: first.sequence, until: options.live.value ? null : options.until.value })
    if (!res.data) return
    const known = new Set(lines.value.map((l) => l.sequence))
    lines.value = [...res.data.lines.filter((l) => !known.has(l.sequence)), ...lines.value]
    void refreshHasOlder(generation)
  }

  const { trigger } = usePolling(tick, TICK_MS, options.enabled)
  // ao vivo o corte (`until`) muda a cada ciclo mas não é usado: vigiá-lo zeraria a lista inteira a cada segundo
  watch([() => options.agents.value.join('|'), () => (options.live.value ? null : options.until.value), options.live, options.enabled], () => {
    reset()
    trigger()
  })

  return { lines, error, loading, behind, hasOlder, gaps, fillGap, loadOlder }
}
