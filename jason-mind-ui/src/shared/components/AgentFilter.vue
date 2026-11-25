<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { Icon } from '@/shared/ui'
import { useSessionStore } from '@/stores/session.store'

/** Botão de filtro de agentes com a lista de marcas (usado no Console e em Messages). */
defineProps<{ selected: string[]; open: boolean; label: string }>()
const emit = defineEmits<{ toggle: []; close: []; pick: [name: string] }>()
const { agents } = storeToRefs(useSessionStore())
</script>

<template>
  <div style="position: relative; display: inline-flex">
    <button type="button" class="mi-btn mi-icon" :title="label" :aria-label="label" :aria-pressed="open" :style="{ color: 'var(--c-1f2329)', ...(open || selected.length ? { background: 'var(--sel-bg)' } : {}) }" @click="emit('toggle')"><Icon name="filter" /></button>
    <template v-if="open">
      <div aria-hidden="true" style="position: fixed; inset: 0; z-index: 4" @click="emit('close')" />
      <div role="group" aria-label="Agents" class="mi-menu2">
        <button v-for="a in agents" :key="a.name" type="button" class="mi-btn" :aria-pressed="selected.includes(a.name)" :style="{ justifyContent: 'flex-start', gap: 'var(--sp-12)', background: selected.includes(a.name) ? 'var(--sel-bg)' : 'transparent' }" @click="emit('pick', a.name)">
          <span>{{ a.name }}</span><Icon v-if="selected.includes(a.name)" name="check" />
        </button>
      </div>
    </template>
  </div>
</template>
