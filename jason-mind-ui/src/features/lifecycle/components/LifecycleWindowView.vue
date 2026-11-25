<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { describeError } from '@/shared/api/describe-error'
import EntityChip from '@/shared/components/EntityChip.vue'
import { CONCEPT_COLORS, conceptOf } from '@/shared/domain/entities'
import { numericId } from '@/shared/domain/ids'
import { Icon } from '@/shared/ui'
import type { IconName } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { useLifecycleStore } from '@/stores/lifecycle.store'
import type { LifecycleWindow } from '@/stores/lifecycle.store'
import { useSessionStore } from '@/stores/session.store'
import { lifecycleService } from '../services/lifecycle.service'
import { stepTitle } from '@/features/reasoning/domain/step-catalog'
import { describeEntry, isRedundant, valueEntityLabel } from '../domain/entry-text'
import type { LifecycleEntry } from '../types/lifecycle.types'

/** Uma janela de Life cycle: nascimento, mudanças e fim da entidade, cada nó com causa, origem e posição. */
const props = defineProps<{ win: LifecycleWindow }>()
const emit = defineEmits<{ close: []; drag: [e: PointerEvent, mode: 'move' | 'e' | 's' | 'se'] }>()

const session = useSessionStore()
const layout = useLayoutStore()
const lifecycle = useLifecycleStore()

const rawEntries = ref<LifecycleEntry[]>([])
/** O que a lista mostra: sem as entradas de contabilidade que repetem outra do mesmo momento. */
const entries = computed(() => rawEntries.value.filter((e) => !isRedundant(e, rawEntries.value)))
const nextCursor = ref<number | string | null>(null)
const error = ref<string | null>(null)
const loading = ref(false)

/** Instante até o qual rolagens feitas pelo próprio código (centralizar a entrada) não disparam carga. */
let quietUntil = 0
const loadingNewer = ref(false)
let lastNewerAt = 0

/** A primeira página ou, com `more`, as entradas mais antigas que as carregadas (entram em cima, sem mover o que se lê). */
async function load(more = false) {
  loading.value = true
  try {
    const res = await lifecycleService.get(props.win.agent, props.win.id, { before: more ? nextCursor.value : null })
    if (!res.data) return
    const box = body.value
    const before = box?.scrollHeight ?? 0
    rawEntries.value = more ? [...rawEntries.value, ...res.data.entries] : res.data.entries
    nextCursor.value = res.data.nextCursor
    if (more && box) {
      quietUntil = Date.now() + 400
      await nextTick()
      box.scrollTop += box.scrollHeight - before
    }
    error.value = null
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    loading.value = false
  }
}

/** As entradas mais novas que a mais nova carregada (o agente continuou rodando); entram embaixo. */
async function loadNewer() {
  if (loadingNewer.value || !rawEntries.value.length) return
  loadingNewer.value = true
  lastNewerAt = Date.now()
  try {
    const newest = rawEntries.value.reduce((m, e) => Math.max(m, e.sequence), -1)
    const res = await lifecycleService.get(props.win.agent, props.win.id, { after: newest })
    const known = new Set(rawEntries.value.map((e) => e.sequence))
    const fresh = (res.data?.entries ?? []).filter((e) => !known.has(e.sequence)).reverse()
    if (fresh.length) rawEntries.value = [...fresh, ...rawEntries.value]
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    loadingNewer.value = false
  }
}

/** O fim da rolagem carrega mais: o topo traz as mais antigas e o fundo, as mais novas. */
function onScroll() {
  const box = body.value
  if (!box || Date.now() < quietUntil) return
  if (box.scrollTop < 48 && nextCursor.value !== null && !loading.value) void load(true)
  if (box.scrollHeight - box.scrollTop - box.clientHeight < 48 && Date.now() - lastNewerAt > 1500) void loadNewer()
}
/** A ocorrência selecionada: fica marcada até outra ser escolhida (não é temporária). */
/** A API entrega da mais recente para a mais antiga; a lista mostra da mais antiga (em cima) para a mais recente (embaixo). */
const shown = computed(() => [...entries.value].reverse())
const selectedSequence = ref<number | null>(null)
const body = ref<HTMLElement | null>(null)
const isSelected = (e: LifecycleEntry) => e.sequence === selectedSequence.value
const selectedIndex = computed(() => entries.value.findIndex(isSelected))
// a lista vem da mais recente para a mais antiga (a poda tira as antigas): `first` é a mais antiga e `last` a mais recente
const atFirst = computed(() => nextCursor.value === null && selectedIndex.value === entries.value.length - 1)
const atLast = computed(() => selectedIndex.value <= 0)

