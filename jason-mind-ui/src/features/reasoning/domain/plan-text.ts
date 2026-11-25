import type { Plan } from '@/features/inspector/types/state.types'

const TRIGGER_MARK: Record<string, string> = { BELIEF: '', ACHIEVEMENT: '!', TEST: '?', ACHIEVEMENT_GOAL: '!', TEST_GOAL: '?' }

/** Tira um par de parênteses que envolve o termo todo, como `(Status == order)`. */
function unwrap(term: string): string {
  if (!term.startsWith('(') || !term.endsWith(')')) return term
  let depth = 0
  for (let i = 0; i < term.length - 1; i++) {
    if (term[i] === '(') depth++
    else if (term[i] === ')') depth--
    if (depth === 0) return term
  }
  return term.slice(1, -1)
}

/** O gatilho do plano, como `+!orderFood` ou `+choice(Option)`. */
export const planTrigger = (plan: Plan) => `${plan.trigger.operator}${TRIGGER_MARK[plan.trigger.type] ?? ''}${plan.trigger.literal}`

/** O termo de topo do contexto na posição dada (a mesma da avaliação do plano), ou nulo se o plano não o tem. */
export const contextTerm = (plan: Plan, index: number): string | null => (plan.context[index] ? unwrap(plan.context[index]) : null)

/** O contexto do plano ligado por `&`; vazio quando o plano não tem contexto. */
export const planContext = (plan: Plan) => plan.context.map(unwrap).join(' & ')

/** Gatilho e contexto, como se escreve no programa: `+choice(Option) : service(...) & Status == order`. */
export const planText = (plan: Plan) => (plan.context.length ? `${planTrigger(plan)} : ${planContext(plan)}` : planTrigger(plan))
