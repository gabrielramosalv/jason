import { computed, shallowRef, watch } from 'vue'
import type { Ref } from 'vue'
import { usePolling } from '@/shared/composables/usePolling'
import { messagesService } from '../services/messages.service'
import type { MasMessage, MessagesResponse } from '../types/message.types'

/** Parâmetros da D-156 (provisórios, a medir na UI real). */
export const LIVE_PAGE_LIMIT = 200
export const LIVE_PAGES_PER_TICK = 3
const TICK_MS = 1000
const MAX_ROWS = 3000

export interface MessageRow {
  kind: 'message'
  message: MasMessage
}
export interface GapRow {
  kind: 'gap'
  id: string
  omitted: number
  beforeCursor: string | null
}
export type Row = MessageRow | GapRow

const sentTotal = (res: MessagesResponse) => res.counts.reduce((sum, c) => sum + c.sent, 0)

interface Options {
  agents: Ref<string[]>
  /** Corte `sequenceEnd` da viagem no tempo; usado só fora do modo ao vivo [D-128]. */
  until: Ref<number | null>
  live: Ref<boolean>
  enabled: Ref<boolean>
}

/**
 * Lista de mensagens do MAS. Ao vivo segue a D-156: até 3 consultas `after` por tick e, se ainda houver atraso,
 * pula para o fim, avisa quantas ficaram de fora e deixa a lacuna carregável por `before`.
 */
export function useMessages(options: Options) {
  const rows = shallowRef<Row[]>([])
  const counts = shallowRef<MessagesResponse['counts']>([])
  const error = shallowRef<string | null>(null)
  const loading = shallowRef(false)
  const olderCursor = shallowRef<string | null>(null)

  let nextCursor: string | null = null
  let lastTotal = 0
  let initialized = false
  let generation = 0

  const hasOlder = computed(() => olderCursor.value !== null)

  function reset() {
    generation++
    rows.value = []
    counts.value = []
    olderCursor.value = null
    nextCursor = null
    lastTotal = 0
    initialized = false
    error.value = null
  }

  function merge(messages: MasMessage[]) {
    if (!messages.length) return
    const next = rows.value.slice()
    const index = new Map<string, number>()
    next.forEach((r, i) => r.kind === 'message' && index.set(r.message.id, i))
    for (const m of messages) {
      const at = index.get(m.id)
      if (at !== undefined) next[at] = { kind: 'message', message: m }
      else {
        index.set(m.id, next.length)
        next.push({ kind: 'message', message: m })
      }
    }
    rows.value = next.length > MAX_ROWS ? next.slice(next.length - MAX_ROWS) : next
  }

  function query(extra: { after?: string | null; before?: string | null }) {
    return messagesService.list({
      agents: options.agents.value,
      until: options.live.value ? null : options.until.value,
      limit: LIVE_PAGE_LIMIT,
      ...extra,
    })
  }

  async function initial() {
    const mine = generation
    const res = await query({})
    if (mine !== generation || !res.data) return
    initialized = true
    rows.value = res.data.messages.map((message) => ({ kind: 'message', message }))
    counts.value = res.data.counts
    nextCursor = res.data.nextCursor
    olderCursor.value = res.data.previousCursor && res.data.messages.length >= LIVE_PAGE_LIMIT ? res.data.previousCursor : null
    lastTotal = sentTotal(res.data)
  }

  async function tick() {
    if (!options.enabled.value) return
    const mine = generation
    try {
      if (!initialized) {
        loading.value = true
        await initial()
        return
      }
      if (!options.live.value) return
      let full = false
      for (let page = 0; page < LIVE_PAGES_PER_TICK; page++) {
        const res = await query({ after: nextCursor })
        if (mine !== generation || !res.data) return
        merge(res.data.messages)
        counts.value = res.data.counts
        lastTotal = sentTotal(res.data)
        if (res.data.nextCursor) nextCursor = res.data.nextCursor
        full = res.data.messages.length >= LIVE_PAGE_LIMIT
        if (!full) break
      }
      if (full) await jumpToEnd(mine)
      error.value = null
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : 'Falha ao ler as mensagens.'
      throw cause
    } finally {
      loading.value = false
    }
  }

  /** Atraso persistente: mostra as mais recentes e registra a lacuna no ponto do corte. */
  async function jumpToEnd(mine: number) {
    const res = await query({})
    if (mine !== generation || !res.data) return
    const total = sentTotal(res.data)
    const known = new Set(rows.value.flatMap((r) => (r.kind === 'message' ? [r.message.id] : [])))
    const fresh = res.data.messages.filter((m) => !known.has(m.id))
    const omitted = Math.max(0, total - lastTotal - fresh.length)
    const updates = new Map(res.data.messages.map((m) => [m.id, m]))
    const kept = rows.value.map((r) => (r.kind === 'message' && updates.has(r.message.id) ? ({ kind: 'message', message: updates.get(r.message.id)! } as MessageRow) : r))
    const gap: GapRow[] = omitted > 0 ? [{ kind: 'gap', id: `gap-${total}`, omitted, beforeCursor: res.data.previousCursor }] : []
    const merged = [...kept, ...gap, ...fresh.map((message) => ({ kind: 'message', message }) as MessageRow)]
    rows.value = merged.length > MAX_ROWS ? merged.slice(merged.length - MAX_ROWS) : merged
    counts.value = res.data.counts
    lastTotal = total
    nextCursor = res.data.nextCursor ?? nextCursor
  }

  /** Carrega um trecho da lacuna (do fim para trás), por `before`. */
  async function fillGap(gap: GapRow) {
    if (!gap.beforeCursor) {
      rows.value = rows.value.filter((r) => r !== gap)
      return
    }
    const mine = generation
    const res = await query({ before: gap.beforeCursor })
    if (mine !== generation || !res.data) return
    const known = new Set(rows.value.flatMap((r) => (r.kind === 'message' ? [r.message.id] : [])))
    const older = res.data.messages.filter((m) => !known.has(m.id))
    const at = rows.value.indexOf(gap)
    const remaining = gap.omitted - older.length
    const replacement: Row[] = [
      ...(remaining > 0 && res.data.previousCursor && res.data.messages.length >= LIVE_PAGE_LIMIT
        ? [{ kind: 'gap', id: gap.id, omitted: remaining, beforeCursor: res.data.previousCursor } as GapRow]
        : []),
      ...older.map((message) => ({ kind: 'message', message }) as MessageRow),
    ]
    const next = rows.value.slice()
    next.splice(at, 1, ...replacement)
    rows.value = next
  }

  /** Página mais antiga que a primeira da lista (navegação para trás). */
  async function loadOlder() {
    if (!olderCursor.value) return
    const mine = generation
    const res = await query({ before: olderCursor.value })
    if (mine !== generation || !res.data) return
    const known = new Set(rows.value.flatMap((r) => (r.kind === 'message' ? [r.message.id] : [])))
    rows.value = [...res.data.messages.filter((m) => !known.has(m.id)).map((message) => ({ kind: 'message', message }) as MessageRow), ...rows.value]
    olderCursor.value = res.data.previousCursor && res.data.messages.length >= LIVE_PAGE_LIMIT ? res.data.previousCursor : null
  }

  const { trigger } = usePolling(tick, TICK_MS, options.enabled)
  // ao vivo o corte (`until`) muda a cada ciclo mas não é usado: vigiá-lo zeraria a lista inteira a cada segundo
  watch([() => options.agents.value.join('|'), () => (options.live.value ? null : options.until.value), options.live, options.enabled], () => {
    reset()
    trigger()
  })

  return { rows, counts, error, loading, hasOlder, fillGap, loadOlder }
}
