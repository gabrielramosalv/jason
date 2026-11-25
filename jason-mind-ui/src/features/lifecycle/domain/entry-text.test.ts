import { describe, expect, it } from 'vitest'
import type { LifecycleEntry, LifecycleKind } from '../types/lifecycle.types'
import { describeEntry, isRedundant, valueEntityLabel } from './entry-text'

const entry = (kind: LifecycleKind, extra: Partial<LifecycleEntry> = {}): LifecycleEntry => ({
  sequence: 1,
  kind,
  code: null,
  timeline: null,
  value: null,
  moment: { cycle: 1, iteration: 0, phase: 'ACT', step: 'EXECUTE_INTENTION', ordinal: 0 },
  cause: null,
  entity: null,
  into: null,
  ...extra,
})

describe('describeEntry', () => {
  it('usa o código do servidor e os dados da ocorrência', () => {
    expect(describeEntry(entry('START', { code: 'BELIEF_ADDED_BY_MESSAGE', cause: { sender: 'bob' } }), 'belief')).toBe('Learned from a message from bob')
    expect(describeEntry(entry('START', { code: 'EVENT_TRIGGERED_BY_MESSAGE', cause: { sender: 'bob' } }), 'event')).toBe('Triggered by a message from bob')
    expect(describeEntry(entry('CHANGE', { code: 'INTENTION_SELECTED', timeline: 'place', value: 'SELECTED_INTENTION' }), 'intention')).toBe('Selected to run in this cycle')
  })

  it('escreve as mudanças sem código pela linha do tempo e o valor', () => {
    expect(describeEntry(entry('CHANGE', { timeline: 'place', value: 'NONE' }), 'intention')).toBe('Left the queue of running intentions')
    expect(describeEntry(entry('CHANGE', { timeline: 'place', value: 'RUNNING' }), 'intention')).toBe('Back in the queue of running intentions')
    expect(describeEntry(entry('CHANGE', { timeline: 'waitingOn', value: 'e201' }), 'intention')).toBe('Waiting for the event #201 to be handled')
    expect(describeEntry(entry('CHANGE', { timeline: 'waitingOn', value: 'null' }), 'intention')).toBe('Stopped waiting')
    expect(describeEntry(entry('CHANGE', { timeline: 'suspended', value: 'false' }), 'intention')).toBe('Resumed')
    expect(describeEntry(entry('CHANGE', { timeline: 'currentDeed', value: '4' }), 'intendedMeans')).toBe('Executing deed 5 of the plan')
    expect(describeEntry(entry('CHANGE', { timeline: 'state', value: 'NO_APPLICABLE' }), 'event')).toBe('No applicable plan was found')
    expect(describeEntry(entry('CHANGE', { timeline: 'annotations', value: '[source(self)]' }), 'belief')).toBe('Annotations are now [source(self)]')
    expect(describeEntry(entry('CHANGE', { timeline: 'unknown', value: 'SOME_VALUE' }), 'belief')).toBe('unknown is now SOME_VALUE')
  })

  it('escreve os efeitos pelo tipo da entidade afetada, no lugar do "Generated" seco', () => {
    const affected = (type: string) => ({ type, id: 'x1', label: 'x' })
    expect(describeEntry(entry('GENERATES', { entity: affected('message') }), 'intention')).toBe('Sent a message')
    expect(describeEntry(entry('GENERATES', { entity: affected('event') }), 'intention')).toBe('Triggered an event')
    expect(describeEntry(entry('GENERATES', { entity: affected('belief') }), 'intention')).toBe('Added a belief')
    expect(describeEntry(entry('GENERATES', { code: 'EVENT_GENERATED_INTENTION', entity: affected('intention') }), 'event')).toBe('Started a new intention')
    expect(describeEntry(entry('MODIFIES', { entity: affected('belief') }), 'intention')).toBe('Updated a belief')
    expect(describeEntry(entry('REMOVES', { entity: affected('belief') }), 'intention')).toBe('Removed a belief')
    expect(describeEntry(entry('GENERATES', { entity: affected('plan') }), 'intention')).toBe('Started a plan')
  })

  it('escreve começo e fim sem código pelo tipo da entidade dona', () => {
    expect(describeEntry(entry('START'), 'message')).toBe('Sent')
    expect(describeEntry(entry('START'), 'intention')).toBe('Created')
    expect(describeEntry(entry('END'), 'belief')).toBe('Removed from the belief base')
    expect(describeEntry(entry('END'), 'intendedMeans')).toBe('Finished: its plan ended')
    expect(describeEntry(entry('START'), 'intendedMeans')).toBe('Started: a plan was chosen for this goal')
  })

  it('diz de onde vêm as entidades da carga do programa', () => {
    const loaded = { moment: { cycle: 0, iteration: 0, phase: 'EXTERNAL', step: 'INITIAL_LOAD', ordinal: 1 } }
    expect(describeEntry(entry('START', loaded), 'belief')).toBe('Loaded with the program')
    expect(describeEntry(entry('START', loaded), 'plan')).toBe('Loaded with the program')
    expect(describeEntry(entry('START', loaded), 'event')).toBe('Triggered by the program load')
  })

  it('escreve o efeito sobre a intenção pelo campo que mudou e pelo valor, não por "Changed an intention"', () => {
    const intention = { type: 'intention', id: 'i1', label: 'x' }
    const on = (timeline: string, value: string) => describeEntry(entry('MODIFIES', { timeline, value, entity: intention }), 'intendedMeans')
    expect(on('waitingOn', 'e140')).toBe('Made the intention wait for the event #140')
    expect(on('waitingOn', 'a7')).toBe('Made the intention wait for an action')
    expect(on('place', 'NONE')).toBe('Took the intention out of the queue')
    expect(on('place', 'RUNNING')).toBe('Put the intention back in the queue')
    expect(on('suspended', 'true')).toBe('Suspended the intention')
    expect(describeEntry(entry('REMOVES', { entity: { type: 'intendedMeans', id: 'im1', label: 'x' } }), 'event')).toBe('Ended a goal')
  })
})

