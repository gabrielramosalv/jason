import { reactive, ref, shallowRef } from 'vue'
import { defineStore } from 'pinia'
import type { AgentState } from '@/features/inspector/types/state.types'

export type BeliefGroup = 'self' | 'msg' | 'env'

/** Estado de leitura compartilhado entre os painéis: o estado do ciclo, a seleção e as seções recolhidas. */
export const useInspectorStore = defineStore('inspector', () => {
  const state = shallowRef<AgentState | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const selectedIntentionId = ref<string | null>(null)
  /** Seções abertas ou recolhidas (chave livre por painel), todas abertas por padrão. */
  const collapsed = reactive<Record<string, boolean>>({})
  /** Filtros marcados no painel Beliefs: nenhum marcado mostra os três grupos; marcados, só os escolhidos. */
  const beliefFilter = reactive<Record<BeliefGroup, boolean>>({ self: false, msg: false, env: false })
  const beliefShown = (group: BeliefGroup) => !Object.values(beliefFilter).some(Boolean) || beliefFilter[group]

  /** O objetivo que abriu a intenção é o primeiro da pilha dela (`goal`); os outros foram chamados por ele (`subgoal`). Sem estado, `goal`. */
  function goalKindOf(intendedMeansId: string | null | undefined): 'goal' | 'subgoal' {
    for (const i of state.value?.intentions ?? []) {
      const at = i.levels.findIndex((l) => l.intendedMeansId === intendedMeansId)
      if (at >= 0) return at === 0 ? 'goal' : 'subgoal'
    }
    return 'goal'
  }
  /** A intenção que tem o objetivo na pilha dela, no estado do ciclo exibido. */
  function intentionOfGoal(intendedMeansId: string): string | null {
    return state.value?.intentions?.find((i) => i.levels.some((l) => l.intendedMeansId === intendedMeansId))?.id ?? null
  }
  function selectIntention(id: string | null) {
    selectedIntentionId.value = id
  }
  function toggleSection(key: string) {
    collapsed[key] = !collapsed[key]
  }
  const isOpen = (key: string) => !collapsed[key]

  return { state, loading, error, selectedIntentionId, collapsed, beliefFilter, beliefShown, goalKindOf, intentionOfGoal, selectIntention, toggleSection, isOpen }
})
