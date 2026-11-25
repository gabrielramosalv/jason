import type { PhaseName } from '../types/reasoning.types'

export interface StepInfo {
  step: string
  phase: PhaseName
  title: string
  /** O que o passo faz, para o cartão de explicação. */
  rule: string
  /** Texto do passo vazio. */
  idle: string
}

/** Os 9 passos do ciclo em três fases, na ordem em que o diagrama os mostra. */
export const STEPS: StepInfo[] = [
  { step: 'PERCEIVE', phase: 'SENSE', title: 'Perceive environment', idle: 'No changes', rule: 'Reads the percepts of the environment and compares them with the belief base: each addition or removal becomes an event.' },
  { step: 'PROCESS_MESSAGES', phase: 'SENSE', title: 'Read message', idle: 'No message', rule: 'Takes the messages of the mailbox and turns them into events or beliefs, according to the performative.' },
  { step: 'SELECT_EVENT', phase: 'DELIBERATE', title: 'Select event', idle: 'No event', rule: 'Takes the oldest pending event of the queue (FIFO), unless the agent overrides the selection.' },
  { step: 'FIND_OPTION', phase: 'DELIBERATE', title: 'Select plan', idle: 'No plan needed', rule: 'Looks for the relevant plans of the event in the order of the file and picks the first whose context holds true.' },
  { step: 'ADD_INTENDED_MEANS', phase: 'DELIBERATE', title: 'Create intention', idle: 'Nothing to add', rule: 'Pushes the chosen plan on an intention: a new one for a belief or goal event, the same one for a subgoal.' },
  { step: 'PROCESS_ACTIONS', phase: 'ACT', title: 'Handle actions', idle: 'No action returned', rule: 'Reads the result of an external action: on success the intention goes on, on failure it is dropped or recovered.' },
  { step: 'SELECT_INTENTION', phase: 'ACT', title: 'Select intention', idle: 'None eligible', rule: 'Picks the next intention of the queue (round-robin): one intention runs one step per cycle.' },
  { step: 'EXECUTE_INTENTION', phase: 'ACT', title: 'Execute step', idle: 'Nothing to execute', rule: 'Executes the current formula of the top plan of the selected intention: an action, a goal, a belief change or a test.' },
  { step: 'CLEAR_INTENTION', phase: 'ACT', title: 'Clear intention', idle: 'Nothing to clear', rule: 'Removes the plan that finished and, when the stack empties, the intention itself.' },
]

export const PHASES: { phase: PhaseName; title: string; icon: 'phase-sense' | 'phase-deliberate' | 'phase-act' }[] = [
  { phase: 'SENSE', title: 'Sense', icon: 'phase-sense' },
  { phase: 'DELIBERATE', title: 'Deliberate', icon: 'phase-deliberate' },
  { phase: 'ACT', title: 'Act', icon: 'phase-act' },
]

const RULES: Record<string, string> = {
  SELECT_EVENT_FIFO: 'Takes the oldest pending event (FIFO queue).',
  SELECT_INTENTION_ROUND_ROBIN: 'Takes the next intention of the queue (round-robin).',
  PLAN_FIRST_FOUND: 'Applicable and selected: the first applicable plan in the order of the file.',
  PLAN_NOT_EVALUATED: 'Not evaluated: an earlier plan was already applicable.',
}

export function ruleText(code: string | null): string | null {
  if (!code) return null
  return RULES[code] ?? code.toLowerCase().replace(/_/g, ' ')
}

export function stepTitle(step: string): string {
  return STEPS.find((s) => s.step === step)?.title ?? step.toLowerCase().replace(/_/g, ' ')
}
