<script setup lang="ts">
import { computed, ref } from 'vue'
import ConsoleActions from '@/features/console/components/ConsoleActions.vue'
import ConsolePanel from '@/features/console/components/ConsolePanel.vue'
import DockHost from '@/features/layout/components/DockHost.vue'
import FloatingHost from '@/features/layout/components/FloatingHost.vue'
import MessagesActions from '@/features/messages/components/MessagesActions.vue'
import MessagesPanel from '@/features/messages/components/MessagesPanel.vue'
import ReasoningPanel from '@/features/reasoning/components/ReasoningPanel.vue'
import SearchBox from '@/features/search/components/SearchBox.vue'
import TimelineView from '@/features/timeline/components/TimelineView.vue'
import { IconButton } from '@/shared/ui'
import { useInspectorStore } from '@/stores/inspector.store'
import { useLayoutStore } from '@/stores/layout.store'
import { useSearchStore } from '@/stores/search.store'
import { useSessionStore } from '@/stores/session.store'
import { useUiStore } from '@/stores/ui.store'
import BeliefsActions from '../components/BeliefsActions.vue'
import BeliefsPanel from '../components/BeliefsPanel.vue'
import EventsPanel from '../components/EventsPanel.vue'
import IntentionsPanel from '../components/IntentionsPanel.vue'
import SourcePanel from '../components/SourcePanel.vue'
import { useAgentState } from '../composables/useAgentState'

/** A tela do agente: ferramentas de busca à esquerda, a visão de ciclo (painéis ancorados) ou a Timeline (janelas). */
useAgentState()
const layout = useLayoutStore()
const session = useSessionStore()
const inspector = useInspectorStore()
const search = useSearchStore()
const ui = useUiStore()

const relevantOnly = ref(false)
const intCount = computed(() => inspector.state?.intentions?.length ?? null)
const evCount = computed(() => inspector.state?.events?.length ?? null)
const timeline = computed(() => layout.view === 'timeline')
</script>

<template>
  <aside class="mi-rail mi-rail-l" aria-label="Timeline tools" data-tour="tools">
    <IconButton icon="search-cycles" strong label="Search cycles" :pressed="ui.searchOpen" :selected="ui.searchOpen" @click="ui.searchOpen = !ui.searchOpen" />
    <IconButton icon="relevant-cycles" strong :label="relevantOnly ? 'Show all cycles' : 'Show only relevant cycles'" :pressed="relevantOnly" :selected="relevantOnly" @click="relevantOnly = !relevantOnly" />
  </aside>
  <div class="mi-stage" style="position: relative; flex: 1 1 0; min-height: 0; min-width: 0; display: flex; flex-direction: column">
    <SearchBox v-if="ui.searchOpen" @close="ui.searchOpen = false" />
    <p v-if="!session.selectedAgent" class="mi-emptybig" style="margin: auto">Waiting for the first agent of the MAS…</p>
    <template v-else>
      <TimelineView v-if="timeline" :relevant-only="relevantOnly" />
      <component :is="timeline ? FloatingHost : DockHost" :class="timeline && 'mi-fl'">
        <template #count-int><span class="mi-count">{{ intCount }}</span></template>
        <template #count-ev><span class="mi-count">{{ evCount }}</span></template>
        <template #actions-bel><BeliefsActions /></template>
        <template #actions-console><ConsoleActions /></template>
        <template #actions-msgs><MessagesActions /></template>
        <template #body-code><SourcePanel /></template>
        <template #body-int><IntentionsPanel /></template>
        <template #body-ev><EventsPanel /></template>
        <template #body-bel><BeliefsPanel /></template>
        <template #body-console><ConsolePanel /></template>
        <template #body-msgs><MessagesPanel /></template>
        <template #body-reason><ReasoningPanel /></template>
      </component>
    </template>
  </div>
</template>
