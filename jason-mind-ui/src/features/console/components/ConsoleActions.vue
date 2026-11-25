<script setup lang="ts">
import AgentFilter from '@/shared/components/AgentFilter.vue'
import { IconButton } from '@/shared/ui'
import { usePanelFiltersStore } from '@/stores/panel-filters.store'

/** Busca e filtro de agentes do cabeçalho do Console. */
const filters = usePanelFiltersStore()
</script>

<template>
  <input v-if="filters.console.searchOpen" v-model="filters.console.q" type="text" class="mi-searchin" placeholder="Search…" aria-label="Search console" />
  <IconButton icon="search" label="Search console" :selected="filters.console.searchOpen" @click="filters.console.searchOpen = !filters.console.searchOpen" />
  <AgentFilter label="Filter agents" :selected="filters.console.agents" :open="filters.console.filterOpen" @toggle="filters.console.filterOpen = !filters.console.filterOpen" @close="filters.console.filterOpen = false" @pick="filters.toggleIn(filters.console.agents, $event)" />
</template>
