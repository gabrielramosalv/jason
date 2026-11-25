import { ref } from 'vue'
import type { Ref } from 'vue'
import { useLayoutStore } from '@/stores/layout.store'
import { dropSide } from '../domain/dock-tree'
import type { DropSide, PanelKey } from '../domain/dock-tree'

export interface DropHint {
  left: number
  top: number
  width: number
  height: number
  label: string
}

const LABELS: Record<DropSide, string> = {
  tab: 'Move as a tab',
  left: 'Move to the left',
  right: 'Move to the right',
  top: 'Move above',
  bottom: 'Move below',
}
const THRESHOLD = 5

/**
 * Gesto de arrastar painéis no dock: pega uma aba (só ela) ou o contêiner inteiro, mostra onde cairá
 * (aba, esquerda, direita, em cima ou embaixo do painel sob o ponteiro) e move ao soltar.
 */
export function useDockDrag(host: Ref<HTMLElement | null>) {
  const layout = useLayoutStore()
  const hint = ref<DropHint | null>(null)
  const dragging = ref(false)

  function targetAt(x: number, y: number): { sib: PanelKey; side: DropSide; rect: DOMRect } | null {
    const el = [...(host.value?.querySelectorAll<HTMLElement>('section[data-k]') ?? [])].find((s) => {
      const r = s.getBoundingClientRect()
      return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
    })
    if (!el) return null
    const rect = el.getBoundingClientRect()
    return { sib: el.dataset.k as PanelKey, side: dropSide(x - rect.left, y - rect.top, rect.width, rect.height), rect }
  }

  function start(keys: PanelKey[], e: PointerEvent) {
    if (e.button !== 0) return
    const origin = { x: e.clientX, y: e.clientY }
    let active = false

    const move = (ev: PointerEvent) => {
      if (!active && Math.hypot(ev.clientX - origin.x, ev.clientY - origin.y) < THRESHOLD) return
      active = true
      dragging.value = true
      const base = host.value?.getBoundingClientRect()
      const t = targetAt(ev.clientX, ev.clientY)
      if (!t || !base || keys.includes(t.sib)) {
        hint.value = null
        return
      }
      const r = t.rect
      const half = (v: number) => v / 2
      const box = {
        tab: { left: r.left, top: r.top, width: r.width, height: r.height },
        left: { left: r.left, top: r.top, width: half(r.width), height: r.height },
        right: { left: r.left + half(r.width), top: r.top, width: half(r.width), height: r.height },
        top: { left: r.left, top: r.top, width: r.width, height: half(r.height) },
        bottom: { left: r.left, top: r.top + half(r.height), width: r.width, height: half(r.height) },
      }[t.side]
      hint.value = { left: box.left - base.left, top: box.top - base.top, width: box.width, height: box.height, label: LABELS[t.side] }
    }

    const up = (ev: PointerEvent) => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', up)
      const t = active ? targetAt(ev.clientX, ev.clientY) : null
      hint.value = null
      dragging.value = false
      if (t && !keys.includes(t.sib)) layout.move(keys, t.sib, t.side)
    }

    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
  }

  return { hint, dragging, start }
}
