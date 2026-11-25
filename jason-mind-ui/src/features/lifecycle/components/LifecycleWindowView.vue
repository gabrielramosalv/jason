<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { describeError } from '@/shared/api/describe-error'
import EntityChip from '@/shared/components/EntityChip.vue'
import { CONCEPT_COLORS, conceptOf } from '@/shared/domain/entities'
import { numericId } from '@/shared/domain/ids'
import { Icon } from '@/shared/ui'
import type { IconName } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import type { LifecycleWindow } from '@/stores/lifecycle.store'
import { useSessionStore } from '@/stores/session.store'
import { lifecycleService } from '../services/lifecycle.service'
import { describeEntry } from '../domain/entry-text'
import type { LifecycleEntry } from '../types/lifecycle.types'

/** Uma janela de Life cycle: nascimento, mudanças e fim da entidade, cada nó com causa, origem e posição. */
const props = defineProps<{ win: LifecycleWindow }>()
const emit = defineEmits<{ close: []; drag: [e: PointerEvent, mode: 'move' | 'e' | 's' | 'se'] }>()

const session = useSessionStore()
const layout = useLayoutStore()

const entries = ref<LifecycleEntry[]>([])
const nextCursor = ref<number | string | null>(null)
const error = ref<string | null>(null)
const loading = ref(false)

async function load(more = false) {
  loading.value = true
  try {
    const res = await lifecycleService.get(props.win.agent, props.win.id, more ? nextCursor.value : null)
    if (!res.data) return
    entries.value = more ? [...entries.value, ...res.data.entries] : res.data.entries
    nextCursor.value = res.data.nextCursor
    error.value = null
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    loading.value = false
  }
}
/** Entradas do momento pedido (o passo do Reasoning de onde a janela abriu): acendem e piscam. */
const lit = (e: LifecycleEntry) => {
  const h = props.win.highlight
  return !!h && e.moment.cycle === h.cycle && e.moment.step === h.step && e.moment.iteration === h.iteration && e.moment.ordinal === h.ordinal
}
const body = ref<HTMLElement | null>(null)
const flashing = ref(false)
/** Rola a janela até a primeira entrada acesa ficar no meio da área visível. */
function centerLit() {
  const box = body.value
  const el = box?.querySelector<HTMLElement>('[data-lit="true"]')
  if (!box || !el) return
  const b = box.getBoundingClientRect()
  const r = el.getBoundingClientRect()
  box.scrollTop += r.top - b.top - (b.height - r.height) / 2
}
/** Leva a primeira entrada acesa à vista e reinicia o piscar; carrega mais páginas se ela ainda não chegou. */
async function showLit() {
  if (!props.win.highlight) return
  for (let page = 0; page < 20 && !entries.value.some(lit) && nextCursor.value !== null; page++) await load(true)
  flashing.value = false
  await nextTick()
  flashing.value = true
  centerLit()
  // o layout ainda pode se ajustar (fontes, janela recém-aberta): confere de novo logo depois
  window.setTimeout(centerLit, 250)
}
onMounted(async () => {
  await load()
  await showLit()
})
watch(() => props.win.flash, () => void showLit())

