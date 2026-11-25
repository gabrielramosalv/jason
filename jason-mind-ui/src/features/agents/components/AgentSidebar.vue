<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { useSessionStore } from '@/stores/session.store'
import { agentDot } from '../domain/agent-status'

/** Barra lateral de agentes: um botão de 28 px por agente, com o estado e o ciclo atual. */
const session = useSessionStore()
const layout = useLayoutStore()
const { agents, selectedAgent } = storeToRefs(session)

function startResize(e: PointerEvent) {
  let last = e.clientX
  const move = (ev: PointerEvent) => {
    layout.agentsWidth = Math.min(420, Math.max(140, layout.agentsWidth + ev.clientX - last))
    last = ev.clientX
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}
</script>

<template>
  <aside class="mi-panel mi-flat mi-agside" data-tour="agents" aria-label="Agents" :style="{ width: `${layout.agentsWidth + 1}px` }">
    <div class="mi-agres" role="separator" aria-orientation="vertical" aria-label="Resize agents sidebar" @pointerdown.prevent="startResize" />
    <div style="flex: 1 1 0; min-height: 0; overflow-y: auto; padding: var(--sp-4); display: flex; flex-direction: column; gap: var(--sp-4)">
      <button
        v-for="a in agents"
        :key="a.name"
        type="button"
        class="mi-row"
        :style="{ ...(a.name === selectedAgent ? { background: 'var(--sel-bg)' } : {}), flexDirection: 'column', alignItems: 'stretch', justifyContent: 'center', height: '28px', flex: '0 0 auto', gap: 'var(--sp-4)', padding: '0 var(--sp-12)' }"
        @click="session.selectAgent(a.name)"
      >
        <span style="display: flex; align-items: center; gap: var(--sp-8)">
          <span class="mi-dot" :style="{ background: agentDot(a) }" />
          <strong style="font-weight: 500; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ a.name }}</strong>
          <span style="flex-shrink: 0; margin-left: auto; color: var(--c-8a8f99); font-size: 11.5px; font-variant-numeric: tabular-nums; display: inline-flex; align-items: center; gap: var(--sp-4)" title="cycles"><Icon name="cycle" :size="11" />{{ a.newestCycle ?? 0 }}</span>
        </span>
      </button>
      <p v-if="!agents.length" class="mi-empty">No agents registered yet</p>
    </div>
  </aside>
</template>
