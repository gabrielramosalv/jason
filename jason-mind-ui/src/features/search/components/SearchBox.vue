<script setup lang="ts">
import { reactive, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { Icon } from '@/shared/ui'
import type { IconName } from '@/shared/ui'
import { useSearchStore } from '@/stores/search.store'
import { useSessionStore } from '@/stores/session.store'
import type { SearchType } from '../types/search.types'

/** Caixa flutuante de busca nos ciclos: texto, tipos de entidade, resultados e navegação entre eles. */
const emit = defineEmits<{ close: [] }>()
const search = useSearchStore()
const session = useSessionStore()
const { query, hits, total, index, active, types, mode, onlyHits } = storeToRefs(search)

const pos = reactive({ x: 12, y: 12, dragging: false })
const DEBOUNCE_MS = 250
let timer: ReturnType<typeof setTimeout> | null = null

/** Espera 250 ms depois da última tecla antes de buscar [D-022]. */
function onInput(value: string) {
  query.value = value
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => void search.run(), DEBOUNCE_MS)
}
watch(() => session.selectedAgent, () => search.clear())

const FILTERS: { type: SearchType; title: string; icon: IconName }[] = [
  { type: 'intention', title: 'Intention', icon: 'intention' },
  { type: 'belief', title: 'Belief', icon: 'belief' },
  { type: 'message', title: 'Message', icon: 'message' },
  { type: 'event', title: 'Event', icon: 'event' },
]

function startDrag(e: PointerEvent) {
  if ((e.target as HTMLElement).closest('button, input')) return
  const o = { px: e.clientX, py: e.clientY, x: pos.x, y: pos.y }
  pos.dragging = true
  const move = (ev: PointerEvent) => {
    pos.x = Math.max(0, o.x + ev.clientX - o.px)
    pos.y = Math.max(0, o.y + ev.clientY - o.py)
  }
  const up = () => {
    pos.dragging = false
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}
</script>

<template>
  <div class="mi-zoombox" role="search" aria-label="Search cycles" style="position: absolute; z-index: 6; opacity: 1; gap: var(--sp-4); padding: var(--sp-4) var(--sp-4) var(--sp-4) var(--sp-12)" :style="{ left: `${pos.x}px`, top: `${pos.y}px`, boxShadow: pos.dragging ? '0 8px 24px rgba(0,0,0,.3)' : '0 2px 10px rgba(0,0,0,.1)' }" @pointerdown="startDrag">
    <Icon name="search-small" />
    <input :value="query" type="text" class="mi-tlq" placeholder="Search in cycles" aria-label="Search cycles" :readonly="mode === 'deed'" @input="onInput(($event.target as HTMLInputElement).value)" @keydown.enter.exact.prevent="search.go(1)" @keydown.enter.shift.prevent="search.go(-1)" @keydown.esc="search.clear()" />
    <button v-if="mode === 'deed'" type="button" class="mi-zbtn" title="Searching one deed: clear the search to filter by type" aria-label="Deed" aria-pressed="true" style="flex-shrink: 0; color: var(--c-1f2329); background: var(--sel-bg)"><Icon name="chip-action" /></button>
    <template v-else>
    <button v-for="f in FILTERS" :key="f.type" type="button" class="mi-zbtn" :title="f.title" :aria-label="f.title" :aria-pressed="types.includes(f.type)" :style="{ flexShrink: 0, color: types.includes(f.type) ? 'var(--c-1f2329)' : 'var(--c-8a8f99)' }" @click="search.toggleType(f.type)"><Icon :name="f.icon" /></button>
    </template>
    <template v-if="active">
      <span style="padding: 0 var(--sp-4); font-size: 11.5px; color: var(--c-8a8f99); white-space: nowrap; font-variant-numeric: tabular-nums" aria-live="polite">{{ hits.length ? index + 1 : 0 }} of {{ total }}</span>
      <button type="button" class="mi-zbtn" title="Previous result" aria-label="Previous result" :disabled="!hits.length" style="flex-shrink: 0" @click="search.go(-1)"><Icon name="result-previous" /></button>
      <button type="button" class="mi-zbtn" title="Next result" aria-label="Next result" :disabled="!hits.length" style="flex-shrink: 0" @click="search.go(1)"><Icon name="result-next" /></button>
      <button type="button" class="mi-zbtn" title="Clear search" aria-label="Clear search" style="flex-shrink: 0" @click="search.clear()"><Icon name="clear" /></button>
    </template>
    <button type="button" class="mi-zbtn" :title="onlyHits ? 'Navigate all cycles' : 'Only navigate matches'" :aria-label="onlyHits ? 'Navigate all cycles' : 'Only navigate matches'" :aria-pressed="onlyHits" :style="{ flexShrink: 0, background: onlyHits ? 'var(--sel-bg)' : 'transparent' }" @click="onlyHits = !onlyHits"><Icon name="filter" /></button>
    <button type="button" class="mi-zbtn" title="Minimize search" aria-label="Minimize search" style="flex-shrink: 0" @click="emit('close')"><Icon name="minimize" /></button>
  </div>
</template>
