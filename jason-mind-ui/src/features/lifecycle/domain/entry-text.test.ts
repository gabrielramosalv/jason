import { describe, expect, it } from 'vitest'
import type { LifecycleEntry, LifecycleKind } from '../types/lifecycle.types'
import { describeEntry } from './entry-text'

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
    expect(describeEntry(entry('END'), 'intendedMeans')).toBe('Plan finished')
  })
})
