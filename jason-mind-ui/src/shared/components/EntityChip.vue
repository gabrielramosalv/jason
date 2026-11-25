<script setup lang="ts">
import { computed } from 'vue'
import { CONCEPT_COLORS, CONCEPT_ICON, chipText, conceptOf } from '@/shared/domain/entities'
import type { ConceptKind } from '@/shared/domain/entities'
import { useInspectorStore } from '@/stores/inspector.store'
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
  /** Força o conceito do chip (o objetivo que abriu a intenção ou um subobjetivo). */
  kind?: ConceptKind
}>()

const lifecycle = useLifecycleStore()
const session = useSessionStore()
const inspector = useInspectorStore()
const kind = computed<ConceptKind>(() => props.kind ?? (props.type === 'intendedMeans' ? inspector.goalKindOf(props.id) : conceptOf(props.type)))
const colors = computed(() => CONCEPT_COLORS[kind.value])
const text = computed(() => chipText(props.type, props.label))
const clickable = computed(() => !props.static && !!props.id && kind.value !== 'plan')

function open(ev: MouseEvent) {
  if (!clickable.value || !props.id || !session.selectedAgent) return
  lifecycle.open({ agent: session.selectedAgent, id: props.id, type: props.type, label: text.value }, { x: ev.clientX, y: ev.clientY })
}
</script>

<template>
  <button type="button" class="mi-belief" :title="clickable ? 'Life cycle of this ' + kind : label" :style="!clickable && { cursor: 'default' }" @click="open">
    <span
      class="mi-bchip"
      :style="{ background: colors.bg, color: colors.fg, ...(icon || idBadge != null ? { display: 'inline-flex', alignItems: 'center', gap: 'var(--sp-4)' } : {}), boxShadow: selected ? '0 0 0 2px var(--c-4338ca)' : undefined }"
    >
      <Icon v-if="icon" :name="CONCEPT_ICON[kind]" />
      <span>{{ text }}</span>
      <span v-if="idBadge != null" style="opacity: 0.7">#{{ idBadge }}</span>
    </span>
    <span v-if="change === 'new'" class="mi-new" role="img" aria-label="new" title="new"><Icon name="badge-new" /></span>
    <span v-else-if="change === 'removed'" class="mi-new" role="img" aria-label="removed" title="removed"><Icon name="badge-removed" /></span>
  </button>
</template>
