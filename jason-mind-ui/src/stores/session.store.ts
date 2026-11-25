import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { masService } from '@/features/agents/services/mas.service'
import type { AgentSummary, MasInfo } from '@/features/agents/types/mas.types'

/** Cursor da viagem no tempo do agente selecionado: o ciclo mostrado e o corte de sequência [D-128]. */
export interface ShownCycle {
  cycle: number
  sequenceEnd: number
  oldestCycle: number
  newestCycle: number
}

export const useSessionStore = defineStore('session', () => {
  const mas = ref<MasInfo | null>(null)
  const masEtag = ref<string | null>(null)
  const masError = ref<string | null>(null)
  const selectedAgent = ref<string | null>(null)
  /** Ciclo escolhido pelo usuário; nulo é o modo ao vivo (acompanha o mais novo). */
  const cycle = ref<number | null>(null)
  const shown = ref<ShownCycle | null>(null)

  const agents = computed<AgentSummary[]>(() => mas.value?.agents ?? [])
  const selected = computed(() => agents.value.find((a) => a.name === selectedAgent.value) ?? null)
  const isLive = computed(() => cycle.value === null)

  async function refreshMas() {
    try {
      const res = await masService.get(masEtag.value)
      masError.value = null
      if (res.notModified || !res.data) return
      mas.value = res.data
      masEtag.value = res.etag
      if (!selectedAgent.value || !res.data.agents.some((a) => a.name === selectedAgent.value)) {
        const first = res.data.agents[0]?.name ?? null
        if (first !== selectedAgent.value) selectAgent(first)
      }
      // a poda levou o ciclo escolhido embora: vai ao mais antigo que ainda existe
      const oldest = selected.value?.oldestCycle
      if (cycle.value !== null && oldest != null && cycle.value < oldest) cycle.value = oldest
    } catch (cause) {
      masError.value = cause instanceof Error ? cause.message : 'Falha ao consultar o MAS.'
      throw cause
    }
  }

  function selectAgent(name: string | null) {
    if (name === selectedAgent.value) return
    selectedAgent.value = name
    cycle.value = null
    shown.value = null
  }

  function goToCycle(value: number) {
    const s = selected.value
    let target = Math.trunc(value)
    if (s?.oldestCycle != null) target = Math.max(s.oldestCycle, target)
    if (s?.newestCycle != null && target >= s.newestCycle) {
      cycle.value = null
      return
    }
    cycle.value = target
  }

  function goLive() {
    cycle.value = null
  }

  function step(delta: number) {
    const base = shown.value?.cycle ?? selected.value?.newestCycle
    if (base == null) return
    goToCycle(base + delta)
  }

  return { mas, masError, agents, selected, selectedAgent, cycle, shown, isLive, refreshMas, selectAgent, goToCycle, goLive, step }
})
