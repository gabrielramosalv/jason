<script setup lang="ts">
import { computed } from 'vue'
import EntityChip from '@/shared/components/EntityChip.vue'
import PanelSection from '@/shared/components/PanelSection.vue'
import { annotationSource } from '@/shared/domain/entities'
import { useInspectorStore } from '@/stores/inspector.store'
import type { Belief } from '../types/state.types'

/** Painel Beliefs: Mental notes, Messages (por remetente) e Environment, cada um recolhível. */
const inspector = useInspectorStore()

const visible = computed<Belief[]>(() => (inspector.state?.beliefs ?? []).filter((b) => !b.annotations.includes('hide_in_mind_inspector')))

function groupOf(b: Belief): { group: 'self' | 'msg' | 'env'; from: string | null } {
  const src = annotationSource(b.annotations)
  if (src === 'percept' || b.categories.includes('environment')) return { group: 'env', from: null }
  if (src && src !== 'self') return { group: 'msg', from: src }
  return { group: 'self', from: null }
}

const self = computed(() => visible.value.filter((b) => groupOf(b).group === 'self'))
const env = computed(() => visible.value.filter((b) => groupOf(b).group === 'env'))
const bySender = computed(() => {
  const map = new Map<string, Belief[]>()
  for (const b of visible.value) {
    const { group, from } = groupOf(b)
    if (group === 'msg' && from) map.set(from, [...(map.get(from) ?? []), b])
  }
  return [...map.entries()]
})
const msgCount = computed(() => bySender.value.reduce((n, [, l]) => n + l.length, 0))

const sections = computed(() => [
  { key: 'bel-self', group: 'self' as const, label: 'Mental notes', icon: 'group-notes' as const, count: self.value.length },
  { key: 'bel-msg', group: 'msg' as const, label: 'Messages', icon: 'group-messages' as const, count: msgCount.value },
  { key: 'bel-env', group: 'env' as const, label: 'Environment', icon: 'group-environment' as const, count: env.value.length },
].filter((s) => inspector.beliefShown(s.group) && s.count > 0))

const empty = computed(() => !!inspector.state && !sections.value.length)
</script>

<template>
  <div class="mi-body-scroll">
    <div v-if="empty" class="mi-emptybig">No beliefs in this cycle</div>
    <template v-for="(s, i) in sections" :key="s.key">
      <PanelSection :label="s.label" :icon="s.icon" :count="s.count" :open="inspector.isOpen(s.key)" :divider="i > 0" @toggle="inspector.toggleSection(s.key)">
        <div v-if="s.group === 'msg'">
          <template v-for="[from, list] in bySender" :key="from">
            <button type="button" class="mi-sechead" style="padding: var(--sp-4) 0" @click="inspector.toggleSection(`${s.key}:${from}`)">
              <span :style="{ display: 'inline-grid', transform: inspector.isOpen(`${s.key}:${from}`) ? 'none' : 'rotate(-90deg)' }"><svg width="12" height="12" viewBox="0 0 14 14" fill="none"><path d="M3.5 5.5L7 9l3.5-3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" /></svg></span>
              <span>{{ from }}</span>
            </button>
            <div v-if="inspector.isOpen(`${s.key}:${from}`)" class="mi-chips" style="padding-left: var(--sp-16)">
              <EntityChip v-for="b in list" :key="b.id" type="belief" :id="b.id" :label="b.content" :change="b.change" />
            </div>
          </template>
        </div>
        <div v-else class="mi-chips">
          <EntityChip v-for="b in s.group === 'self' ? self : env" :key="b.id" type="belief" :id="b.id" :label="b.content" :change="b.change" />
        </div>
      </PanelSection>
    </template>
  </div>
</template>
