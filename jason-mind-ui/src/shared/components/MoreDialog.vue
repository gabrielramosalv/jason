<script setup lang="ts">
import { computed, ref } from 'vue'
import { Icon } from '@/shared/ui'

/** Lista completa de uma seção cortada ("Show more"): título, contagem, busca opcional e itens coloridos. */
export interface MoreItem {
  key: string
  text: string
  bg: string
  color: string
  tip?: string
  chosen?: boolean
  /** Entidade removida: nome riscado. */
  removed?: boolean
  badge?: string | null
  /** Tipo do item (conceito da legenda), para o filtro por tipo. */
  kind?: string
}

const props = defineProps<{ title: string; items: MoreItem[]; searchable?: boolean; anchor: { left: number; bottom: number; maxHeight: number } }>()
const emit = defineEmits<{ close: []; pick: [key: string] }>()

const q = ref('')
const off = ref<string[]>([])
const kinds = computed(() => [...new Set(props.items.map((i) => i.kind).filter((k): k is string => !!k))])
const shown = computed(() => {
  const s = q.value.trim().toLowerCase()
  return props.items.filter((i) => (!s || i.text.toLowerCase().includes(s)) && !(i.kind && off.value.includes(i.kind)))
})
function toggleKind(kind: string) {
  off.value = off.value.includes(kind) ? off.value.filter((k) => k !== kind) : [...off.value, kind]
}
</script>

<template>
  <Teleport to=".mi-root">
    <div style="position: fixed; inset: 0; z-index: 38" @click="emit('close')" />
    <div role="dialog" :aria-label="title" class="mi-dialog" :style="{ position: 'fixed', zIndex: 39, left: `${anchor.left}px`, bottom: `${anchor.bottom}px`, width: '320px', maxHeight: `${anchor.maxHeight}px`, borderRadius: '12px' }">
      <div class="mi-ph"><h2>{{ title }}</h2><span class="mi-count">{{ items.length }}</span><button type="button" class="mi-btn mi-icon" title="Close" aria-label="Close" style="margin-left: auto; border-color: transparent; color: var(--c-8a8f99)" @click="emit('close')"><Icon name="close" /></button></div>
      <div v-if="searchable" style="padding: 0 var(--sp-8) var(--sp-8); flex-shrink: 0"><input v-model="q" type="text" class="mi-searchin" style="width: 100%" placeholder="Search…" aria-label="Search" /></div>
      <div v-if="searchable && kinds.length > 1" style="padding: 0 var(--sp-8) var(--sp-8); display: flex; flex-wrap: wrap; gap: var(--sp-4)">
        <button v-for="k in kinds" :key="k" type="button" class="mi-bchip" :aria-pressed="!off.includes(k)" :style="{ cursor: 'pointer', textTransform: 'capitalize', background: off.includes(k) ? 'transparent' : 'var(--sel-hover)', color: off.includes(k) ? 'var(--c-8a8f99)' : 'var(--c-1f2329)' }" @click="toggleKind(k)">{{ k }}</button>
      </div>
      <div style="flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 0 var(--sp-8) var(--sp-8); display: flex; flex-direction: column; gap: var(--sp-4)">
        <button v-for="i in shown" :key="i.key" type="button" :title="i.tip" class="mi-moreitem" :style="{ background: i.bg, color: i.color }" @click="emit('pick', i.key)">
          <span :style="{ flex: 1, minWidth: 0, overflowWrap: 'anywhere', textAlign: 'left', textDecoration: i.removed ? 'line-through' : 'none' }">{{ i.text }}</span>
          <span v-if="i.chosen" role="img" aria-label="Chosen" title="Chosen" style="display: inline-grid; flex-shrink: 0"><Icon name="chosen-small" /></span>
          <span v-if="i.badge" class="mi-pill" :style="{ background: 'var(--ov-08)', color: i.color }">#{{ i.badge }}</span>
        </button>
      </div>
    </div>
  </Teleport>
</template>
