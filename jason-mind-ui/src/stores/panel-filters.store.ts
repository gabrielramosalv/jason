import { reactive } from 'vue'
import { defineStore } from 'pinia'

/** Busca e filtros dos painéis Console e Messages, compartilhados entre o cabeçalho e o corpo de cada painel. */
export const usePanelFiltersStore = defineStore('panel-filters', () => {
  const console = reactive({ searchOpen: false, q: '', filterOpen: false, agents: [] as string[] })
  const messages = reactive({
    searchOpen: false,
    q: '',
    /** `time` é a conversa em ordem cronológica (chat); `abs` lista recebidas e enviadas. */
    mode: 'time' as 'time' | 'abs',
    filterOpen: false,
    agents: [] as string[],
    contact: null as string | null,
    recvOpen: true,
    sentOpen: true,
  })

  function toggleIn(list: string[], name: string) {
    const i = list.indexOf(name)
    if (i >= 0) list.splice(i, 1)
    else list.push(name)
  }

  return { console, messages, toggleIn }
})
