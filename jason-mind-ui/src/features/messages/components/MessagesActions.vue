<script setup lang="ts">
import AgentFilter from '@/shared/components/AgentFilter.vue'
import { IconButton } from '@/shared/ui'
import { usePanelFiltersStore } from '@/stores/panel-filters.store'

/** Cabeçalho do painel Messages: busca, filtro de agentes e a troca entre o chat e as listas Received e Sent. */
const filters = usePanelFiltersStore()
</script>

<template>
  <input v-if="filters.messages.searchOpen" v-model="filters.messages.q" type="text" class="mi-searchin" placeholder="Search…" aria-label="Search" />
  <IconButton icon="search" label="Search" :selected="filters.messages.searchOpen" @click="filters.messages.searchOpen = !filters.messages.searchOpen" />
  <AgentFilter label="Filter agents" :selected="filters.messages.agents" :open="filters.messages.filterOpen" @toggle="filters.messages.filterOpen = !filters.messages.filterOpen" @close="filters.messages.filterOpen = false" @pick="filters.toggleIn(filters.messages.agents, $event)" />
  <IconButton v-if="filters.messages.mode === 'abs'" icon="chat" label="Timeline (chat)" @click="filters.messages.mode = 'time'" />
  <IconButton v-else icon="all-messages" label="All messages" @click="filters.messages.mode = 'abs'" />
</template>
