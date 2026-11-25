import { shallowRef, watch } from 'vue'
import type { Ref } from 'vue'
import { ApiClientError } from '@/shared/api'
import { reasoningService } from '../services/reasoning.service'
import type { ReasoningResponse } from '../types/reasoning.types'

const MIN_GAP_MS = 700

/**
 * Raciocínio do ciclo mostrado: uma leitura por ciclo (D-022, grupo B). Ao vivo o ciclo muda a cada instante,
 * então as leituras ficam a pelo menos 700 ms uma da outra.
 */
export function useReasoning(agent: Ref<string | null>, cycle: Ref<number | null>, enabled: Ref<boolean>) {
  const data = shallowRef<ReasoningResponse | null>(null)
  const error = shallowRef<string | null>(null)
  const loading = shallowRef(false)

  let last = 0
  let timer: ReturnType<typeof setTimeout> | null = null
  let generation = 0

  async function fetchNow() {
    const a = agent.value
    const c = cycle.value
    if (!a || c == null || !enabled.value) return
    const mine = ++generation
    last = Date.now()
    loading.value = true
    try {
      const res = await reasoningService.get(a, c)
      if (mine !== generation) return
      data.value = res.data
      error.value = null
    } catch (cause) {
      if (mine !== generation) return
      data.value = null
      error.value = cause instanceof ApiClientError && cause.status === 410 ? 'Ciclo podado.' : cause instanceof Error ? cause.message : 'Falha ao ler o raciocínio.'
    } finally {
      if (mine === generation) loading.value = false
    }
  }

  function schedule() {
    if (timer) clearTimeout(timer)
    const wait = Math.max(0, MIN_GAP_MS - (Date.now() - last))
    timer = setTimeout(() => void fetchNow(), wait)
  }

  watch([agent, cycle, enabled], schedule, { immediate: true })

  return { data, error, loading }
}
