<script setup lang="ts">
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { PANELS, RAIL_ORDER } from '../domain/panels'

/** Barra lateral direita: um botão por painel para mostrar ou esconder, e o reset do layout. */
const layout = useLayoutStore()
</script>

<template>
  <aside class="mi-rail mi-rail-r" aria-label="Sidebar" data-tour="rail">
    <button
      v-for="key in RAIL_ORDER"
      :key="key"
      type="button"
      class="mi-btn mi-icon"
      :title="PANELS[key].rail"
      :aria-label="PANELS[key].rail"
      :aria-selected="layout.visible[key]"
      :style="{ position: 'relative', color: layout.visible[key] ? 'var(--c-1f2329)' : 'var(--c-5b5f6b)', background: layout.visible[key] ? 'var(--sel-hover)' : 'transparent', borderColor: 'transparent' }"
      @click="layout.toggle(key)"
    >
      <Icon :name="key === 'code' ? 'rail-code' : key === 'reason' ? 'rail-reasoning' : PANELS[key].icon" />
    </button>
    <button type="button" class="mi-btn mi-icon" title="Reset layout" aria-label="Reset layout" style="margin-top: auto; color: var(--c-5b5f6b); border-color: transparent" @click="layout.reset()"><Icon name="reset-layout" /></button>
  </aside>
</template>
