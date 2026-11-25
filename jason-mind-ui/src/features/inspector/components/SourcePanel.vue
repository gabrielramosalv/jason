<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import EntityChip from '@/shared/components/EntityChip.vue'
import { numericId } from '@/shared/domain/ids'
import { planText } from '@/features/reasoning/domain/plan-text'
import { Icon } from '@/shared/ui'
import { useInspectorStore } from '@/stores/inspector.store'
import { useLayoutStore } from '@/stores/layout.store'
import { useSearchStore } from '@/stores/search.store'
import { useUiStore } from '@/stores/ui.store'
import { useProgram } from '../composables/useProgram'
import { intentionCategory } from '../domain/intention-category'
import { TOKEN_COLOR, tokenize } from '../domain/asl-tokens'
import type { IconName } from '@/shared/ui'

/** Painel Source code: barra de intenções em execução, o código com a linha executada e o painel de variáveis. */
const inspector = useInspectorStore()
const layout = useLayoutStore()
const search = useSearchStore()
const ui = useUiStore()
const { files, plans } = useProgram()

const activeFile = ref<string | null>(null)
const varsOpen = ref(true)
const deedMenu = ref<{ line: number; items: { plan: string; index: number; content: string }[] } | null>(null)
const editor = ref<HTMLElement | null>(null)

const intentions = computed(() => inspector.state?.intentions ?? [])

/** A intenção em foco: a que o usuário escolheu ou a que executou no ciclo (o topo da pilha dela é o destaque). */
const focused = computed(() => intentions.value.find((i) => i.id === inspector.selectedIntentionId) ?? intentions.value.find((i) => i.executedInCycle) ?? intentions.value[0] ?? null)

const levels = computed(() => {
  const f = focused.value
  if (!f) return []
  return f.levels.map((lv) => {
    const im = inspector.state?.intendedMeans?.find((m) => m.id === lv.intendedMeansId)
    const plan = im ? plans.value.find((p) => p.id === im.planId) : undefined
    return { level: lv, im, plan }
  })
})

const top = computed(() => levels.value[levels.value.length - 1])
const highlight = computed(() => {
  const t = top.value
  if (!t?.im || !t.plan) return null
  const deed = t.im.currentDeed != null ? t.plan.body[t.im.currentDeed] : null
  return { path: t.plan.source.path, line: deed?.line ?? t.plan.source.line, bindings: t.im.bindings }
})

const current = computed(() => files.value.find((f) => f.path === activeFile.value) ?? files.value.find((f) => f.content.trim()) ?? files.value[0] ?? null)
const lines = computed(() => (current.value ? current.value.content.split(/\r?\n/) : []))
const shownFiles = computed(() => files.value.filter((f) => f.content.trim()))

const deedsByLine = computed(() => {
  const map = new Map<number, { plan: string; index: number; content: string }[]>()
  for (const plan of plans.value) {
    if (plan.source.path !== (current.value?.path ?? '')) continue
    plan.body.forEach((deed, index) => map.set(deed.line, [...(map.get(deed.line) ?? []), { plan: plan.id, index, content: deed.content }]))
  }
  return map
})

watch(() => highlight.value?.path, (path) => {
  if (path && files.value.some((f) => f.path === path)) activeFile.value = path
})
watch(() => [highlight.value?.line, current.value?.path], async () => {
  await nextTick()
  editor.value?.querySelector('[data-cur="true"]')?.scrollIntoView({ block: 'nearest' })
})

const isCurrent = (n: number) => highlight.value?.line === n && highlight.value?.path === current.value?.path
const ghost = computed(() => (highlight.value?.bindings.length ? '// ' + highlight.value.bindings.slice(0, 6).map((b) => `${b.name} = ${b.value}`).join(', ') : ''))

const categoryIcon: Record<string, IconName> = { executing: 'cat-executing', environment: 'cat-environment', waiting: 'cat-waiting', queue: 'cat-queue', suspended: 'cat-suspended' }

/** A lupa abre a caixa de busca já com a deed escolhida; uma deed só, sem os filtros de tipo. */
function onLens(line: number) {
  ui.searchOpen = true
  const items = deedsByLine.value.get(line) ?? []
  const [first] = items
  if (items.length === 1 && first) {
    deedMenu.value = null
    void search.runDeed(first.plan, first.index, first.content)
  } else deedMenu.value = { line, items }
}
function pick(item: { plan: string; index: number; content: string }) {
  deedMenu.value = null
  ui.searchOpen = true
  void search.runDeed(item.plan, item.index, item.content)
}