describe('isRedundant', () => {
  const at = (ordinal: number) => ({ moment: { cycle: 6, iteration: 0, phase: 'ACT', step: 'EXECUTE_INTENTION', ordinal } })
  it('esconde o `place` NONE quando a mesma intenção passa a esperar no mesmo momento', () => {
    const none = entry('CHANGE', { sequence: 1, timeline: 'place', value: 'NONE', ...at(1) })
    const waits = entry('CHANGE', { sequence: 2, timeline: 'waitingOn', value: 'e140', ...at(1) })
    expect(isRedundant(none, [none, waits])).toBe(true)
    expect(isRedundant(waits, [none, waits])).toBe(false)
    expect(isRedundant(none, [none])).toBe(false)
  })

  it('esconde o fim da espera quando a volta à fila vem no mesmo momento, e o `state` selecionado do evento', () => {
    const stop = entry('CHANGE', { sequence: 1, timeline: 'waitingOn', value: 'null', ...at(1) })
    const back = entry('CHANGE', { sequence: 2, timeline: 'place', value: 'RUNNING', ...at(1) })
    expect(isRedundant(stop, [stop, back])).toBe(true)
    const state = entry('CHANGE', { sequence: 3, timeline: 'state', value: 'SELECTED', ...at(2) })
    const place = entry('CHANGE', { sequence: 4, timeline: 'place', value: 'SELECTED', ...at(2) })
    expect(isRedundant(state, [state, place])).toBe(true)
  })

  it('não junta entradas de entidades diferentes nem de momentos diferentes', () => {
    const none = entry('MODIFIES', { sequence: 1, timeline: 'place', value: 'NONE', entity: { type: 'intention', id: 'i1', label: 'x' }, ...at(1) })
    const other = entry('MODIFIES', { sequence: 2, timeline: 'waitingOn', value: 'e1', entity: { type: 'intention', id: 'i2', label: 'y' }, ...at(1) })
    const later = entry('CHANGE', { sequence: 3, timeline: 'waitingOn', value: 'e1', ...at(2) })
    expect(isRedundant(none, [none, other, later])).toBe(false)
  })
})

describe('valueEntityLabel', () => {
  it('nomeia a linha da entidade para a qual o valor aponta', () => {
    const target = { type: 'event', id: 'e140', label: '+!checkout(x)' }
    expect(valueEntityLabel(entry('CHANGE', { timeline: 'waitingOn', value: 'e140', valueEntity: target }))).toBe('waiting on')
    expect(valueEntityLabel(entry('CHANGE', { timeline: 'stack', value: 'im3', valueEntity: { type: 'intendedMeans', id: 'im3', label: '+!g' } }))).toBe('pushed')
    expect(valueEntityLabel(entry('CHANGE', { timeline: 'place', value: 'RUNNING' }))).toBeNull()
  })
})
