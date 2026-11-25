<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, toRef, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { Icon } from '@/shared/ui'
import { useSearchStore } from '@/stores/search.store'
import { useSessionStore } from '@/stores/session.store'
import { useTimeline } from '../composables/useTimeline'
import { useTimelineFocus } from '../composables/useTimelineFocus'

/**
 * Faixa de ciclos: um ponto por ciclo, com as três fases acesas quando houve atividade. Arrastar o fundo anda
 * pela faixa; arrastar o ponto do ciclo escolhido move a âncora (onde ele fica na tela); os botões travam os
 * eixos e a roda com Ctrl dá zoom.
 */
const props = defineProps<{ relevantOnly: boolean }>()

const session = useSessionStore()
const search = useSearchStore()
const { selectedAgent, selected, shown, isLive, cycle } = storeToRefs(session)
const focus = useTimelineFocus()

const BASE_CELL = 52
const cell = computed(() => BASE_CELL * focus.zoom.value)
const host = ref<HTMLElement | null>(null)
let observer: ResizeObserver | null = null

const bounds = computed(() => (selected.value?.oldestCycle != null && selected.value.newestCycle != null ? { oldest: selected.value.oldestCycle, newest: selected.value.newestCycle } : null))
const current = computed(() => shown.value?.cycle ?? cycle.value ?? selected.value?.newestCycle ?? null)

const { phases, rendered, viewStart, offscreen, pan, follow, setVisibleCount } = useTimeline(selectedAgent, bounds, current, isLive, toRef(focus.anchor, 'x'), focus.lockX)

const cells = computed(() => rendered.value.filter((c) => !props.relevantOnly || Object.values(phases.value.get(c) ?? {}).some(Boolean) || c === current.value))

/**
 * Movimento em lote (vários ciclos entram ou saem de uma vez): cada célula espera um pouco mais quanto mais longe
 * está do ciclo atual, então as mais próximas do ponto chegam primeiro e as distantes depois. Um passo só (ao vivo,
 * um ciclo novo por vez) não escalona, para a faixa não ondular.
 */
const STAGGER_MS = 45
const STAGGER_MAX = 14
const batch = ref(false)
let batchTimer = 0
watch(cells, (now, before) => {
  const had = new Set(before)
  const has = new Set(now)
  const changed = now.filter((c) => !had.has(c)).length + before.filter((c) => !has.has(c)).length
  batch.value = changed > 2
  window.clearTimeout(batchTimer)
  if (batch.value) batchTimer = window.setTimeout(() => (batch.value = false), 1600)
})
onBeforeUnmount(() => window.clearTimeout(batchTimer))
const delayOf = (c: number) => (batch.value && current.value != null ? `${Math.min(Math.abs(c - current.value), STAGGER_MAX) * STAGGER_MS}ms` : '0ms')

function measure() {
  if (host.value) setVisibleCount(Math.floor(host.value.clientWidth / cell.value))
}
onMounted(() => {
  measure()
  observer = new ResizeObserver(measure)
  if (host.value) observer.observe(host.value)
})
onBeforeUnmount(() => observer?.disconnect())

/** Cobertura de cada resultado da busca: do primeiro ciclo ao último, ou até o mais novo se a entidade ainda existe. */
const hitRanges = computed(() => search.hits.map((h) => [h.firstCycle, h.lastCycle ?? bounds.value?.newest ?? h.firstCycle] as const))
const isHit = (c: number) => hitRanges.value.some(([a, b]) => c >= a && c <= b)

type Drag = { kind: 'pan' | 'anchor'; x: number; y: number; id: number; captured: boolean }
let drag: Drag | null = null
const showGuides = ref(false)

/** As guias da âncora cruzam o centro real da bolinha do ciclo atual (medido no DOM), não uma fração da tela. */
const guide = ref({ x: 0, y: 0 })
let frame = 0
function measureGuide() {
  const dot = host.value?.querySelector<HTMLElement>('.mi-tlcell[aria-pressed="true"] .mi-tldot')
  if (!dot || !host.value) return
  const d = dot.getBoundingClientRect()
  const h = host.value.getBoundingClientRect()
  guide.value = { x: d.left + d.width / 2 - h.left, y: d.top + d.height / 2 - h.top }
}
function trackGuide() {
  measureGuide()
  frame = showGuides.value ? requestAnimationFrame(trackGuide) : 0
}
watch(showGuides, (on) => {
  if (on && !frame) trackGuide()
})
onBeforeUnmount(() => cancelAnimationFrame(frame))

function onDown(e: PointerEvent) {
  const onCurrent = !!(e.target as HTMLElement).closest('[aria-pressed="true"].mi-tlcell')
  drag = { kind: onCurrent ? 'anchor' : 'pan', x: e.clientX, y: e.clientY, id: e.pointerId, captured: false }
  showGuides.value = onCurrent
}
function onMove(e: PointerEvent) {
  if (!drag || !host.value) return
  const dx = e.clientX - drag.x
  const dy = e.clientY - drag.y
  // só captura o ponteiro quando vira arraste: capturar já no clique desviaria o click dos botões dos ciclos
  if (!drag.captured) {
    if (Math.hypot(dx, dy) < 4) return
    host.value.setPointerCapture(drag.id)
    drag.captured = true
  }
  if (drag.kind === 'anchor') {
    focus.moveAnchor(dx, dy, host.value.clientWidth, host.value.clientHeight)
    drag.x = e.clientX
    drag.y = e.clientY
    return
  }
  const n = Math.round(-dx / cell.value)
  if (n !== 0) {
    pan(n)
    drag.x = e.clientX
  }
  if (dy) {
    focus.panY(dy)
    drag.y = e.clientY
  }
}
function onUp() {
  drag = null
  showGuides.value = false
}
function onWheel(e: WheelEvent) {
  if (e.ctrlKey) {
    focus.setZoom(focus.zoom.value * Math.exp(-e.deltaY * 0.002))
    measure()
    return
  }
  pan(Math.sign(e.deltaY || e.deltaX) * 4)
}

