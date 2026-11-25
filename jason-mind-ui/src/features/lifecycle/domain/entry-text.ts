import { numericId } from '@/shared/domain/ids'
import type { LifecycleEntry } from '../types/lifecycle.types'

/**
 * Texto de cada entrada do Life cycle, escrito para quem lê o histórico de uma entidade.
 * Vem do código que o servidor já dá à ocorrência (`code`) e, sem código, da linha do tempo que mudou e do
 * valor novo; os efeitos (gerou, modificou, removeu) dependem do tipo da entidade afetada.
 */

/** Como cada tipo de entidade aparece no meio de uma frase, com o artigo. */
const THING: Record<string, string> = {
  belief: 'a belief',
  event: 'an event',
  intention: 'an intention',
  intendedMeans: 'a goal',
  subgoal: 'a subgoal',
  message: 'a message',
  plan: 'a plan',
  action: 'an action',
  timer: 'a timer',
}
const thing = (type: string | undefined) => THING[type ?? ''] ?? 'an entity'

/** `SELECTED_INTENTION` vira `selected intention`. */
const words = (value: string) => value.toLowerCase().replace(/_/g, ' ')

/** Quem mandou, quando a causa é uma mensagem. */
const from = (e: LifecycleEntry) => (e.cause?.sender ? ` from ${e.cause.sender}` : '')

/** O que cada código do servidor quer dizer para a entidade que o recebeu. */
function byCode(code: string, e: LifecycleEntry): string | null {
  switch (code) {
    case 'BELIEF_PERCEIVED':
      return 'Perceived in the environment'
    case 'BELIEF_ADDED_BY_MESSAGE':
      return `Learned from a message${from(e)}`
    case 'BELIEF_ADDED_BY_INTENTION':
      return 'Added by a goal'
    case 'BELIEF_REMOVED_BY_ENVIRONMENT':
      return 'No longer perceived: removed from the belief base'
    case 'BELIEF_REMOVED_BY_INTENTION':
      return 'Removed by a goal'
    case 'MESSAGE_RECEIVED':
      return 'Received by the agent'
    case 'MESSAGE_READ':
      return 'Read and processed'
    case 'EVENT_TRIGGERED_BY_PERCEPTION':
      return 'Triggered by a change in the environment'
    case 'EVENT_TRIGGERED_BY_MESSAGE':
      return `Triggered by a message${from(e)}`
    case 'EVENT_TRIGGERED_BY_INTENTION':
      return 'Triggered by a goal'
    case 'EVENT_TRIGGERED_BY_FAILURE':
      return 'Triggered by a failure'
    case 'EVENT_TRIGGERED_BY_ACTION_FAILURE':
      return 'Triggered by an action that failed'
    case 'EVENT_SELECTED':
      return 'Selected to be handled'
    case 'EVENT_NO_APPLICABLE_PLAN':
      return 'No applicable plan was found'
    case 'EVENT_HANDLED':
      return 'Handled: a plan was chosen for it'
    case 'EVENT_INTENTION_CONTINUED':
      return 'Handled: the intention waiting for it continued'
    case 'EVENT_GOAL_FAILED':
      return 'Goal failed: no plan could achieve it'
    case 'EVENT_DISCARDED':
      return 'Discarded: no plan was interested in it'
    case 'INTENTION_CREATED':
      return 'Created'
    case 'INTENTION_CLEARED':
      return 'Finished and cleared'
    case 'INTENTION_SELECTED':
      return 'Selected to run in this cycle'
    case 'INTENTION_WAITING_EVENT':
      return 'Waiting for an event to be handled'
    case 'INTENTION_BACK_IN_QUEUE':
      return 'Back in the queue of running intentions'
    case 'INTENTION_WAITING_ACTION':
      return 'Waiting for an action to finish'
    case 'INTENTION_SUSPENDED':
      return 'Suspended'
    case 'INTENTION_SUBGOAL_PUSHED':
      return 'Started a subgoal'
    case 'MESSAGE_GENERATED_BELIEF':
      return 'Added a belief to the agent'
    case 'MESSAGE_GENERATED_EVENT':
      return 'Triggered an event'
    case 'MESSAGE_MODIFIED_BELIEF':
      return 'Updated a belief'
    case 'EVENT_GENERATED_INTENTION':
      return 'Started a new intention'
    case 'EVENT_GENERATED_SUBGOAL':
      return 'Started a subgoal'
    case 'EVENT_GENERATED_FAILURE_EVENT':
      return 'Triggered a failure event'
    case 'EVENT_REQUEUED':
      return 'Put back as a new event'
    default:
      return null
  }
}