/** Rola a janela até a ocorrência selecionada ficar no meio da área visível. */
function centerSelected() {
  quietUntil = Date.now() + 700
  const box = body.value
  const el = box?.querySelector<HTMLElement>('[data-selected="true"]')
  if (!box || !el) return
  const b = box.getBoundingClientRect()
  const r = el.getBoundingClientRect()
  box.scrollTop += r.top - b.top - (b.height - r.height) / 2
}
/** Escolhe a ocorrência: o Reasoning a destaca e, com `navigate`, o inspetor vai ao ciclo dela. */
async function select(sequence: number | null, center = true, navigate = false) {
  selectedSequence.value = sequence
  const entry = entries.value.find((e) => e.sequence === sequence)
  if (entry) {
    lifecycle.setFocus(props.win.key, props.win.id, { cycle: entry.moment.cycle, step: entry.moment.step, iteration: entry.moment.iteration, ordinal: entry.moment.ordinal, kind: entry.kind })
    if (navigate) goTo(entry)
  }
  if (!center) return
  await nextTick()
  centerSelected()
  // o layout ainda pode se ajustar (fontes, janela recém-aberta): confere de novo logo depois
  window.setTimeout(centerSelected, 250)
}
/** Carrega páginas até a condição valer ou o histórico acabar. */
async function loadUntil(done: () => boolean) {
  for (let page = 0; page < 50 && !done() && nextCursor.value !== null; page++) await load(true)
}
/** Seleciona o pedido do Reasoning (o momento do passo) ou, sem pedido, o estado da entidade no ciclo atual. */
async function focusMoment() {
  const h = props.win.highlight
  if (h) {
    const sameMoment = (e: LifecycleEntry) => e.moment.cycle === h.cycle && e.moment.step === h.step && e.moment.iteration === h.iteration && e.moment.ordinal === h.ordinal
    // a entidade pode ter várias ocorrências no mesmo passo (muda e termina): vale a do tipo pedido
    const match = (e: LifecycleEntry) => sameMoment(e) && (!h.kind || e.kind === h.kind)
    await loadUntil(() => entries.value.some(sameMoment))
    const hit = entries.value.find(match) ?? entries.value.find(sameMoment)
    if (hit) return select(hit.sequence)
  }
  const cycle = session.shown?.cycle ?? session.selected?.newestCycle ?? Infinity
  await loadUntil(() => entries.value.some((e) => e.moment.cycle <= cycle))
  // a mais recente até o ciclo atual; se a entidade só existe depois dele, a mais antiga que há
  const current = entries.value.find((e) => e.moment.cycle <= cycle) ?? entries.value[entries.value.length - 1]
  if (current) await select(current.sequence)
}
async function move(to: 'first' | 'previous' | 'next' | 'last') {
  if ((to === 'next' && selectedIndex.value <= 0) || to === 'last') await loadNewer()
  if (to === 'first') await loadUntil(() => false)
  if (to === 'previous') await loadUntil(() => selectedIndex.value < entries.value.length - 1)
  const list = entries.value
  const i = Math.max(0, selectedIndex.value)
  const target = to === 'last' ? 0 : to === 'first' ? list.length - 1 : to === 'previous' ? Math.min(list.length - 1, i + 1) : Math.max(0, i - 1)
  if (list[target]) await select(list[target].sequence, true, true)
}
onMounted(async () => {
  await load()
  await focusMoment()
})
watch(() => props.win.flash, () => void focusMoment())

const icon = (e: LifecycleEntry): IconName => (e.kind === 'START' ? 'life-start' : e.kind === 'END' || e.kind === 'REMOVES' ? 'life-end' : e.kind === 'CHANGE' || e.kind === 'MODIFIES' ? 'life-change' : 'life-out')
const heading = (e: LifecycleEntry) => describeEntry(e, props.win.type)
const phaseIcon = (phase: string): IconName => (phase === 'SENSE' ? 'in-sense' : phase === 'DELIBERATE' ? 'in-deliberate' : 'chip-action')

function goTo(e: LifecycleEntry) {
  if (props.win.agent !== session.selectedAgent) session.selectAgent(props.win.agent)
  session.goToCycle(e.moment.cycle)
  layout.show('reason')
  layout.activate('reason')
}

