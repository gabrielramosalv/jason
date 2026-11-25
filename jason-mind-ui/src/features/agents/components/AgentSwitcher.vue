<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { useSessionStore } from '@/stores/session.store'
import { agentDot } from '../domain/agent-status'

/** Nome do agente na barra de cima; com mais de um agente vira um botão que abre a lista. */
const session = useSessionStore()
const layout = useLayoutStore()
const { agents, selected, selectedAgent } = storeToRefs(session)
const open = ref(false)

function pick(name: string) {
  session.selectAgent(name)
  open.value = false
}
</script>

<template>
  <div v-if="selected" style="display: flex; align-items: center; gap: var(--sp-4)">
    <h2 v-if="agents.length < 2 || layout.agentsOpen" class="mi-agtitle"><span class="mi-dot" :style="{ background: agentDot(selected) }" />{{ selected.name }}</h2>
    <h2 v-else style="margin: 0; font-size: 12.8px; font-weight: 500">
      <button type="button" class="mi-btn" title="Switch agent" aria-label="Switch agent" aria-haspopup="listbox" :aria-expanded="open" style="height: 28px; padding: 0 var(--sp-8) 0 var(--sp-12); gap: var(--sp-8); border-color: transparent; font: inherit; color: var(--c-1f2329)" @click="open = !open">
        <span class="mi-dot" :style="{ background: agentDot(selected) }" />{{ selected.name }}<Icon name="chevron-down" />
      </button>
    </h2>
    <template v-if="open">
      <div class="mi-scrim" @click="open = false" />
      <div role="listbox" aria-label="Agents" class="mi-pop" style="left: 4px; top: 34px; min-width: 190px">
        <button v-for="a in agents" :key="a.name" type="button" class="mi-row" :style="{ ...(a.name === selectedAgent ? { background: 'var(--sel-bg)' } : {}), height: '28px', padding: '0 var(--sp-12)', gap: 'var(--sp-8)' }" @click="pick(a.name)">
          <span class="mi-dot" :style="{ background: agentDot(a) }" />
          <strong style="font-weight: 500">{{ a.name }}</strong>
          <span style="margin-left: auto; padding-left: var(--sp-16); color: var(--c-8a8f99); font-size: 11.5px; font-variant-numeric: tabular-nums; display: inline-flex; align-items: center; gap: var(--sp-4)" title="cycles"><Icon name="cycle" :size="11" />{{ a.newestCycle ?? 0 }}</span>
        </button>
      </div>
    </template>
  </div>
</template>