function startVarsResize(e: PointerEvent) {
  let last = e.clientX
  const move = (ev: PointerEvent) => {
    layout.varsWidth = Math.min(420, Math.max(140, layout.varsWidth - (ev.clientX - last)))
    last = ev.clientX
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
  <div v-if="shownFiles.length > 1" class="mi-filetabs">
    <button v-for="f in shownFiles" :key="f.path" type="button" class="mi-tabbtn" :aria-selected="f.path === current?.path" :style="{ background: f.path === current?.path ? 'var(--sel-bg)' : 'transparent', fontSize: '11.5px' }" @click="activeFile = f.path">{{ f.path || '(dynamic)' }}</button>
  </div>
  <div v-if="intentions.length" class="mi-runbar">
    <div class="mi-runbar-in">
      <button v-for="i in intentions" :key="i.id" type="button" class="mi-bchip" :title="`${i.state.toLowerCase().replace(/_/g, ' ')}`" :aria-pressed="i.id === focused?.id" :style="{ flexShrink: 0, cursor: 'pointer', whiteSpace: 'nowrap', display: 'inline-flex', alignItems: 'center', gap: 'var(--sp-4)', color: 'var(--c-6d3aa6)', background: 'var(--c-e4d3f7)', boxShadow: `inset 0 0 0 1px ${i.id === focused?.id ? 'var(--c-6d3aa6)' : 'var(--ov-08)'}` }" @click="inspector.selectIntention(i.id)">
        <Icon name="chip-intention" /><span>Intention</span>
        <span class="mi-pill" style="background: var(--ov-08); color: var(--c-6d3aa6)">#{{ numericId(i.id) }}</span>
        <span style="display: inline-flex" :title="intentionCategory(i)"><Icon :name="categoryIcon[intentionCategory(i)]!" /></span>
      </button>
    </div>
  </div>
  <div style="flex: 1 1 auto; min-height: 0; display: flex; gap: 0; padding: 0 var(--sp-4) var(--sp-4); position: relative">
    <div ref="editor" class="mi-editor">
      <p v-if="!current" class="mi-empty">No source code loaded</p>
      <div v-for="(text, i) in lines" :key="i" class="mi-eline" :data-ln="i + 1" :data-cur="isCurrent(i + 1)" :style="isCurrent(i + 1) ? { background: 'var(--c-e4d3f7)', boxShadow: 'inset 3px 0 0 var(--c-6d3aa6)' } : undefined">
        <button v-if="deedsByLine.has(i + 1)" type="button" class="mi-egut" title="Search the cycles where this line was current" aria-label="Search the cycles where this line was current" @click="onLens(i + 1)"><Icon name="search-small" :size="11" /></button>
        <span v-else class="mi-egut" />
        <span class="mi-enum" :style="{ color: isCurrent(i + 1) ? 'var(--c-6d3aa6)' : 'var(--c-8a8f99)' }">{{ i + 1 }}</span>
        <span class="mi-ecode"><span v-for="(tk, k) in tokenize(text)" :key="k" :style="{ color: TOKEN_COLOR[tk.kind] }">{{ tk.text }}</span></span>
        <span v-if="isCurrent(i + 1) && ghost" class="mi-ghost">{{ ghost }}</span>
        <ul v-if="deedMenu && deedMenu.line === i + 1" class="mi-pop" style="top: 22px; left: 30px; list-style: none; margin: 0">
          <li v-for="item in deedMenu.items" :key="`${item.plan}-${item.index}`"><button type="button" class="mi-row" style="height: 28px; font-family: 'JetBrains Mono', monospace" @click="pick(item)">{{ item.content }}</button></li>
        </ul>
      </div>
    </div>
    <template v-if="varsOpen">
      <div class="mi-resizer mi-vres" role="separator" aria-orientation="vertical" aria-label="Resize variables" @pointerdown.prevent="startVarsResize" />
      <div class="mi-vars" :style="{ flex: `0 0 ${layout.varsWidth}px` }">
        <div class="mi-vhead" style="display: flex; align-items: center; justify-content: space-between; gap: var(--sp-4); padding: 0 var(--sp-4) 0 var(--sp-12); margin-bottom: var(--sp-8)">
          <span style="display: inline-flex; align-items: center; gap: var(--sp-4)"><Icon name="variables" />Variables</span>
          <button type="button" class="mi-btn mi-icon mi-x" title="Minimize variables" aria-label="Minimize variables" @click="varsOpen = false"><Icon name="hide" /></button>
        </div>
        <template v-for="(lv, n) in levels" :key="lv.level.intendedMeansId">
          <div v-if="lv.im?.bindings.length" class="mi-vgroup"><EntityChip type="plan" :id="lv.plan?.id ?? lv.level.intendedMeansId" :label="lv.plan ? planText(lv.plan) : lv.level.trigger" icon /></div>
          <div v-for="b in lv.im?.bindings ?? []" :key="`${n}-${b.name}`" class="mi-vrow"><span class="mi-vk">{{ b.name }}</span><span class="mi-vv">{{ b.value }}</span></div>
        </template>
        <p v-if="!levels.some((l) => l.im?.bindings.length)" class="mi-empty">No variables bound</p>
      </div>
    </template>
    <div v-else style="position: absolute; z-index: 2; top: 6px; right: 10px; display: inline-flex; padding: var(--sp-2); background: var(--bg-card); border: 1px solid var(--c-e6e8ec); border-radius: 10px">
      <button type="button" class="mi-btn mi-icon" title="Show variables" aria-label="Show variables" style="border-color: transparent; color: var(--c-5b5f6b)" @click="varsOpen = true"><Icon name="variables-show" /></button>
    </div>
  </div>
</template>
