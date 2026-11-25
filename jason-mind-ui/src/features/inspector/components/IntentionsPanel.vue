<script setup lang="ts">
import { computed, ref } from 'vue'
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

/** Painel Intentions: as intenções do ciclo agrupadas por categoria, cada uma com todos os objetivos da sua pilha (o primeiro e os que ele chamou). */
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

const GOAL_LIMIT = 3
const more = ref<{ intention: Intention; anchor: { left: number; bottom: number; maxHeight: number } } | null>(null)
/** A pilha, do topo (o que roda) para a base: o objetivo que abriu a intenção é o último. */
const goalsOf = (i: Intention) => i.levels.map((level, at) => ({ level, kind: at === 0 ? ('goal' as const) : ('subgoal' as const) })).reverse()
/** Até {@link GOAL_LIMIT} objetivos: os mais novos e o original; os do meio ficam atrás do "Show more". */
const shownGoals = (i: Intention) => {
  const all = goalsOf(i)
  return all.length <= GOAL_LIMIT ? all : [...all.slice(0, GOAL_LIMIT - 1), all[all.length - 1]!]
}
const hiddenGoals = (i: Intention) => {
  const all = goalsOf(i)
  return all.length <= GOAL_LIMIT ? [] : all.slice(GOAL_LIMIT - 1, -1)
}
const moreItems = computed<MoreItem[]>(() => (more.value ? hiddenGoals(more.value.intention) : []).map(({ level, kind }) => ({ key: level.intendedMeansId, text: level.trigger, bg: CONCEPT_COLORS[kind].bg, color: CONCEPT_COLORS[kind].fg, badge: numericId(level.intendedMeansId) })))

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
            <div class="mi-intgrp">
              <div style="display: flex; align-items: center; gap: var(--sp-2)">
                <EntityChip type="intention" :id="it.id" :label="it.id" :id-badge="numericId(it.id)" icon :selected="it.id === inspector.selectedIntentionId" @click.capture="inspector.selectIntention(it.id)" />
              </div>
              <template v-for="(gl, n) in shownGoals(it)" :key="gl.level.intendedMeansId">
                <div v-if="hiddenGoals(it).length && n === shownGoals(it).length - 1" class="mi-goalrow">
                  <span class="mi-golink" aria-hidden="true"><Icon name="goal-link" /></span>
                  <button type="button" class="mi-bchip" :title="`Show ${hiddenGoals(it).length} more goals`" style="background: transparent; color: var(--c-8a8f99); cursor: pointer" @click="openMore(it, $event)">Show more (+{{ hiddenGoals(it).length }})</button>
                </div>
                <div class="mi-goalrow">
                  <span class="mi-golink" aria-hidden="true"><Icon name="goal-link" /></span>
                  <EntityChip type="intendedMeans" :kind="gl.kind" :id="gl.level.intendedMeansId" :label="gl.level.trigger" :id-badge="numericId(gl.level.intendedMeansId)" icon />
                </div>
              </template>
            </div>
          </template>
        </div>
      </PanelSection>
    </template>
    <MoreDialog v-if="more" :title="`More goals of Intention #${numericId(more.intention.id)}`" :items="moreItems" :anchor="more.anchor" @close="more = null" @pick="more = null" />
  </div>
</template>
