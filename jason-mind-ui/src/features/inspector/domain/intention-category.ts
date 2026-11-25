import type { Intention } from '../types/state.types'

export type IntentionCategory = 'executing' | 'environment' | 'waiting' | 'queue' | 'suspended'

/** Em qual seção do painel Intentions uma intenção cai, a partir do estado que o Mind API informa. */
export function intentionCategory(intention: Intention): IntentionCategory {
  if (intention.executedInCycle) return 'executing'
  const state = intention.state.toUpperCase()
  if (state === 'WAITING_ACTION') return 'environment'
  if (state.startsWith('WAITING')) return 'waiting'
  if (state.startsWith('SUSPENDED')) return 'suspended'
  return 'queue'
}
