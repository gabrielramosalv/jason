import { computed, reactive, ref } from 'vue'

export const DEFAULT_ANCHOR = { x: 0.65, y: 0.5 }
export const MIN_ZOOM = 0.5
export const MAX_ZOOM = 2

/**
 * Foco da Timeline: onde o ciclo escolhido fica na tela (a âncora), as travas de cada eixo e o zoom.
 * Com o eixo X travado a faixa não anda para os lados, só o ciclo escolhido muda; com o Y travado ela não sobe nem desce.
 */
export function useTimelineFocus() {
  const anchor = reactive({ ...DEFAULT_ANCHOR })
  const lockX = ref(false)
  const lockY = ref(true)
  const zoom = ref(1)
  /** Deslocamento vertical da faixa quando o eixo Y está livre. */
  const offsetY = ref(0)

  const focusMoved = computed(() => anchor.x !== DEFAULT_ANCHOR.x || anchor.y !== DEFAULT_ANCHOR.y || offsetY.value !== 0)

  const clamp = (n: number, min: number, max: number) => Math.min(max, Math.max(min, n))

  function moveAnchor(dx: number, dy: number, width: number, height: number) {
    anchor.x = clamp(anchor.x + dx / Math.max(1, width), 0.05, 0.95)
    anchor.y = clamp(anchor.y + dy / Math.max(1, height), 0.15, 0.85)
  }

  function resetFocus() {
    anchor.x = DEFAULT_ANCHOR.x
    anchor.y = DEFAULT_ANCHOR.y
    offsetY.value = 0
  }

  function setZoom(value: number) {
    zoom.value = clamp(value, MIN_ZOOM, MAX_ZOOM)
  }

  function panY(dy: number) {
    if (!lockY.value) offsetY.value = clamp(offsetY.value + dy, -160, 160)
  }

  return { anchor, lockX, lockY, zoom, offsetY, focusMoved, moveAnchor, resetFocus, setZoom, panY }
}
