<script setup lang="ts">
import { computed } from 'vue'
import { CONCEPT_COLORS, CONCEPT_ICON, conceptOf } from '@/shared/domain/entities'
import { Icon } from '@/shared/ui'
import { useLifecycleStore } from '@/stores/lifecycle.store'
import { useSessionStore } from '@/stores/session.store'

/** Chip de uma entidade, na cor do conceito dela; o clique abre o ciclo de vida (Life cycle). */
const props = defineProps<{
  type: string
  id?: string | null
  label: string
  /** Marca de novo ou de removido neste ciclo. */
  change?: 'new' | 'removed' | null
  /** Mostra o ícone do conceito antes do texto. */
  icon?: boolean
  /** Mostra o número da entidade (#id) depois do texto. */
  idBadge?: string | number | null
  selected?: boolean
  static?: boolean
}>()

const lifecycle = useLifecycleStore()
const session = useSessionStore()
const kind = computed(() => conceptOf(props.type))
const colors = computed(() => CONCEPT_COLORS[kind.value])
const clickable = computed(() => !props.static && !!props.id && kind.value !== 'plan')

function open(ev: MouseEvent) {
  if (!clickable.value || !props.id || !session.selectedAgent) return
  lifecycle.open({ agent: session.selectedAgent, id: props.id, type: props.type, label: props.label }, { x: ev.clientX, y: ev.clientY })
}
</script>

<template>
  <button type="button" class="mi-belief" :title="clickable ? 'Life cycle of this ' + kind : label" :style="!clickable && { cursor: 'default' }" @click="open">
    <span
      class="mi-bchip"
      :style="{ background: change === 'removed' ? 'var(--chip-idle-bg)' : colors.bg, color: change === 'removed' ? 'var(--c-8a8f99)' : colors.fg, textDecoration: change === 'removed' ? 'line-through' : 'none', ...(icon || idBadge != null ? { display: 'inline-flex', alignItems: 'center', gap: 'var(--sp-4)' } : {}), boxShadow: selected ? '0 0 0 2px var(--c-4338ca)' : undefined }"
    >
      <Icon v-if="icon" :name="CONCEPT_ICON[kind]" />
      <span>{{ label }}</span>
      <span v-if="idBadge != null" style="opacity: 0.7">#{{ idBadge }}</span>
    </span>
    <span v-if="change === 'new'" class="mi-new" role="img" aria-label="new" title="new"><Icon name="badge-new" /></span>
  </button>
</template>
