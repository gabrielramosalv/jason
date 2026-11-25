import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  DEFAULT_HIDDEN,
  DEFAULT_PLACE,
  PANEL_KEYS,
  defaultTree,
  insertLeaf,
  moveKeys,
  leavesOf,
  removeLeaf,
  resizeSplit,
  setActive,
} from '@/features/layout/domain/dock-tree'
import type { DockNode, DropSide, Leaf, PanelKey } from '@/features/layout/domain/dock-tree'

export type ViewMode = 'cycle' | 'timeline'
export type Theme = 'light' | 'dark'

const LAYOUT_KEY = 'mind-inspector:agent-layout:v7'
const THEME_KEY = 'mind-inspector:theme'

interface Persisted {
  tree: DockNode | null
  floating: Leaf[]
  agentsOpen: boolean
  agentsWidth: number
  varsWidth: number
}

/** Na Timeline começam à vista só Intentions (com Events em aba) e Beliefs, como no protótipo. */
const floatingDefault = (): Leaf[] => [{ k: 'int', t: ['int', 'ev'] }, { k: 'bel' }]

function initial(): Persisted {
  let tree = defaultTree()
  for (const key of DEFAULT_HIDDEN) tree = removeLeaf(tree, key) ?? tree
  return { tree, floating: floatingDefault(), agentsOpen: true, agentsWidth: 200, varsWidth: 200 }
}

function load(): Persisted {
  try {
    const raw = localStorage.getItem(LAYOUT_KEY)
    if (raw) return { ...initial(), ...(JSON.parse(raw) as Partial<Persisted>) }
  } catch {
    /* layout corrompido: volta ao padrão */
  }
  return initial()
}

/** Layout do inspetor: árvore de painéis, barra de agentes, tema e visão (ciclo ou Timeline). Salvo no navegador. */
export const useLayoutStore = defineStore('layout', () => {
  const saved = load()
  const tree = ref<DockNode | null>(saved.tree)
  const floating = ref<Leaf[]>(saved.floating)
  const agentsOpen = ref(saved.agentsOpen)
  const agentsWidth = ref(saved.agentsWidth)
  const varsWidth = ref(saved.varsWidth)
  const view = ref<ViewMode>('cycle')
  /** Sobe a cada reset: quem guarda posições locais (as janelas flutuantes) descarta as suas. */
  const resetCount = ref(0)
  const theme = ref<Theme>((localStorage.getItem(THEME_KEY) as Theme | null) ?? 'dark')

  watch([tree, floating, agentsOpen, agentsWidth, varsWidth], () => localStorage.setItem(LAYOUT_KEY, JSON.stringify({ tree: tree.value, floating: floating.value, agentsOpen: agentsOpen.value, agentsWidth: agentsWidth.value, varsWidth: varsWidth.value })), { deep: true })
  watch(theme, (value) => localStorage.setItem(THEME_KEY, value))

  const timeline = computed(() => view.value === 'timeline')

  /** Painéis à vista na visão atual: os ancorados (ciclo) ou as janelas flutuantes (Timeline). */
  const visible = computed(() => {
    const on = new Set(timeline.value ? floating.value.flatMap((l) => l.t ?? [l.k]) : leavesOf(tree.value))
    return Object.fromEntries(PANEL_KEYS.map((k) => [k, on.has(k)])) as Record<PanelKey, boolean>
  })

  function show(key: PanelKey) {
    if (visible.value[key]) return
    if (timeline.value) {
      floating.value = [...floating.value, { k: key }]
      return
    }
    const place = DEFAULT_PLACE[key]
    tree.value = insertLeaf(tree.value, key, place.sib ? { sib: place.sib, side: place.side ?? 'right' } : { root: place.root ?? 'bottom' })
  }

  function hide(key: PanelKey) {
    if (!timeline.value) {
      tree.value = removeLeaf(tree.value, key)
      return
    }
    floating.value = floating.value.flatMap((l) => {
      const t = (l.t ?? [l.k]).filter((x) => x !== key)
      if (!t.length) return []
      return [t.length === 1 ? { k: t[0]! } : { k: t.includes(l.k) ? l.k : t[0]!, t }]
    })
  }

  function toggle(key: PanelKey) {
    if (visible.value[key]) hide(key)
    else show(key)
  }

  function activate(key: PanelKey) {
    if (!timeline.value) {
      if (tree.value) tree.value = setActive(tree.value, key)
      return
    }
    floating.value = floating.value.map((l) => ((l.t ?? [l.k]).includes(key) && l.t ? { ...l, k: key } : l))
  }

  /** Move uma aba ou um contêiner para junto de `sib` (gesto de arrastar do dock). */
  function move(keys: PanelKey[], sib: PanelKey, side: DropSide) {
    if (!timeline.value) tree.value = moveKeys(tree.value, keys, sib, side)
  }

  function resize(path: number[], index: number, deltaFraction: number) {
    if (tree.value) tree.value = resizeSplit(tree.value, path, index, deltaFraction)
  }

  function reset() {
    const fresh = initial()
    tree.value = fresh.tree
    floating.value = fresh.floating
    agentsWidth.value = fresh.agentsWidth
    varsWidth.value = fresh.varsWidth
    resetCount.value++
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  return { tree, floating, agentsOpen, agentsWidth, varsWidth, view, resetCount, theme, visible, show, hide, toggle, activate, move, resize, reset, toggleTheme }
})