const human = (code: string | null) => (code ? code.charAt(0) + code.slice(1).toLowerCase().replace(/_/g, ' ') : null)
const icon = (e: LifecycleEntry): IconName => (e.kind === 'START' ? 'life-start' : e.kind === 'END' ? 'life-end' : e.kind === 'CHANGE' ? 'life-change' : 'life-out')
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
    <div class="mi-ph" style="cursor: grab; user-select: none; align-items: flex-start; padding: var(--sp-4)">
      <span style="color: var(--c-1f2329); display: inline-grid; place-items: center; flex-shrink: 0; width: 20px; height: 28px; margin-left: var(--sp-8)"><Icon name="life" /></span>
      <span class="mi-bchip" title="Life cycle" :style="{ background: head.bg, color: head.fg, display: 'inline-flex', alignItems: 'baseline', gap: 'var(--sp-4)', flex: '0 1 auto', minWidth: 0, marginTop: '3px', fontSize: '11.5px' }"><span style="min-width: 0; overflow-wrap: anywhere">{{ win.label }}</span><span v-if="win.type !== 'belief'" style="opacity: 0.7; flex-shrink: 0">#{{ numericId(win.id) }}</span></span>
      <button type="button" class="mi-btn mi-icon" title="Close" aria-label="Close" style="margin-left: auto; flex-shrink: 0; border-color: transparent; color: var(--c-8a8f99)" @click="emit('close')"><Icon name="close" /></button>
    </div>
    <div ref="body" style="flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: var(--sp-12); display: flex; flex-direction: column; gap: var(--sp-12)">
      <p v-if="error" class="mi-warnbox" style="border-radius: 6px">{{ error }}</p>
      <div style="display: flex; flex-direction: column">
        <template v-for="(e, i) in entries" :key="e.sequence"><div class="mi-lentry" :class="lit(e) && flashing && 'mi-lflash'" :data-lit="lit(e)" style="display: flex; gap: var(--sp-16); position: relative">
          <div style="flex-shrink: 0; width: 14px; display: flex; flex-direction: column; align-items: center"><span :title="heading(e)" role="img" :aria-label="heading(e)" style="flex-shrink: 0; width: 14px; height: 22px; display: inline-flex; align-items: center; justify-content: center; color: var(--c-8a8f99)"><Icon :name="icon(e)" /></span></div>
          <div class="mi-lbody">
            <span style="grid-column: 1 / -1; font-size: 12.8px; line-height: 22px; overflow-wrap: anywhere">{{ heading(e) }}</span>
            <div v-if="e.entity" class="mi-lrow"><span class="mi-llab">{{ e.kind === 'REMOVES' ? 'removed' : e.kind === 'MODIFIES' ? 'modified' : 'generated' }}</span><EntityChip :type="e.entity.type" :id="e.entity.id" :label="e.entity.label" :id-badge="e.entity.type === 'event' || e.entity.type === 'intention' ? numericId(e.entity.id) : null" icon /></div>
            <div v-if="e.into" class="mi-lrow"><span class="mi-llab">into</span><EntityChip :type="e.into.type" :id="e.into.id" :label="e.into.label" :id-badge="numericId(e.into.id)" icon /></div>
            <div v-if="e.cause?.by" class="mi-lrow"><span class="mi-llab">by</span><EntityChip :type="e.cause.by.type" :id="e.cause.by.id" :label="e.cause.by.label" :id-badge="e.cause.by.type === 'message' ? null : numericId(e.cause.by.id)" icon /></div>
            <div v-if="e.cause?.sender" class="mi-lrow"><span class="mi-llab">from</span><span style="display: inline-flex; align-items: center; gap: var(--sp-4); padding-left: var(--sp-4)"><Icon name="from" />{{ e.cause.sender }}</span></div>
            <div v-if="e.cause?.location?.plan" class="mi-lrow"><span class="mi-llab">at</span><span class="mi-bchip" :style="{ cursor: 'default', background: CONCEPT_COLORS.plan.bg, color: CONCEPT_COLORS.plan.fg, display: 'inline-flex', alignItems: 'center', gap: 'var(--sp-4)', maxWidth: '100%' }"><Icon name="chip-plan" /><span style="white-space: nowrap; line-height: 12px">{{ e.cause.location.plan }}</span><span v-if="e.cause.location.line" :title="`Line ${e.cause.location.line} of the plan`" style="opacity: 0.7; display: inline-flex; align-items: center; line-height: 1"><Icon name="line" /><span style="display: block; line-height: 12px">{{ e.cause.location.line }}</span></span></span></div>
            <div class="mi-lrow" style="min-height: 22px">
              <span class="mi-llab">in</span>
              <div style="display: flex; align-items: center; min-width: 0">
              <span style="display: inline-flex; align-items: center; gap: var(--sp-4); height: 22px"><Icon :name="phaseIcon(e.moment.phase)" /><span>{{ human(e.moment.step) }}</span><span style="display: inline-flex; align-items: center; gap: var(--sp-4); margin-left: var(--sp-4)"><Icon name="in-cycle" />{{ e.moment.cycle }}</span></span>
              <button type="button" class="mi-iterbtn mi-golife" title="Go to this cycle in Reasoning" aria-label="Go to this cycle in Reasoning" style="width: 22px; min-width: 22px; height: 22px; padding: 0; border: none; margin-left: var(--sp-12)" @click="goTo(e)"><Icon name="go" /></button>
              </div>
            </div>
          </div>
        </div>
        <div v-if="i < entries.length - 1" style="height: 1px; background: var(--bd-card); margin: var(--sp-12) 0" />
        </template>
      </div>
      <div style="display: flex; align-items: center; gap: var(--sp-8)">
        <button v-if="nextCursor !== null && !loading" type="button" class="mi-btn" @click="load(true)">Load more</button>
        <span v-if="loading" class="mi-muted">Loading…</span>
        <span v-if="nextCursor === null && !loading && entries.length" class="mi-muted">End of the retained history</span>
      </div>
    </div>
    <div aria-hidden="true" style="position: absolute; top: 36px; right: 0; bottom: 12px; width: 6px; cursor: ew-resize; z-index: 2" @pointerdown.stop.prevent="$emit('drag', $event, 'e')" />
    <div aria-hidden="true" style="position: absolute; left: 0; right: 12px; bottom: 0; height: 6px; cursor: ns-resize; z-index: 2" @pointerdown.stop.prevent="$emit('drag', $event, 's')" />
    <div aria-hidden="true" title="Resize" style="position: absolute; right: 0; bottom: 0; width: 12px; height: 12px; cursor: nwse-resize; z-index: 3" @pointerdown.stop.prevent="$emit('drag', $event, 'se')" />
  </div>
</template>
