<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useLayoutStore } from '@/stores/layout.store'
import type { PanelKey } from '../domain/dock-tree'
import { PANELS } from '../domain/panels'
import PanelHeader from './PanelHeader.vue'

/**
 * Janelas flutuantes da visão Timeline: cada painel visível vira uma janela que se arrasta pelo cabeçalho,
 * se redimensiona pelas bordas e sobe ao topo quando clicada. Os conteúdos vêm pelos mesmos slots do DockHost.
 */
const layout = useLayoutStore()
const host = ref<HTMLElement | null>(null)
/** Largura do palco, medida depois de montar: as posições iniciais dependem dela. */
const width = ref(0)
onMounted(() => (width.value = host.value?.clientWidth ?? 0))

interface Win {
  x: number
  y: number
  w: number
  h: number
  z: number
}
const wins = reactive<Partial<Record<PanelKey, Win>>>({})
let top = 1
/** Redefinir o layout devolve cada janela à posição e ao tamanho originais. */
watch(() => layout.resetCount, () => {
  for (const key of Object.keys(wins) as PanelKey[]) delete wins[key]
})

const leaves = computed(() => layout.floating)
const tabsOf = (leaf: { k: PanelKey; t?: PanelKey[] }) => leaf.t ?? [leaf.k]
/** Tamanho inicial de cada janela (do protótipo: Intentions 360x224, Beliefs 300x395). */
const SIZE: Record<PanelKey, { w: number; h: number }> = {
  int: { w: 360, h: 224 },
  ev: { w: 360, h: 224 },
  bel: { w: 300, h: 395 },
  code: { w: 520, h: 360 },
  console: { w: 560, h: 220 },
  msgs: { w: 560, h: 260 },
  reason: { w: 700, h: 260 },
}
const EDGE = 4

/** A janela de um grupo tem uma chave estável (a primeira aba), qualquer que seja a aba ativa. */
const idOf = (leaf: { k: PanelKey; t?: PanelKey[] }): PanelKey => (leaf.t ?? [leaf.k])[0]!

/**
 * Posição inicial: as janelas ocupam o canto superior direito, a 4 px da borda e a 4 px uma da outra, a última da lista
 * mais à direita; quando não cabe mais uma, a seguinte vai para a linha de baixo.
 */
function place(key: PanelKey): Omit<Win, 'z'> {
  const order = [...leaves.value].map(idOf).reverse()
  let x = width.value - EDGE
  let y = EDGE
  let rowH = 0
  for (const k of order) {
    const size = SIZE[k]
    if (x - size.w < EDGE && x !== width.value - EDGE) {
      x = width.value - EDGE
      y += rowH + EDGE
      rowH = 0
    }
    x -= size.w
    if (k === key) return { x, y, w: size.w, h: size.h }
    x -= EDGE
    rowH = Math.max(rowH, size.h)
  }
  return { x: EDGE, y: EDGE, ...SIZE[key] }
}

function win(key: PanelKey): Win {
  return (wins[key] ??= { ...place(key), z: ++top })
}

function raise(key: PanelKey) {
  win(key).z = ++top
}

