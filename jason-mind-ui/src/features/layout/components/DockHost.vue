<script setup lang="ts">
import { computed, ref } from 'vue'
import { useLayoutStore } from '@/stores/layout.store'
import { layoutOf } from '../domain/dock-tree'
import type { Handle, PanelKey } from '../domain/dock-tree'
import { PANELS } from '../domain/panels'
import { useDockDrag } from '../composables/useDockDrag'
import PanelHeader from './PanelHeader.vue'

/**
 * Hospeda os painéis do layout: posiciona cada folha da árvore, com 4 px entre elas, e as divisórias que
 * redimensionam. O conteúdo de cada painel vem pelos slots `body-<chave>`, `actions-<chave>` e `count-<chave>`.
 */
const layout = useLayoutStore()
const host = ref<HTMLElement | null>(null)
const { hint, dragging, start: startDrag } = useDockDrag(host)

const arrangement = computed(() => layoutOf(layout.tree))

const pct = (n: number) => `${n * 100}%`

function leafStyle(rect: { x: number; y: number; w: number; h: number }) {
  return { position: 'absolute', left: `calc(${pct(rect.x)} + 2px)`, top: `calc(${pct(rect.y)} + 2px)`, width: `calc(${pct(rect.w)} - 4px)`, height: `calc(${pct(rect.h)} - 4px)` } as const
}

function handleStyle(h: Handle) {
  const { span } = h
  return h.o === 'h'
    ? { left: `calc(${pct(h.at)} - 4px)`, top: pct(span.y), width: '8px', height: pct(span.h) }
    : { left: pct(span.x), top: `calc(${pct(h.at)} - 4px)`, width: pct(span.w), height: '8px' }
}

function startResize(h: Handle, e: PointerEvent) {
  const el = host.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const extent = (h.o === 'h' ? rect.width * h.span.w : rect.height * h.span.h) || 1
  let last = h.o === 'h' ? e.clientX : e.clientY
  const target = e.currentTarget as HTMLElement
  target.classList.add('on')
  const move = (ev: PointerEvent) => {
    const now = h.o === 'h' ? ev.clientX : ev.clientY
    layout.resize(h.path, h.index, (now - last) / extent)
    last = now
  }
  const up = () => {
    target.classList.remove('on')
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

const tabsOf = (leaf: { k: PanelKey; t?: PanelKey[] }) => leaf.t ?? [leaf.k]
</script>

<template>
  <div class="mi-grid" :class="dragging && 'mi-dragging'">
  <div ref="host" class="mi-dockin">
    <section
      v-for="{ leaf, rect } in arrangement.leaves"
      :key="tabsOf(leaf).join('+')"
      class="mi-panel mi-card"
      :class="leaf.k === 'console' || leaf.k === 'msgs' || leaf.k === 'reason' ? 'mi-dock' : ''"
      :data-k="leaf.k"
      :aria-label="PANELS[leaf.k].title"
      :style="leafStyle(rect)"
    >
      <PanelHeader :active="leaf.k" :tabs="tabsOf(leaf)" @activate="layout.activate" @hide="layout.hide" @drag="(keys, e) => startDrag(keys, e)">
        <template v-for="key in tabsOf(leaf)" :key="key" #[`count-${key}`]><slot :name="`count-${key}`" /></template>
        <template #[`actions-${leaf.k}`]><slot :name="`actions-${leaf.k}`" /></template>
      </PanelHeader>
      <slot :name="`body-${leaf.k}`" />
    </section>
    <div
      v-for="h in arrangement.handles"
      :key="`${h.path.join('.')}-${h.index}`"
      class="mi-split"
      :class="h.o === 'h' ? 'mi-split-h' : 'mi-split-v'"
      role="separator"
      :aria-orientation="h.o === 'h' ? 'vertical' : 'horizontal'"
      :aria-label="h.o === 'h' ? 'Resize width' : 'Resize height'"
      :style="handleStyle(h)"
      @pointerdown.prevent="startResize(h, $event)"
    />
    <div v-if="hint" class="mi-drophint" :style="{ left: `${hint.left}px`, top: `${hint.top}px`, width: `${hint.width}px`, height: `${hint.height}px` }">{{ hint.label }}</div>
  </div>
  </div>
</template>