const head = CONCEPT_COLORS[conceptOf(props.win.type)]
</script>

<template>
  <div role="dialog" aria-label="Life cycle" class="mi-dialog" :style="{ position: 'fixed', zIndex: props.win.z, left: `${win.x}px`, top: `${win.y}px`, width: `${win.w}px`, height: `${win.h}px`, maxWidth: '96vw', maxHeight: '90vh' }" @pointerdown="$emit('drag', $event, 'move')">
    <div class="mi-ph" style="cursor: grab; user-select: none; align-items: center; padding: var(--sp-2) var(--sp-4)">
      <span style="color: var(--c-1f2329); display: inline-grid; place-items: center; flex-shrink: 0; width: 20px; height: 28px; margin-left: var(--sp-8)"><Icon name="in-cycle" /></span>
      <span style="font-weight: 600; font-size: 13px; color: var(--c-1f2329)">Life cycle</span>
      <button type="button" class="mi-btn mi-icon" title="Close" aria-label="Close" style="margin-left: auto; flex-shrink: 0; border-color: transparent; color: var(--c-8a8f99)" @click="emit('close')"><Icon name="close" /></button>
    </div>
    <div style="flex: 0 0 auto; padding: 2px var(--sp-2) 0 var(--sp-12); display: flex; align-items: center; justify-content: space-between; gap: var(--sp-8)">
      <span class="mi-bchip" :title="win.label" :style="{ background: head.bg, color: head.fg, display: 'inline-flex', alignItems: 'baseline', gap: 'var(--sp-4)', flex: '0 1 auto', minWidth: 0, fontSize: '11.5px' }"><span style="min-width: 0; overflow-wrap: anywhere">{{ win.label }}</span><span v-if="win.type !== 'belief'" style="opacity: 0.7; flex-shrink: 0">#{{ numericId(win.id) }}</span></span>
    <div class="mi-cycgroup" style="margin: -5px 0 -5px auto; flex-shrink: 0" @pointerdown.stop>
      <button type="button" class="mi-btn mi-icon" title="First cycle" aria-label="First cycle" :style="{ opacity: atFirst ? 0.35 : 1 }" @click="move('first')"><Icon name="first" /></button>
      <button type="button" class="mi-btn mi-icon" title="Previous cycle" aria-label="Previous cycle" :style="{ opacity: atFirst ? 0.35 : 1 }" @click="move('previous')"><Icon name="previous" /></button>
      <button type="button" class="mi-btn mi-icon" title="Next cycle" aria-label="Next cycle" :style="{ opacity: atLast ? 0.35 : 1 }" @click="move('next')"><Icon name="next" /></button>
      <button type="button" class="mi-btn mi-icon" title="Last cycle" aria-label="Last cycle" :style="{ opacity: atLast ? 0.35 : 1 }" @click="move('last')"><Icon name="last" /></button>
    </div>
    </div>
    <div style="flex: 0 0 auto; height: 1px; background: var(--bd-card); margin: var(--sp-12) var(--sp-12) 0" />
    <div ref="body" @scroll.passive="onScroll" style="flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: var(--sp-12); display: flex; flex-direction: column; gap: var(--sp-12)">
      <p v-if="error" class="mi-warnbox" style="border-radius: 6px">{{ error }}</p>
      <div v-if="loading || nextCursor === null" style="display: flex; align-items: center; gap: var(--sp-8)">
        <span v-if="loading" class="mi-muted">Loading older…</span>
        <span v-else-if="entries.length" class="mi-muted">End of the retained history</span>
      </div>
      <div style="display: flex; flex-direction: column">
        <template v-for="(e, i) in shown" :key="e.sequence"><div class="mi-lentry" :class="isSelected(e) && 'mi-lsel'" :data-selected="isSelected(e)" style="display: flex; gap: var(--sp-16); position: relative">
          <div style="flex-shrink: 0; width: 14px; display: flex; flex-direction: column; align-items: center"><span :title="heading(e)" role="img" :aria-label="heading(e)" style="flex-shrink: 0; width: 14px; height: 22px; display: inline-flex; align-items: center; justify-content: center; color: var(--c-8a8f99)"><Icon :name="icon(e)" /></span></div>
          <div class="mi-lbody">
            <div style="display: flex; align-items: flex-start; gap: var(--sp-8)"><span style="font-size: 12.8px; line-height: 22px; overflow-wrap: anywhere; min-width: 0">{{ heading(e) }}</span><button type="button" class="mi-iterbtn mi-golife" title="Go to this cycle in Reasoning" aria-label="Go to this cycle in Reasoning" style="width: 22px; min-width: 22px; height: 22px; padding: 0; border: none; margin-left: auto; flex-shrink: 0" @click="select(e.sequence, false, true)"><Icon name="go" /></button></div>
            <div v-if="e.entity" class="mi-lrow"><span class="mi-llab">{{ e.kind === 'REMOVES' ? 'removed' : e.kind === 'MODIFIES' ? 'modified' : 'generated' }}</span><EntityChip :type="e.entity.type" :id="e.entity.id" :label="e.entity.label" :id-badge="e.entity.type === 'event' || e.entity.type === 'intention' ? numericId(e.entity.id) : null" icon /></div>
            <div v-if="e.into" class="mi-lrow"><span class="mi-llab">into</span><EntityChip :type="e.into.type" :id="e.into.id" :label="e.into.label" :id-badge="numericId(e.into.id)" icon /></div>
            <div v-if="e.cause?.by" class="mi-lrow"><span class="mi-llab">by</span><EntityChip :type="e.cause.by.type" :id="e.cause.by.id" :label="e.cause.by.label" :id-badge="e.cause.by.type === 'message' ? null : numericId(e.cause.by.id)" icon /></div>
            <div v-if="e.valueEntity" class="mi-lrow"><span class="mi-llab">{{ valueEntityLabel(e) }}</span><EntityChip :type="e.valueEntity.type" :id="e.valueEntity.id" :label="e.valueEntity.label" :id-badge="e.valueEntity.type === 'event' || e.valueEntity.type === 'intendedMeans' ? numericId(e.valueEntity.id) : null" icon /></div>
            <div v-if="e.cause?.sender" class="mi-lrow"><span class="mi-llab">from</span><span style="display: inline-flex; align-items: center; gap: var(--sp-4); padding-left: var(--sp-4)"><Icon name="from" />{{ e.cause.sender }}</span></div>
            <div v-if="e.cause?.location?.plan" class="mi-lrow"><span class="mi-llab">at</span><span class="mi-bchip" :style="{ cursor: 'default', background: CONCEPT_COLORS.plan.bg, color: CONCEPT_COLORS.plan.fg, display: 'inline-flex', alignItems: 'center', gap: 'var(--sp-4)', maxWidth: '100%' }"><Icon name="chip-plan" /><span style="white-space: nowrap; line-height: 12px">{{ e.cause.location.plan }}</span><span v-if="e.cause.location.line" :title="`Line ${e.cause.location.line} of the plan`" style="opacity: 0.7; display: inline-flex; align-items: center; line-height: 1"><Icon name="line" /><span style="display: block; line-height: 12px">{{ e.cause.location.line }}</span></span></span></div>
            <div class="mi-lrow" style="min-height: 22px">
              <span class="mi-llab">in</span>
              <div style="display: flex; align-items: center; min-width: 0">
              <span style="display: inline-flex; align-items: center; gap: var(--sp-4); height: 22px"><Icon :name="phaseIcon(e.moment.phase)" /><span>{{ stepTitle(e.moment.step) }}</span><span style="display: inline-flex; align-items: center; gap: var(--sp-4); margin-left: var(--sp-4)"><Icon name="in-cycle" />{{ e.moment.cycle }}</span></span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="i < shown.length - 1" style="height: 1px; background: var(--bd-card); margin: var(--sp-12) 0" />
        </template>
      </div>
      <span v-if="loadingNewer" class="mi-muted">Loading newer…</span>
    </div>
    <div aria-hidden="true" style="position: absolute; top: 36px; right: 0; bottom: 12px; width: 6px; cursor: ew-resize; z-index: 2" @pointerdown.stop.prevent="$emit('drag', $event, 'e')" />
    <div aria-hidden="true" style="position: absolute; left: 0; right: 12px; bottom: 0; height: 6px; cursor: ns-resize; z-index: 2" @pointerdown.stop.prevent="$emit('drag', $event, 's')" />
    <div aria-hidden="true" title="Resize" style="position: absolute; right: 0; bottom: 0; width: 12px; height: 12px; cursor: nwse-resize; z-index: 3" @pointerdown.stop.prevent="$emit('drag', $event, 'se')" />
  </div>
</template>
