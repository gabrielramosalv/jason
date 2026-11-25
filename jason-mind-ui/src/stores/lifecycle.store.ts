import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface LifecycleTarget {
  agent: string
  id: string
  type: string
  label: string
}

/** O momento do ciclo que a janela deve acender ao abrir (as mudanças de um passo do Reasoning). */
export interface LifecycleHighlight {
  cycle: number
  step: string
  iteration: number
  ordinal: number
}

export interface LifecycleWindow extends LifecycleTarget {
  key: string
  highlight: LifecycleHighlight | null
  /** Sobe a cada pedido de destaque, para a janela já aberta piscar de novo. */
  flash: number
  x: number
  y: number
  w: number
  h: number
  z: number
}

/** Janelas de Life cycle abertas (D-080): cada chip clicado abre uma, que se arrasta e se redimensiona. */
export const useLifecycleStore = defineStore('lifecycle', () => {
  const windows = ref<LifecycleWindow[]>([])
  let top = 40

  /** Abre a janela perto de onde o chip foi clicado (`at`), ou raise se a entidade já tem janela. */
  function open(target: LifecycleTarget, at?: { x: number; y: number }, highlight: LifecycleHighlight | null = null) {
    const key = `${target.agent}/${target.id}`
    const existing = windows.value.find((w) => w.key === key)
    if (existing) {
      existing.z = ++top
      if (highlight) {
        existing.highlight = highlight
        existing.flash++
      }
      return
    }
    const n = windows.value.length
    const w = 420
    const x = at ? Math.max(8, Math.min(at.x - 24, window.innerWidth - w - 12)) : 180 + n * 28
    const y = at ? Math.max(8, Math.min(at.y + 14, window.innerHeight - 380)) : 70 + n * 28
    windows.value.push({ ...target, key, highlight, flash: 0, x, y, w, h: 360, z: ++top })
  }
  function close(key: string) {
    windows.value = windows.value.filter((w) => w.key !== key)
  }
  function closeAll() {
    windows.value = []
  }
  function raise(key: string) {
    const w = windows.value.find((x) => x.key === key)
    if (w) w.z = ++top
  }

  return { windows, open, close, closeAll, raise }
})
