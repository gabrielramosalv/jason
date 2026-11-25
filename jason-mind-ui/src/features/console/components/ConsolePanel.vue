<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { formatTime } from '@/shared/domain/entities'
import { useStickyScroll } from '@/shared/composables/useStickyScroll'
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { usePanelFiltersStore } from '@/stores/panel-filters.store'
import { useSessionStore } from '@/stores/session.store'
import { useConsole } from '../composables/useConsole'

/** Corpo do painel Console: hora, ciclo (que leva ao Reasoning), agente (se vários) e o texto de cada .print. */
const session = useSessionStore()
const layout = useLayoutStore()
const filters = usePanelFiltersStore()
const { selectedAgent, isLive, shown } = storeToRefs(session)

const scroller = ref<HTMLElement | null>(null)
const agents = computed(() => (filters.console.agents.length ? filters.console.agents : selectedAgent.value ? [selectedAgent.value] : []))
const showAgent = computed(() => agents.value.length > 1)
const until = computed(() => shown.value?.sequenceEnd ?? null)
const enabled = computed(() => layout.visible.console)

const { lines, error, loading, behind, hasOlder, gaps, fillGap, loadOlder } = useConsole({ agents, until, live: isLive, enabled })

const visible = computed(() => {
  const q = filters.console.q.trim().toLowerCase()
  return q ? lines.value.filter((l) => l.text.toLowerCase().includes(q)) : lines.value
})

const { atBottom, pending, onScroll, toBottom } = useStickyScroll(scroller, computed(() => visible.value.map((l) => l.sequence)), isLive)

/** O chip de ciclo leva ao ciclo da linha no agente dela, com o Reasoning à vista. */
function goTo(agent: string, cycle: number) {
  if (agent !== session.selectedAgent) session.selectAgent(agent)
  session.goToCycle(cycle)
  layout.show('reason')
  layout.activate('reason')
}
</script>

<template>
  <div style="position: relative; flex: 1 1 0; min-height: 0; display: flex; flex-direction: column">
  <div ref="scroller" class="mi-code" style="flex: 1 1 0; min-height: 0; overflow-y: auto; padding: var(--sp-4); font-size: 12.8px" @scroll.passive="onScroll">
    <p v-if="error" class="mi-warnbox">{{ error }}</p>
    <p v-if="behind" class="mi-warnbox">The console is behind the flow; catching up.</p>
    <div v-if="lines.length && hasOlder" style="text-align: center; padding: var(--sp-4)"><button type="button" class="mi-btn" @click="loadOlder">Earlier lines</button></div>
    <div v-if="!lines.length && !loading" class="mi-empty">No console output in this slice</div>
    <template v-for="line in visible" :key="line.sequence">
      <div v-for="g in gaps.filter((x) => x.sequence === line.sequence)" :key="`gap-${g.sequence}`" class="mi-gap">
        <span>Lines omitted: the flow outran the screen</span>
        <button type="button" class="mi-btn" @click="fillGap(g)">Load</button>
      </div>
      <div class="mi-logrow">
        <span class="mi-logtime">{{ formatTime(line.timeMilliseconds) }}</span>
        <button type="button" class="mi-cyclink" :title="`Go to cycle ${line.cycle} in Reasoning`" style="color: var(--c-8a8f99); white-space: nowrap; width: 56px; flex-shrink: 0; height: 18.5px; box-sizing: content-box" @click="goTo(line.agent, line.cycle)"><Icon name="cycle" />{{ line.cycle }}</button>
        <span v-if="showAgent" style="width: 72px; flex-shrink: 0; color: var(--c-5b5f6b)">{{ line.agent }}</span>
        <span style="min-width: 0; overflow-wrap: anywhere">{{ line.text }}</span>
      </div>
    </template>
  </div>
  <button v-if="!atBottom && pending > 0" type="button" class="mi-tobottom" :title="pending ? `${pending} new lines: go to the latest` : 'Go to the latest'" aria-label="Go to the latest" @click="toBottom()"><span>{{ pending }} new</span><Icon name="chevron-down" /></button>
  </div>
</template>
