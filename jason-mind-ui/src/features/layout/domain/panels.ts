import type { IconName } from '@/shared/ui/icons'
import type { PanelKey } from './dock-tree'

export interface PanelMeta {
  title: string
  icon: IconName
  /** Nome do botão na barra lateral direita. */
  rail: string
}

/** Título, ícone e rótulo da barra lateral de cada painel. */
export const PANELS: Record<PanelKey, PanelMeta> = {
  code: { title: 'Source code', icon: 'panel-code', rail: 'Source code' },
  bel: { title: 'Beliefs', icon: 'belief', rail: 'Beliefs' },
  int: { title: 'Intentions', icon: 'panel-intentions', rail: 'Intentions' },
  ev: { title: 'Events', icon: 'panel-events', rail: 'Events' },
  reason: { title: 'Reasoning', icon: 'reasoning', rail: 'Reasoning' },
  msgs: { title: 'Messages', icon: 'panel-messages', rail: 'Messages' },
  console: { title: 'Console', icon: 'console', rail: 'Console' },
}

/** Ordem dos botões na barra lateral direita. */
export const RAIL_ORDER: PanelKey[] = ['code', 'bel', 'int', 'ev', 'reason', 'msgs', 'console']
