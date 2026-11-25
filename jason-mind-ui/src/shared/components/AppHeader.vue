<script setup lang="ts">
import { Icon } from '@/shared/ui'
import { useSessionStore } from '@/stores/session.store'
import { useUiStore } from '@/stores/ui.store'

const session = useSessionStore()
const ui = useUiStore()
</script>

<template>
  <header class="mi-header">
    <div style="display: flex; align-items: center; gap: var(--sp-12)">
      <strong style="font-size: 12.8px; font-weight: 400; color: var(--c-8a8f99)">Mind Inspector</strong>
    </div>
    <h1 style="margin: 0; font-size: 12.8px; font-weight: 500">{{ session.mas?.mas ?? '' }}</h1>
    <div style="margin-left: auto; display: flex; align-items: center; gap: var(--sp-8)">
      <button type="button" class="hd-btn" title="Help" aria-label="Help" aria-haspopup="menu" data-tour="help" @click="ui.helpMenu = !ui.helpMenu"><Icon name="help" /></button>
      <button type="button" class="hd-btn" title="Settings" aria-label="Settings" data-tour="settings" @click="ui.toggle('settings')"><Icon name="settings" /></button>
    </div>
    <template v-if="ui.helpMenu">
      <div style="position: fixed; inset: 0; z-index: 40" @click="ui.helpMenu = false" />
      <div role="menu" aria-label="Help" class="mi-menu">
        <button type="button" role="menuitem" class="hd-btn mi-menuitem" @click="ui.startTour()"><Icon name="chosen" /> Tutorial</button>
        <button type="button" role="menuitem" class="hd-btn mi-menuitem" @click="ui.legend = true; ui.helpMenu = false"><Icon name="legend-belief" /> Legend</button>
      </div>
    </template>
  </header>
</template>