/** Mudança de uma linha do tempo sem código: o texto depende de qual linha mudou e do valor novo. */
function byTimeline(e: LifecycleEntry): string {
  const value = e.value ?? ''
  switch (e.timeline) {
    case 'place':
      if (value === 'QUEUE') return 'Put in the event queue'
      if (value === 'PENDING') return 'Set aside as a pending event'
      if (value === 'SELECTED') return 'Selected to be handled'
      if (value === 'NONE') return 'Left the queue of running intentions'
      if (value === 'RUNNING') return 'Back in the queue of running intentions'
      if (value === 'SELECTED_INTENTION') return 'Selected to run in this cycle'
      if (value === 'PENDING_INTENTIONS') return 'Waiting for an event to be handled'
      return `Moved to ${words(value)}`
    case 'state':
      if (value === 'NO_APPLICABLE') return 'No applicable plan was found'
      if (value === 'SELECTED') return 'Selected to be handled'
      if (value === 'PENDING') return 'Waiting to be handled'
      return `Now ${words(value)}`
    case 'waitingOn':
      if (!value || value === 'null') return 'Stopped waiting'
      if (/^e\d+$/.test(value)) return `Waiting for the event #${numericId(value)} to be handled`
      if (/^a\d+$/.test(value)) return 'Waiting for an action to finish'
      return `Waiting for ${value}`
    case 'suspended':
      return value === 'true' ? 'Suspended' : 'Resumed'
    case 'currentDeed': {
      const n = Number(value)
      return Number.isInteger(n) ? `Executing deed ${n + 1} of the plan` : 'Executing the plan'
    }
    case 'annotations':
      return `Annotations are now ${value || 'empty'}`
    case 'sources':
      return `Sources are now ${value || 'empty'}`
    case 'result':
      return `Result: ${value || 'none'}`
    case 'stack':
      return 'Started a subgoal'
    default:
      return e.timeline ? `${words(e.timeline)} is now ${value || 'empty'}` : 'Changed'
  }
}

/** O que o efeito fez à intenção, pelo campo que mudou e pelo valor novo (a janela mostra a intenção logo abaixo). */
function effectOnIntention(e: LifecycleEntry): string {
  const value = e.value ?? ''
  switch (e.timeline) {
    case 'place':
      if (value === 'RUNNING') return 'Put the intention back in the queue'
      if (value === 'NONE') return 'Took the intention out of the queue'
      if (value === 'SELECTED_INTENTION') return 'Gave the intention the turn'
      if (value === 'PENDING_INTENTIONS') return 'Made the intention wait for an event'
      return `Moved the intention to ${words(value)}`
    case 'waitingOn':
      if (!value || value === 'null') return 'Released the intention from its wait'
      if (/^e\d+$/.test(value)) return `Made the intention wait for the event #${numericId(value)}`
      if (/^a\d+$/.test(value)) return 'Made the intention wait for an action'
      return `Made the intention wait for ${value}`
    case 'suspended':
      return value === 'true' ? 'Suspended the intention' : 'Resumed the intention'
    default:
      return 'Changed the intention'
  }
}

