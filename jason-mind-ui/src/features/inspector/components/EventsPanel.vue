<script setup lang="ts">
import { computed } from 'vue'
import EntityChip from '@/shared/components/EntityChip.vue'
import PanelSection from '@/shared/components/PanelSection.vue'
import { useInspectorStore } from '@/stores/inspector.store'

/** Painel Events: os eventos do ciclo agrupados pelo lugar em que estão (selecionado, fila). */
const inspector = useInspectorStore()

const events = computed(() => inspector.state?.events ?? [])
const label = (place: string) => place.charAt(0) + place.slice(1).toLowerCase().replace(/_/g, ' ')
const groups = computed(() => {
  const map = new Map<string, typeof events.value>()
  for (const e of events.value) map.set(e.place, [...(map.get(e.place) ?? []), e])
  return [...map.entries()].map(([place, items]) => ({ place, items }))
})
</script>

<template>
  <div class="mi-body-scroll">
    <div v-if="inspector.state && !events.length" class="mi-empty">No events in this cycle</div>
    <template v-for="(g, n) in groups" :key="g.place">
      <PanelSection :label="label(g.place)" icon="ev-icon" :count="g.items.length" :open="inspector.isOpen(`ev-${g.place}`)" :divider="n > 0" @toggle="inspector.toggleSection(`ev-${g.place}`)">
        <div style="display: flex; flex-direction: column; gap: var(--sp-8); padding-top: var(--sp-2); padding-bottom: var(--sp-4)">
          <div v-for="e in g.items" :key="e.id" :title="`${e.state.toLowerCase().replace(/_/g, ' ')}${e.atomic ? ' · atomic' : ''}`" style="display: flex; align-items: center; gap: var(--sp-4); min-width: 0">
            <EntityChip type="event" :id="e.id" :label="e.trigger" :change="e.change" icon />
          </div>
        </div>
      </PanelSection>
    </template>
  </div>
</template>
