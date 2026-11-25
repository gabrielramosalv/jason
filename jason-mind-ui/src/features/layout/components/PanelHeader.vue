<script setup lang="ts">
import { Icon } from '@/shared/ui'
import type { PanelKey } from '../domain/dock-tree'
import { PANELS } from '../domain/panels'

/**
 * Cabeçalho de um painel: com um painel só, o ícone, o título e a contagem; com várias abas, as abas (cada uma
 * com o botão de esconder só ela). À direita ficam as ações do painel e o botão de esconder.
 */
defineProps<{ active: PanelKey; tabs: PanelKey[]; draggable?: boolean }>()
const emit = defineEmits<{ activate: [key: PanelKey]; hide: [key: PanelKey]; drag: [keys: PanelKey[], e: PointerEvent] }>()
</script>

<template>
  <div class="mi-ph" :style="draggable ? { cursor: 'grab' } : undefined">
    <div v-if="tabs.length > 1" class="mi-tabs">
      <span v-for="key in tabs" :key="key" class="mi-tabwrap" :data-tab="key">
        <button type="button" class="mi-tabbtn" title="Drag to move only this tab" :aria-selected="key === active" :style="{ position: 'relative', color: key === active ? 'var(--c-1f2329)' : 'var(--c-5b5f6b)', background: key === active ? 'var(--sel-hover)' : 'transparent' }" @pointerdown="emit('drag', [key], $event)" @click="emit('activate', key)">
          <span style="display: inline-grid" :style="{ color: key === active ? 'var(--c-1f2329)' : 'var(--c-8a8f99)' }"><Icon :name="PANELS[key].icon" /></span>
          {{ PANELS[key].title }}
          <slot :name="`count-${key}`" />
        </button>
        <button type="button" class="mi-tabx" :title="`Hide ${PANELS[key].title}`" :aria-label="`Hide ${PANELS[key].title}`" @click="emit('hide', key)"><Icon name="tab-close" /></button>
      </span>
    </div>
    <template v-else>
      <span style="color: var(--c-1f2329, #1c2024); display: inline-grid; margin-left: var(--sp-8)"><Icon :name="PANELS[active].icon" /></span>
      <h2>{{ PANELS[active].title }}</h2>
      <slot :name="`count-${active}`" />
      <button v-if="!draggable" type="button" class="mi-grip" title="Drag container (with all its tabs)" aria-label="Drag container" @pointerdown.prevent="emit('drag', [active], $event)"><Icon name="grip" /></button>
    </template>
    <span style="margin-left: auto; display: inline-flex; align-items: center; gap: var(--sp-4)"><slot :name="`actions-${active}`" /></span>
    <button type="button" class="mi-btn mi-icon mi-x" title="Hide" aria-label="Hide" @click="emit('hide', active)"><Icon name="hide" /></button>
  </div>
</template>
