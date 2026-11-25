import { ref } from 'vue'
import { defineStore } from 'pinia'

export type PopoverKind = 'settings' | null

/** Estado passageiro da interface: o popover aberto no canto, a legenda e o tour guiado. */
export const useUiStore = defineStore('ui', () => {
  const popover = ref<PopoverKind>(null)
  const legend = ref(false)
  /** Passo atual do tour; nulo quando fechado. */
  const tour = ref<number | null>(null)
  const helpMenu = ref(false)
  /** Caixa flutuante de busca nos ciclos aberta ou minimizada. */
  const searchOpen = ref(false)

  function toggle(kind: Exclude<PopoverKind, null>) {
    popover.value = popover.value === kind ? null : kind
  }

  function startTour() {
    helpMenu.value = false
    popover.value = null
    tour.value = 0
  }

  return { popover, legend, tour, helpMenu, searchOpen, toggle, startTour }
})
