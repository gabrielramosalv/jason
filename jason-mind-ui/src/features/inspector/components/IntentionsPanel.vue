<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import EntityChip from '@/shared/components/EntityChip.vue'
import MoreDialog from '@/shared/components/MoreDialog.vue'
import type { MoreItem } from '@/shared/components/MoreDialog.vue'
import PanelSection from '@/shared/components/PanelSection.vue'
import { numericId } from '@/shared/domain/ids'
import { CONCEPT_COLORS } from '@/shared/domain/entities'
import { Icon } from '@/shared/ui'
import type { IconName } from '@/shared/ui'
import { useInspectorStore } from '@/stores/inspector.store'
import { intentionCategory } from '../domain/intention-category'
import type { IntentionCategory } from '../domain/intention-category'
import type { Intention } from '../types/state.types'

/** Painel Intentions: as intenções do ciclo agrupadas por categoria, com os subobjetivos de cada uma. */
const inspector = useInspectorStore()

const SECTIONS: { key: IntentionCategory; label: string; icon: IconName }[] = [
  { key: 'executing', label: 'Executing', icon: 'int-executing' },
  { key: 'environment', label: 'Waiting (action)', icon: 'int-environment' },
  { key: 'waiting', label: 'Waiting', icon: 'int-waiting' },
  { key: 'queue', label: 'Queue', icon: 'int-queue' },
  { key: 'suspended', label: 'Suspended', icon: 'int-suspended' },
]

const intentions = computed(() => inspector.state?.intentions ?? [])
const groups = computed(() =>
  SECTIONS.map((s) => ({ ...s, items: intentions.value.filter((i) => intentionCategory(i) === s.key) })).filter((s) => s.items.length),
)

const SUB_LIMIT = 3
const subOpen = reactive<Record<string, boolean>>({})
const more = ref<{ intention: Intention; anchor: { left: number; bottom: number; maxHeight: number } } | null>(null)
const subsOf = (i: Intention) => i.levels.slice(1)
const visibleSubs = (i: Intention) => subsOf(i).slice(0, SUB_LIMIT)
const moreItems = computed<MoreItem[]>(() => subsOf(more.value?.intention ?? ({ levels: [] } as unknown as Intention)).map((l) => ({ key: l.intendedMeansId, text: l.trigger, bg: CONCEPT_COLORS.subgoal.bg, color: CONCEPT_COLORS.subgoal.fg })))

function openMore(it: Intention, ev: MouseEvent) {
  const r = (ev.currentTarget as HTMLElement).getBoundingClientRect()
  more.value = { intention: it, anchor: { left: Math.max(8, r.left), bottom: Math.max(8, window.innerHeight - r.top + 6), maxHeight: Math.max(180, r.top - 16) } }
}
</script>

<template>
  <div class="mi-body-scroll">
    <div v-if="inspector.state && !intentions.length" class="mi-empty">No intentions in this cycle</div>
    <template v-for="(g, n) in groups" :key="g.key">
      <PanelSection :label="g.label" :icon="g.icon" :count="g.items.length" :open="inspector.isOpen(`int-${g.key}`)" :divider="n > 0" @toggle="inspector.toggleSection(`int-${g.key}`)">
        <div class="mi-chips">
          <template v-for="it in g.items" :key="it.id">
            <EntityChip v-if="!subsOf(it).length" type="intention" :id="it.id" :label="it.levels[0]?.trigger ?? it.id" :id-badge="numericId(it.id)" :selected="it.id === inspector.selectedIntentionId" @click.capture="inspector.selectIntention(it.id)" />
            <div v-else class="mi-intgrp">
              <div style="display: flex; align-items: center; gap: var(--sp-2)">
                <EntityChip type="intention" :id="it.id" :label="it.levels[0]?.trigger ?? it.id" :id-badge="numericId(it.id)" @click.capture="inspector.selectIntention(it.id)" />
                <button
                  type="button"
                  class="mi-zbtn mi-subtog"
                  :title="`Show or hide subgoals (${subsOf(it).length})`"
                  aria-label="Show or hide subgoals"
                  :aria-expanded="subOpen[it.id] !== false"
                  style="height: 20px; min-width: 20px; padding: 0 var(--sp-4); display: inline-flex; align-items: center; justify-content: center; gap: 1px; width: auto"
                  :style="{ color: 'var(--c-b0306f)' }"
                  @click="subOpen[it.id] = subOpen[it.id] === false"
                >
                  <Icon v-if="subOpen[it.id] !== false" name="subgoals-open" />
                  <template v-else><Icon name="chip-subgoal" /><span style="font-size: 10px; font-weight: 600; line-height: 1">{{ subsOf(it).length }}</span></template>
                </button>
              </div>
              <template v-if="subOpen[it.id] !== false">
                <button v-if="subsOf(it).length > SUB_LIMIT" type="button" class="mi-bchip" :title="`Show all ${subsOf(it).length} subgoals`" style="background: transparent; color: var(--c-8a8f99); cursor: pointer" @click="openMore(it, $event)">Show more (+{{ subsOf(it).length - SUB_LIMIT }})</button>
                <span v-for="sb in visibleSubs(it)" :key="sb.intendedMeansId" class="mi-bchip" :title="`Subgoal on top of intention #${numericId(it.id)}`" :style="{ background: CONCEPT_COLORS.subgoal.bg, color: CONCEPT_COLORS.subgoal.fg, display: 'inline-flex', alignItems: 'center', gap: 'var(--sp-4)' }">
                  <Icon name="chip-subgoal" /><span>{{ sb.trigger }}</span>
                </span>
              </template>
            </div>
          </template>
        </div>
      </PanelSection>
    </template>
    <MoreDialog v-if="more" :title="`Subgoals of #${numericId(more.intention.id)}`" :items="moreItems" :anchor="more.anchor" @close="more = null" @pick="more = null" />
  </div>
</template>
