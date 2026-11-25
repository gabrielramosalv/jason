import type { AgentSummary } from '../types/mas.types'

/** Cor do ponto de estado: ativo, ocioso ou terminado. */
export function agentDot(agent: Pick<AgentSummary, 'active' | 'ended'>): string {
  if (agent.ended) return 'var(--dot-off)'
  return agent.active ? 'var(--c-1a9a3c)' : 'var(--c-d39b2a)'
}
