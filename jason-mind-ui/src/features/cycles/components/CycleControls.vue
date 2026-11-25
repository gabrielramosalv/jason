<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { Icon } from '@/shared/ui'
import { useSearchStore } from '@/stores/search.store'
import { useSessionStore } from '@/stores/session.store'

/** Controles de ciclo no centro da barra: primeiro, anterior, próximo, último (ao vivo) e o número do ciclo. */
const session = useSessionStore()
const search = useSearchStore()
const { selected, shown, isLive } = storeToRefs(session)

const current = computed(() => shown.value?.cycle ?? selected.value?.newestCycle ?? 0)
const total = computed(() => selected.value?.newestCycle ?? 0)
const oldest = computed(() => selected.value?.oldestCycle ?? 0)

const editing = ref(false)
const draft = ref<string | number>('')
const input = ref<HTMLInputElement | null>(null)

async function edit() {
  draft.value = String(current.value)
  editing.value = true
  await nextTick()
  input.value?.select()
}

function commit() {
  editing.value = false
  const n = Number(draft.value)
  if (String(draft.value).trim() !== '' && Number.isFinite(n)) session.goToCycle(n)
}

/** Com "só os acertos" ligado e uma busca ativa, anterior e próximo saltam entre os ciclos com resultado. */
function stepBy(direction: 1 | -1) {
  if (search.onlyHits && search.active) {
    const target = search.hitCycle(current.value, direction)
    if (target != null) session.goToCycle(target)
    return
  }
  session.step(direction)
}

const atFirst = computed(() => current.value <= oldest.value)
const op = (dim: boolean) => (dim ? 0.35 : 1)
</script>

<template>
  <div v-if="selected" class="mi-cyc" data-tour="cycles">
    <div class="mi-cycgroup">
      <button type="button" class="mi-btn mi-icon" title="First cycle" aria-label="First cycle" :style="{ opacity: op(atFirst) }" @click="session.goToCycle(oldest)"><Icon name="first" /></button>
      <button type="button" class="mi-btn mi-icon" title="Previous cycle" aria-label="Previous cycle" :style="{ opacity: op(atFirst) }" @click="stepBy(-1)"><Icon name="previous" /></button>
      <button type="button" class="mi-btn mi-icon" title="Next cycle" aria-label="Next cycle" :style="{ opacity: op(isLive) }" @click="stepBy(1)"><Icon name="next" /></button>
      <button type="button" class="mi-btn mi-icon" title="Go to the last cycle" aria-label="Go to the last cycle" data-tour="live" :style="{ opacity: op(isLive) }" @click="session.goLive()"><Icon name="last" /></button>
    </div>
    <span class="mi-cycinfo" :title="isLive ? undefined : `Current cycle of ${total}`">
      <Icon name="cycle-current" />
      <input v-if="editing" ref="input" v-model="draft" type="number" class="mi-cycinput" aria-label="Go to cycle" :min="oldest" :max="total" @keydown.enter="commit" @keydown.esc="editing = false" @blur="commit" />
      <button v-else type="button" class="mi-cyclink" title="Click to go to a specific cycle" aria-label="Go to a specific cycle" style="background: transparent; cursor: text; font-size: 11.5px; font-variant-numeric: tabular-nums; margin: 0" @click="edit">{{ current }}</button>
      <span v-if="!isLive" style="color: var(--c-8a8f99)">/ {{ total }}</span>
    </span>
    <span v-if="isLive" class="mi-cycinfo" style="margin: 0 var(--sp-8) 0 var(--sp-12)"><span class="mi-live" />Real time</span>
  </div>
</template>