/** A célula que sai deixa o fluxo no mesmo lugar, para as vizinhas deslizarem sem salto enquanto ela some. */
function pinLeaving(el: Element) {
  const node = el as HTMLElement
  node.style.left = `${node.offsetLeft}px`
  node.style.top = `${node.offsetTop}px`
}

const PH = [
  { key: 'sense', icon: 'phase-sense-small', color: 'var(--c-b4540a)' },
  { key: 'deliberate', icon: 'phase-deliberate-small', color: 'var(--c-2a46a8)' },
  { key: 'act', icon: 'phase-act-small', color: 'var(--c-b42318)' },
] as const

/** Quantas células desenhadas ficam antes da primeira visível: a faixa recua esse tanto, e elas esperam fora da tela. */
const hiddenBefore = computed(() => cells.value.filter((c) => c < viewStart.value).length)
const stripStyle = computed(() => ({ transform: `translate(${-hiddenBefore.value * cell.value}px, ${(focus.anchor.y - 0.5) * (host.value?.clientHeight ?? 0) + focus.offsetY.value}px)` }))
const lockBtn = (on: boolean) => ({ flexShrink: 0, position: 'relative' as const, background: on ? 'var(--sel-bg)' : 'transparent' })
</script>

<template>
  <div style="position: relative; flex: 1 1 0; min-height: 0; min-width: 0; display: flex; background: var(--bg-main); overflow: hidden">
    <div ref="host" class="mi-tl-scroll" style="position: relative; flex: 1 1 0; min-width: 0; overflow: hidden; cursor: grab; display: flex; align-items: center; justify-content: flex-start; touch-action: none; user-select: none" @pointerdown="onDown" @pointermove="onMove" @pointerup="onUp" @pointercancel="onUp" @wheel.prevent="onWheel">
      <template v-if="showGuides">
        <div class="mi-tlguide" :style="{ left: `${guide.x}px`, top: 0, bottom: 0, width: '1px' }" />
        <div class="mi-tlguide" :style="{ top: `${guide.y}px`, left: 0, right: 0, height: '1px' }" />
      </template>
      <div :style="{ position: 'relative', display: 'flex', alignItems: 'flex-start', padding: '0 var(--sp-12)', ...stripStyle }">
        <div class="mi-tlline" :style="{ left: `calc(var(--sp-12) + ${cell / 2}px)`, width: `${Math.max(0, cells.length - 1) * cell}px` }" />
        <TransitionGroup tag="div" name="mi-tlc" style="position: relative; display: flex; align-items: flex-start" @before-leave="pinLeaving">
          <button v-for="c in cells" :key="c" type="button" class="mi-tlcell" :style="{ width: `${cell}px`, '--tl-delay': delayOf(c) }" :title="`Cycle ${c}`" :aria-label="`Select cycle ${c}`" :aria-pressed="c === current" @click="session.goToCycle(c)">
          <span class="mi-tlicons"><span v-for="p in PH" :key="p.key" :style="{ color: 'var(--tl-icon)', opacity: (phases.get(c) as Record<string, boolean> | undefined)?.[p.key] ? 1 : 0.18 }"><Icon :name="p.icon" /></span></span>
          <span class="mi-tldot" :style="{ borderColor: c === current ? 'var(--c-4338ca)' : 'transparent' }"><span :style="{ background: isHit(c) ? '#e5b800' : c === current ? 'var(--c-4338ca)' : 'var(--tl-on)' }" /></span>
          <span class="mi-tlnum">{{ c }}</span>
          </button>
        </TransitionGroup>
      </div>
    </div>
    <div style="position: absolute; z-index: 2; bottom: 12px; right: 12px; display: flex; align-items: flex-end; gap: var(--sp-4)">
      <div v-if="offscreen" class="mi-zoombox" role="group" aria-label="Current cycle" style="opacity: 1">
        <button type="button" class="mi-zbtn" title="Go to current cycle" aria-label="Go to current cycle" @click="follow()"><Icon name="go-current" /></button>
      </div>
      <div class="mi-zoombox" role="group" aria-label="Axes" style="opacity: 1">
        <button v-if="focus.focusMoved.value" type="button" class="mi-zbtn" title="Reset focus" aria-label="Reset focus" style="flex-shrink: 0" @click="focus.resetFocus()"><Icon name="reset-focus" /></button>
        <button type="button" class="mi-zbtn" :title="focus.lockX.value ? 'Unlock the horizontal axis' : 'Lock the horizontal axis'" :aria-label="focus.lockX.value ? 'Unlock the horizontal axis' : 'Lock the horizontal axis'" :aria-pressed="focus.lockX.value" :style="lockBtn(focus.lockX.value)" @click="focus.lockX.value = !focus.lockX.value">
          <Icon name="fit" />
          <span v-if="focus.lockX.value" class="mi-lockbadge"><Icon name="lock-badge" /></span>
        </button>
        <button type="button" class="mi-zbtn" :title="focus.lockY.value ? 'Unlock the vertical axis' : 'Lock the vertical axis'" :aria-label="focus.lockY.value ? 'Unlock the vertical axis' : 'Lock the vertical axis'" :aria-pressed="focus.lockY.value" :style="lockBtn(focus.lockY.value)" @click="focus.lockY.value = !focus.lockY.value">
          <Icon name="lock-y" />
          <span v-if="focus.lockY.value" class="mi-lockbadge"><Icon name="lock-badge" /></span>
        </button>
      </div>
    </div>
  </div>
</template>
