<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { PanelKey } from '@/features/layout/domain/dock-tree'
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { useUiStore } from '@/stores/ui.store'
import { TOUR_STEPS } from '../domain/tour-steps'
import type { TourAction } from '../domain/tour-steps'

/** Tour guiado: escurece a tela, destaca um elemento por vez e explica numa janela ao lado (← →, Esc). */
const ui = useUiStore()
const layout = useLayoutStore()

const index = computed(() => ui.tour ?? 0)
const step = computed(() => TOUR_STEPS[index.value]!)
const rect = ref<{ left: number; top: number; width: number; height: number } | null>(null)
const card = ref<HTMLElement | null>(null)
const cardPos = ref({ left: 20, top: 20 })

function run(action: TourAction) {
  const [kind, arg] = action.split(':') as [string, string]
  if (kind === 'view') layout.view = arg as 'cycle' | 'timeline'
  else if (kind === 'agents') layout.agentsOpen = true
  else if (kind === 'search') ui.searchOpen = arg === 'open'
  else if (kind === 'show') layout.show(arg as PanelKey)
  else if (kind === 'tab') layout.activate(arg as PanelKey)
}

function union(selector: string): { left: number; top: number; right: number; bottom: number } | null {
  const els = [...document.querySelectorAll<HTMLElement>(selector)].filter((e) => e.offsetParent !== null || getComputedStyle(e).position === 'fixed')
  if (!els.length) return null
  let l = Infinity, t = Infinity, r = -Infinity, b = -Infinity
  for (const e of els) {
    const x = e.getBoundingClientRect()
    l = Math.min(l, x.left); t = Math.min(t, x.top); r = Math.max(r, x.right); b = Math.max(b, x.bottom)
  }
  return { left: l, top: t, right: r, bottom: b }
}

async function place() {
  const s = step.value
  s.ensure.forEach(run)
  await nextTick()
  await new Promise((resolve) => setTimeout(resolve, 120))
  const box = s.target ? union(s.target) : null
  rect.value = box ? { left: box.left - 4, top: box.top - 4, width: box.right - box.left + 8, height: box.bottom - box.top + 8 } : null
  const w = 320
  const h = card.value?.offsetHeight ?? 200
  const vw = window.innerWidth
  const vh = window.innerHeight
  if (!box || s.side === 'center') cardPos.value = { left: (vw - w) / 2, top: (vh - h) / 2 }
  else {
    const clamp = (n: number, min: number, max: number) => Math.min(Math.max(n, min), max)
    const pos = {
      bottom: { left: box.left, top: box.bottom + 14 },
      top: { left: box.left, top: box.top - h - 14 },
      right: { left: box.right + 14, top: box.top },
      left: { left: box.left - w - 14, top: box.top },
    }[s.side]
    cardPos.value = { left: clamp(pos.left, 12, vw - w - 12), top: clamp(pos.top, 12, vh - h - 12) }
  }
}

function go(i: number) {
  if (i < 0) return
  if (i >= TOUR_STEPS.length) close()
  else ui.tour = i
}
function close() {
  ui.tour = null
}
function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
  else if (e.key === 'ArrowRight' || e.key === ' ' || e.key === 'Enter') go(index.value + 1)
  else if (e.key === 'ArrowLeft') go(index.value - 1)
  else return
  e.preventDefault()
  e.stopPropagation()
}

watch(index, place)
onMounted(() => {
  window.addEventListener('keydown', onKey, true)
  window.addEventListener('resize', place)
  void place()
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey, true)
  window.removeEventListener('resize', place)
})
</script>

<template>
  <div data-tut="blocker" style="position: fixed; inset: 0; z-index: 1000; cursor: default" />
  <div
    data-tut="spot"
    aria-hidden="true"
    class="mi-spot"
    :style="rect ? { left: `${rect.left}px`, top: `${rect.top}px`, width: `${rect.width}px`, height: `${rect.height}px` } : { left: '50%', top: '50%', width: '0px', height: '0px' }"
  />
  <div ref="card" data-tut="card" role="dialog" aria-label="Tutorial" class="mi-tutcard" :style="{ left: `${cardPos.left}px`, top: `${cardPos.top}px` }">
    <div style="display: flex; align-items: center; gap: var(--sp-8)">
      <span class="mi-label" style="flex: 1; color: #3b82f6">{{ step.stage }}</span>
      <button type="button" class="mi-btn mi-icon" title="Close tutorial" aria-label="Close tutorial" style="border-color: transparent; width: 24px; height: 24px" @click="close"><Icon name="close-2" /></button>
    </div>
    <div style="font-size: 14px; font-weight: 600">{{ step.title }}</div>
    <div style="font-size: 12.5px; line-height: 1.55; color: var(--c-5b5f6b)">{{ step.text }}</div>
    <div style="height: 3px; border-radius: 2px; background: var(--ov-08); overflow: hidden; margin-top: var(--sp-4)"><div :style="{ height: '100%', width: `${((index + 1) / TOUR_STEPS.length) * 100}%`, background: '#3b82f6', transition: 'width .28s ease' }" /></div>
    <div style="display: flex; align-items: center; gap: var(--sp-8)">
      <span style="flex: 1; font-size: 11.5px; color: var(--c-8a8f99); font-variant-numeric: tabular-nums">{{ index + 1 }} / {{ TOUR_STEPS.length }}</span>
      <button type="button" class="mi-btn" :style="{ opacity: index === 0 ? 0.4 : 1 }" @click="go(index - 1)">Back</button>
      <button type="button" class="mi-btn" style="background: #3b82f6; border-color: #3b82f6; color: #fff" @click="go(index + 1)">{{ index === TOUR_STEPS.length - 1 ? 'Done' : 'Next' }}</button>
    </div>
  </div>
</template>
