<script setup lang="ts">
import { storeToRefs } from 'pinia'
import AgentSidebar from '@/features/agents/components/AgentSidebar.vue'
import AgentSwitcher from '@/features/agents/components/AgentSwitcher.vue'
import CycleControls from '@/features/cycles/components/CycleControls.vue'
import LegendModal from '@/features/help/components/LegendModal.vue'
import PanelRail from '@/features/layout/components/PanelRail.vue'
import LifecycleWindows from '@/features/lifecycle/components/LifecycleWindows.vue'
import SettingsPopover from '@/features/settings/components/SettingsPopover.vue'
import GuidedTour from '@/features/help/components/GuidedTour.vue'
import AppHeader from '@/shared/components/AppHeader.vue'
import { usePolling } from '@/shared/composables/usePolling'
import { IconButton } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { useSessionStore } from '@/stores/session.store'
import { useUiStore } from '@/stores/ui.store'

const session = useSessionStore()
const layout = useLayoutStore()
const ui = useUiStore()
const { isLive } = storeToRefs(session)

// um único polling da lista leve de agentes, com ETag, pausado com a aba oculta (REQ-012)
usePolling(() => session.refreshMas(), 1000)
</script>

<template>
  <div class="mi-root" :data-theme="layout.theme" :class="ui.tour !== null && 'mi-touring'">
    <AppHeader />
    <div class="mi-body" style="flex: 1; min-height: 0; display: flex; gap: 0; position: relative">
      <aside class="mi-rail mi-rail-l" aria-label="Navigation">
        <IconButton icon="agents" label="Agents" :selected="layout.agentsOpen" @click="layout.agentsOpen = !layout.agentsOpen" />
      </aside>
      <AgentSidebar v-if="layout.agentsOpen" />
      <main class="mi-agent" style="flex: 1; min-width: 0; display: flex; flex-direction: column; min-height: 0; overflow: hidden; background: var(--bg-main)">
        <div class="mi-bar">
          <AgentSwitcher />
          <CycleControls />
          <IconButton
            icon="timeline-off"
            :label="layout.view === 'timeline' ? 'Back to the cycle view' : 'Timeline view'"
            :pressed="layout.view === 'timeline'"
            style="margin-left: auto"
            data-tour="view-toggle"
            @click="layout.view = layout.view === 'timeline' ? 'cycle' : 'timeline'"
          />
        </div>
        <div class="mi-below" style="flex: 1 1 0; min-height: 0; min-width: 0; display: flex">
          <RouterView />
          <PanelRail />
        </div>
      </main>
      <SettingsPopover v-if="ui.popover === 'settings'" />
    </div>
    <LegendModal v-if="ui.legend" />
    <LifecycleWindows />
    <GuidedTour v-if="ui.tour !== null" />
  </div>
</template>
