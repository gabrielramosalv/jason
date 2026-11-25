import { nextTick, onMounted, ref, watch } from 'vue'
import type { Ref } from 'vue'

const NEAR_BOTTOM_PX = 24

/**
 * Rolagem que segue o fim só enquanto a pessoa está no fim. Se ela subiu para ler algo, o conteúdo novo não a
 * arrasta: o contador `pending` cresce só com itens realmente novos e `toBottom` leva ao fim quando ela pedir
 * (o botão com a seta). `keys` são as chaves dos itens, do mais antigo ao mais novo.
 */
export function useStickyScroll(scroller: Ref<HTMLElement | null>, keys: Ref<readonly (string | number)[]>, active: Ref<boolean>) {
  const atBottom = ref(true)
  const pending = ref(0)

  function onScroll() {
    const el = scroller.value
    if (!el) return
    atBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight <= NEAR_BOTTOM_PX
    if (atBottom.value) pending.value = 0
  }

  function toBottom(smooth = true) {
    const el = scroller.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
    atBottom.value = true
    pending.value = 0
  }

  // só conta o que chegou depois do que já era o mais novo; ciclos antigos carregados em cima ou uma lista trocada não contam
  watch(keys, async (now, before) => {
    if (!active.value) return
    const follow = atBottom.value
    await nextTick()
    if (follow) {
      toBottom(false)
      return
    }
    const previousNewest = before[before.length - 1]
    const at = previousNewest === undefined ? -1 : now.indexOf(previousNewest)
    if (at >= 0) pending.value += now.length - 1 - at
  })

  onMounted(() => void nextTick(() => toBottom(false)))

  return { atBottom, pending, onScroll, toBottom }
}
