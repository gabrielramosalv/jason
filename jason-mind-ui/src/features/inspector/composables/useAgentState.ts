import { computed, shallowRef, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ApiClientError } from '@/shared/api'
import { usePolling } from '@/shared/composables/usePolling'
import { useInspectorStore } from '@/stores/inspector.store'
import { useLayoutStore } from '@/stores/layout.store'
import { useSessionStore } from '@/stores/session.store'
import { inspectorService } from '../services/inspector.service'
import type { AgentState, StatePart } from '../types/state.types'

const LIVE_INTERVAL_MS = 1000

/**
 * Estado do agente selecionado no ciclo mostrado. Pede só as partes visíveis (D-022): a `intentions` é
 * obrigatória com o código à vista, e a `events` só entra com a aba aberta. Ao vivo, consulta a cada
 * segundo com `If-None-Match`, então um ciclo igual custa um 304.
 */
export function useAgentState() {
  const session = useSessionStore()
  const layout = useLayoutStore()
  const inspector = useInspectorStore()
  const { selectedAgent, cycle, isLive } = storeToRefs(session)

  const state = shallowRef<AgentState | null>(null)
  const error = shallowRef<string | null>(null)
  const isLoading = shallowRef(false)

  const include = computed<StatePart[]>(() => {
    const v = layout.visible
    const parts: StatePart[] = []
    if (v.bel) parts.push('beliefs')
    if (v.int || v.code) parts.push('intentions')
    if (v.ev) parts.push('events')
    // sem painel de estado à vista o ciclo mostrado ainda precisa andar (Timeline, Console, Reasoning): pede a parte mais leve
    if (!parts.length) parts.push('intentions')
    return parts
  })

  let etag: string | null = null
  let key = ''
  let generation = 0

  async function fetchOnce() {
    const agent = selectedAgent.value
    if (!agent) return
    const parts = include.value
    if (parts.length === 0) {
      state.value = null
      return
    }
    const nextKey = `${agent}|${cycle.value ?? 'live'}|${parts.join(',')}`
    if (nextKey !== key) {
      key = nextKey
      etag = null
    }
    const mine = ++generation
    try {
      const res = await inspectorService.state(agent, { cycle: cycle.value, include: parts, etag })
      if (mine !== generation) return
      error.value = null
      if (res.notModified || !res.data) return
      etag = res.etag
      state.value = res.data
      session.shown = {
        cycle: res.data.cycle,
        sequenceEnd: res.data.sequenceEnd,
        oldestCycle: res.data.oldestCycle,
        newestCycle: res.data.newestCycle,
      }
    } catch (cause) {
      if (mine !== generation) return
      if (cause instanceof ApiClientError && cause.status === 410) {
        // ciclo podado: volta ao mais antigo que ainda existe
        const oldest = session.selected?.oldestCycle
        if (oldest != null) session.goToCycle(oldest)
        else session.goLive()
        return
      }
      error.value = cause instanceof Error ? cause.message : 'Falha ao ler o estado.'
      throw cause
    }
  }

  async function load() {
    isLoading.value = true
    try {
      await fetchOnce()
    } finally {
      isLoading.value = false
    }
  }

  // fora do modo ao vivo a leitura é única; o polling só roda ao vivo
  const { trigger } = usePolling(fetchOnce, LIVE_INTERVAL_MS, isLive)
  watch([selectedAgent, cycle, include], () => {
    if (selectedAgent.value !== state.value?.agent) state.value = null
    if (isLive.value) trigger()
    else void load()
  })
  if (!isLive.value) void load()

  // os painéis leem o estado pelo store compartilhado
  watch([state, error, isLoading], () => {
    inspector.state = state.value
    inspector.error = error.value
    inspector.loading = isLoading.value
  }, { immediate: true })

  return { state, error, isLoading, include, reload: load }
}