/** Ao arrastar, a janela gruda a menos de 10 px das bordas do palco e das outras janelas, sempre respeitando o vão de 4 px. */
const SNAP_RANGE = 10
function snap(key: PanelKey, w: Win, x: number, y: number): { x: number; y: number } {
  const stage = host.value
  if (!stage) return { x, y }
  const rect = (el: Element) => {
    const r = el.getBoundingClientRect()
    const s = stage.getBoundingClientRect()
    return { x: r.left - s.left, y: r.top - s.top, w: r.width, h: r.height }
  }
  const others = leaves.value
    .filter((leaf) => idOf(leaf) !== key)
    .map((leaf) => ({ x: win(idOf(leaf)).x, y: win(idOf(leaf)).y, w: win(idOf(leaf)).w, h: stage.querySelector(`[data-k="${leaf.k}"]`)?.getBoundingClientRect().height ?? win(idOf(leaf)).h }))
  const search = stage.parentElement?.querySelector('.mi-zoombox[role="search"]')
  if (search) others.push(rect(search))
  const height = stage.querySelector(`[data-k="${key}"]`)?.getBoundingClientRect().height ?? w.h
  let bx: number | null = null
  let by: number | null = null
  const near = (target: number, value: number, best: number | null) => Math.abs(value - target) < SNAP_RANGE && (best === null || Math.abs(value - target) < Math.abs(value - best))
  for (const o of others) {
    const overlapsV = y < o.y + o.h + EDGE && y + height > o.y - EDGE
    const overlapsH = x < o.x + o.w + EDGE && x + w.w > o.x - EDGE
    if (overlapsV) for (const c of [o.x + o.w + EDGE, o.x - EDGE - w.w, o.x]) if (near(c, x, bx)) bx = c
    if (overlapsH) for (const c of [o.y + o.h + EDGE, o.y - EDGE - height, o.y]) if (near(c, y, by)) by = c
  }
  for (const c of [EDGE, stage.clientWidth - EDGE - w.w]) if (near(c, x, bx)) bx = c
  for (const c of [EDGE, stage.clientHeight - EDGE - height]) if (near(c, y, by)) by = c
  const nx = bx ?? x
  const ny = by ?? y
  // a janela não sai do palco
  return { x: Math.max(EDGE, Math.min(nx, stage.clientWidth - EDGE - w.w)), y: Math.max(EDGE, Math.min(ny, stage.clientHeight - EDGE - height)) }
}

function drag(key: PanelKey, e: PointerEvent, mode: 'move' | 'se' | 'e' | 's') {
  if ((e.target as HTMLElement).closest('button, input')) return
  const w = win(key)
  raise(key)
  const origin = { px: e.clientX, py: e.clientY, x: w.x, y: w.y, w: w.w, h: w.h }
  const move = (ev: PointerEvent) => {
    const dx = ev.clientX - origin.px
    const dy = ev.clientY - origin.py
    if (mode === 'move') {
      const snapped = snap(key, w, origin.x + dx, origin.y + dy)
      w.x = snapped.x
      w.y = snapped.y
      return
    }
    if (mode !== 's') w.w = Math.max(240, origin.w + dx)
    if (mode !== 'e') w.h = Math.max(120, origin.h + dy)
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}
</script>

<template>
  <div ref="host" class="mi-grid" data-fl="1">
    <section v-for="leaf in width ? leaves : []" :key="tabsOf(leaf).join('+')" class="mi-panel mi-card" :data-k="leaf.k" :aria-label="PANELS[leaf.k].title" :style="{ position: 'absolute', left: `${win(idOf(leaf)).x}px`, top: `${win(idOf(leaf)).y}px`, width: `${win(idOf(leaf)).w}px`, height: `${win(idOf(leaf)).h}px`, zIndex: win(idOf(leaf)).z }" @pointerdown="raise(idOf(leaf))">
      <PanelHeader :active="leaf.k" :tabs="tabsOf(leaf)" draggable @activate="layout.activate" @hide="layout.hide" @pointerdown="drag(idOf(leaf), $event, 'move')">
        <template v-for="key in tabsOf(leaf)" :key="key" #[`count-${key}`]><slot :name="`count-${key}`" /></template>
        <template #[`actions-${leaf.k}`]><slot :name="`actions-${leaf.k}`" /></template>
      </PanelHeader>
      <slot :name="`body-${leaf.k}`" />
      <span class="mi-rz mi-rz-e" @pointerdown.prevent="drag(idOf(leaf), $event, 'e')" />
      <span class="mi-rz mi-rz-s" @pointerdown.prevent="drag(idOf(leaf), $event, 's')" />
      <span class="mi-rz mi-rz-se" @pointerdown.prevent="drag(idOf(leaf), $event, 'se')" />
    </section>
  </div>
</template>