/** Efeito sobre outra entidade sem código próprio: a frase vem do tipo da entidade afetada e do que mudou nela. */
function byEffect(e: LifecycleEntry): string {
  const type = e.entity?.type
  switch (e.kind) {
    case 'GENERATES':
      if (type === 'message') return 'Sent a message'
      if (type === 'action') return 'Requested an action'
      if (type === 'event') return 'Triggered an event'
      if (type === 'belief') return 'Added a belief'
      return `Started ${thing(type)}`
    case 'MODIFIES':
      if (type === 'intention') return effectOnIntention(e)
      if (type === 'belief') {
        if (e.timeline === 'annotations') return 'Changed the annotations of a belief'
        if (e.timeline === 'sources') return 'Changed the sources of a belief'
        return 'Updated a belief'
      }
      if (type === 'intendedMeans' || type === 'subgoal') {
        const n = Number(e.value)
        return e.timeline === 'currentDeed' && Number.isInteger(n) ? `Moved a goal to deed ${n + 1}` : 'Changed a goal'
      }
      return `Changed ${thing(type)}`
    case 'REMOVES':
      if (type === 'belief') return 'Removed a belief'
      if (type === 'event') return 'Dropped an event'
      if (type === 'intendedMeans' || type === 'subgoal') return 'Ended a goal'
      return `Removed ${thing(type)}`
    default:
      return 'Changed'
  }
}

/** Começo e fim sem código: depende do tipo da entidade dona da janela e de a entidade vir da carga do programa. */
function byOwner(e: LifecycleEntry, ownerType: string): string {
  const start = e.kind === 'START'
  const loaded = start && e.moment.step === 'INITIAL_LOAD'
  switch (ownerType) {
    case 'belief':
      return loaded ? 'Loaded with the program' : start ? 'Added to the belief base' : 'Removed from the belief base'
    case 'message':
      return start ? 'Sent' : 'Discarded'
    case 'event':
      return loaded ? 'Triggered by the program load' : start ? 'Triggered' : 'Ended'
    case 'intention':
      return start ? 'Created' : 'Finished'
    case 'intendedMeans':
      return start ? 'Started: a plan was chosen for this goal' : 'Finished: its plan ended'
    case 'plan':
      return loaded ? 'Loaded with the program' : start ? 'Added to the plan library' : 'Removed from the plan library'
    default:
      return start ? 'Started' : 'Ended'
  }
}

const sameMoment = (a: LifecycleEntry, b: LifecycleEntry) =>
  a.moment.cycle === b.moment.cycle && a.moment.iteration === b.moment.iteration && a.moment.step === b.moment.step && a.moment.ordinal === b.moment.ordinal

/**
 * Entrada de contabilidade que só repete outra do mesmo momento e da mesma entidade, e por isso não aparece na lista:
 * o evento `state` selecionado (o `place` já diz), a intenção que sai da fila quando passa a esperar (a espera já diz) e a
 * que deixa de esperar quando volta à fila (a volta já diz).
 */
export function isRedundant(e: LifecycleEntry, all: readonly LifecycleEntry[]): boolean {
  if (e.kind !== 'CHANGE' && e.kind !== 'MODIFIES') return false
  const value = e.value ?? ''
  const mates = all.filter((o) => o !== e && sameMoment(o, e) && (o.entity?.id ?? '') === (e.entity?.id ?? '') && (o.kind === 'CHANGE' || o.kind === 'MODIFIES'))
  if (e.timeline === 'state' && value === 'SELECTED') return mates.some((o) => o.timeline === 'place' && o.value === 'SELECTED')
  if (e.timeline === 'place' && value === 'NONE') return mates.some((o) => o.timeline === 'waitingOn' && !!o.value && o.value !== 'null')
  if (e.timeline === 'waitingOn' && (!value || value === 'null')) return mates.some((o) => o.timeline === 'place' && o.value === 'RUNNING')
  return false
}

/** O rótulo da linha que mostra a entidade para a qual o valor aponta: o que a intenção espera ou o objetivo que empilhou. */
export function valueEntityLabel(e: LifecycleEntry): string | null {
  if (!e.valueEntity) return null
  if (e.timeline === 'waitingOn') return 'waiting on'
  if (e.timeline === 'stack') return 'pushed'
  return 'now'
}

/** O título da entrada: o que aconteceu, com os dados da própria ocorrência. `ownerType` é o tipo da entidade da janela. */
export function describeEntry(e: LifecycleEntry, ownerType: string): string {
  const coded = e.code ? byCode(e.code, e) : null
  if (coded) return coded
  if (e.kind === 'GENERATES' || e.kind === 'MODIFIES' || e.kind === 'REMOVES') return byEffect(e)
  if (e.kind === 'CHANGE') return byTimeline(e)
  if (e.kind === 'START' || e.kind === 'END') return byOwner(e, ownerType)
  return e.kind
}
