import type { IconName } from '@/shared/ui'

/** Conceitos da legenda e as cores de fundo e de texto de cada um (variáveis do tema). */
export type ConceptKind = 'message' | 'event' | 'plan' | 'intention' | 'subgoal' | 'action' | 'belief'

export const CONCEPT_COLORS: Record<ConceptKind, { bg: string; fg: string }> = {
  belief: { bg: 'var(--c-e2f6e2)', fg: 'var(--c-0a6a0a)' },
  intention: { bg: 'var(--c-e4d3f7)', fg: 'var(--c-6d3aa6)' },
  subgoal: { bg: 'var(--c-fbe3ef)', fg: 'var(--c-b0306f)' },
  event: { bg: 'var(--c-ffe8d3)', fg: 'var(--c-b4540a)' },
  message: { bg: 'var(--c-e6f0fb)', fg: 'var(--c-1d4f91)' },
  plan: { bg: 'var(--c-fdf1d6)', fg: 'var(--c-8a5a00)' },
  action: { bg: 'var(--c-fde8e6)', fg: 'var(--c-b42318)' },
}

export const CONCEPT_ICON: Record<ConceptKind, IconName> = {
  message: 'chip-message',
  event: 'chip-event',
  plan: 'chip-plan',
  subgoal: 'chip-subgoal',
  intention: 'chip-intention',
  action: 'chip-action',
  belief: 'chip-belief',
}

/** Converte o tipo de entidade da API no conceito da legenda. */
export function conceptOf(type: string): ConceptKind {
  switch (type) {
    case 'intendedMeans':
    case 'subgoal':
      return 'subgoal'
    case 'message':
    case 'event':
    case 'plan':
    case 'intention':
    case 'action':
    case 'belief':
      return type
    default:
      return 'plan'
  }
}

export function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`
  const units = ['KiB', 'MiB', 'GiB']
  let v = value / 1024
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v.toFixed(v < 10 ? 2 : 1)} ${units[i]}`
}

export function formatTime(ms: number): string {
  const d = new Date(ms)
  const p = (n: number, k = 2) => String(n).padStart(k, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${p(d.getMilliseconds(), 3)}`
}

/** `source(x)` de uma lista de anotações, sem o envoltório. */
export function annotationSource(annotations: string[]): string | null {
  const s = annotations.find((a) => a.startsWith('source('))
  return s ? s.slice(7, -1) : null
}
