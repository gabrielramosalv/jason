<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { formatTime } from '@/shared/domain/entities'
import { CONCEPT_COLORS } from '@/shared/domain/entities'
import type { ConceptKind } from '@/shared/domain/entities'
import { useStickyScroll } from '@/shared/composables/useStickyScroll'
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { usePanelFiltersStore } from '@/stores/panel-filters.store'
import { useSessionStore } from '@/stores/session.store'
import { useMessages } from '../composables/useMessages'
import type { MasMessage } from '../types/message.types'

/** Corpo do painel Messages: contatos à esquerda e a conversa (chat) ou as listas Received e Sent à direita. */
const session = useSessionStore()
const layout = useLayoutStore()
const filters = usePanelFiltersStore()
const { selectedAgent, isLive, shown } = storeToRefs(session)

const me = computed(() => selectedAgent.value)
const until = computed(() => shown.value?.sequenceEnd ?? null)
const enabled = computed(() => layout.visible.msgs)
const fetchAgents = computed(() => (me.value ? [me.value] : []))

const { rows, counts, error, loading, hasOlder, fillGap, loadOlder } = useMessages({ agents: fetchAgents, until, live: isLive, enabled })

const all = computed<MasMessage[]>(() => rows.value.flatMap((r) => (r.kind === 'message' ? [r.message] : [])))
const peers = computed(() => [...new Set(all.value.flatMap((m) => [m.sender, m.receiver]).filter((n) => n !== me.value))])
const sentTo = (peer: string) => all.value.filter((m) => m.sender === me.value && m.receiver === peer).length
const recvFrom = (peer: string) => all.value.filter((m) => m.receiver === me.value && m.sender === peer).length

const conversation = computed(() => {
  const q = filters.messages.q.trim().toLowerCase()
  const peer = filters.messages.contact
  return all.value.filter((m) => {
    if (peer && peer !== '*' && m.sender !== peer && m.receiver !== peer) return false
    if (filters.messages.agents.length && !filters.messages.agents.some((a) => a === m.sender || a === m.receiver)) return false
    return !q || `${m.performative} ${m.content} ${m.sender} ${m.receiver}`.toLowerCase().includes(q)
  })
})
const scroller = ref<HTMLElement | null>(null)
const { atBottom, pending, onScroll, toBottom } = useStickyScroll(scroller, computed(() => conversation.value.map((m) => m.id)), isLive)
const received = computed(() => conversation.value.filter((m) => m.receiver === me.value))
const sent = computed(() => conversation.value.filter((m) => m.sender === me.value))
const title = computed(() => (filters.messages.contact === '*' || !filters.messages.contact ? 'All messages' : filters.messages.contact))
const isAll = computed(() => filters.messages.contact === '*' || !filters.messages.contact)

const isOut = (m: MasMessage) => m.sender === me.value
const bubble = (m: MasMessage) => ({ alignSelf: isOut(m) ? 'flex-end' : 'flex-start', background: isOut(m) ? 'var(--msg-out-bg)' : 'var(--bg-codebox)', borderColor: isOut(m) ? 'var(--msg-out-bd)' : 'var(--bg-codebox)', borderRadius: isOut(m) ? '12px 12px 3px 12px' : '12px 12px 12px 3px' })

/** A cor do chip muda com o que a mensagem faz: informar (tell) vira crença, pedir um objetivo (achieve) vira intenção. */
const PERFORMATIVE_KIND: Record<string, ConceptKind> = { tell: 'belief', untell: 'belief', tellhow: 'plan', untellhow: 'plan', achieve: 'intention', unachieve: 'intention' }
const chipColors = (m: MasMessage) => CONCEPT_COLORS[PERFORMATIVE_KIND[m.performative.toLowerCase()] ?? 'message']

function goTo(m: MasMessage) {
  if (!m.sent) return
  if (m.sender !== session.selectedAgent) session.selectAgent(m.sender)
  session.goToCycle(m.sent.cycle)
}
void counts
</script>

<template>
  <div style="flex: 1 1 0; min-height: 0; display: flex; gap: var(--sp-4); padding: 0 var(--sp-4) var(--sp-4)">
    <nav aria-label="Contacts" class="mi-contacts">
      <button type="button" class="mi-row mi-ct" :aria-selected="isAll" :style="{ ...(isAll ? { background: 'var(--sel-code)' } : {}), padding: 'var(--sp-8)', gap: 'var(--sp-4)' }" @click="filters.messages.contact = '*'"><strong style="font-weight: 500; flex: 1 1 auto">All</strong></button>
      <button v-for="p in peers" :key="p" type="button" class="mi-row mi-ct" :aria-selected="filters.messages.contact === p" :style="{ ...(filters.messages.contact === p ? { background: 'var(--sel-code)' } : {}), padding: 'var(--sp-8)', gap: 'var(--sp-4)' }" @click="filters.messages.contact = p">
        <strong style="font-weight: 500; flex: 1 1 auto; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ p }}</strong>
        <span style="display: inline-flex; flex-shrink: 0; gap: var(--sp-4); color: var(--c-8a8f99); font-size: 11px">
          <span title="Sent" style="display: inline-flex; align-items: center; gap: var(--sp-4)"><Icon name="sent" />{{ sentTo(p) }}</span>
          <span title="Received" style="display: inline-flex; align-items: center; gap: var(--sp-4)"><Icon name="received" />{{ recvFrom(p) }}</span>
        </span>
      </button>
    </nav>
    <div style="flex: 1 1 0; min-width: 0; display: flex; flex-direction: column">
      <div style="display: flex; align-items: center; justify-content: space-between; gap: var(--sp-8); min-height: 31px; padding: 0 var(--sp-16); flex-wrap: wrap; background: var(--bg-card)"><strong style="font-weight: 500; font-size: 13px">{{ title }}</strong></div>
      <p v-if="error" class="mi-warnbox">{{ error }}</p>
      <div v-if="filters.messages.mode === 'time'" style="position: relative; flex: 1 1 0; min-height: 0; display: flex; flex-direction: column">
      <div ref="scroller" style="flex: 1 1 0; min-height: 0; overflow-y: auto; display: flex; flex-direction: column" @scroll.passive="onScroll">
        <div style="display: flex; flex-direction: column; justify-content: flex-end; flex: 1 0 auto; gap: var(--sp-8); padding: var(--sp-12) var(--sp-16)">
          <div v-if="hasOlder" style="text-align: center"><button type="button" class="mi-btn" @click="loadOlder">Earlier messages</button></div>
          <div v-if="!conversation.length && !loading" style="padding: var(--sp-24) 0; text-align: center; color: var(--c-8a8f99)">No messages exchanged</div>
          <template v-for="row in rows" :key="row.kind === 'gap' ? row.id : row.message.id">
            <div v-if="row.kind === 'gap'" class="mi-gap"><span>{{ row.omitted }} messages omitted: the flow outran the screen</span><button type="button" class="mi-btn" @click="fillGap(row)">Load</button></div>
            <div v-else-if="conversation.includes(row.message)" class="mi-bubble" :style="bubble(row.message)">
              <span style="color: var(--c-8a8f99); font-size: 11.5px">
                <template v-if="isAll"><span style="color: var(--c-1f2329); font-weight: 500">{{ row.message.sender }}</span> {{ row.message.performative }} <span style="color: var(--c-1f2329); font-weight: 500">{{ row.message.receiver }}</span></template>
                <template v-else>{{ row.message.performative }}</template>
              </span>
              <div style="display: flex; align-items: flex-end; gap: var(--sp-12)">
                <span class="mi-bchip" :style="{ background: chipColors(row.message).bg, color: chipColors(row.message).fg, border: 'none' }">{{ row.message.content }}</span>
                <span style="margin-left: auto; color: var(--c-8a8f99); font-size: 11.5px; white-space: nowrap; display: inline-flex; align-items: center; gap: var(--sp-4)">
                  <span v-if="row.message.sent">{{ formatTime(row.message.sent.timeMilliseconds) }}</span>
                  <button v-if="row.message.sent" type="button" class="mi-cyclink" :title="`Go to cycle ${row.message.sent.cycle} of ${row.message.sender}`" @click="goTo(row.message)"><Icon name="cycle" />{{ row.message.sent.cycle }}</button>
                </span>
              </div>
            </div>
          </template>
        </div>
      </div>
      <button v-if="!atBottom && pending > 0" type="button" class="mi-tobottom" :title="pending ? `${pending} new messages: go to the latest` : 'Go to the latest'" aria-label="Go to the latest" @click="toBottom()"><span>{{ pending }} new</span><Icon name="chevron-down" /></button>
      </div>
      <div v-else style="flex: 1 1 0; min-height: 0; overflow-y: auto">
        <div style="display: flex; flex-direction: column; gap: var(--sp-8); padding: var(--sp-12) var(--sp-16)">
          <div v-if="!conversation.length" style="padding: var(--sp-24) 0; text-align: center; color: var(--c-8a8f99)">No messages exchanged</div>
          <template v-for="g in [{ key: 'recvOpen', label: 'Received', list: received, c1: 'recv-chevron', c2: 'recv-icon' }, { key: 'sentOpen', label: 'Sent', list: sent, c1: 'sent-chevron', c2: 'sent-icon' }] as const" :key="g.key">
            <button type="button" class="mi-cyclink" :aria-expanded="filters.messages[g.key]" style="margin: 0; padding: var(--sp-2) var(--sp-4); gap: var(--sp-4); align-self: flex-start; color: var(--c-5b5f6b); font-size: 12px; font-weight: 500" @click="filters.messages[g.key] = !filters.messages[g.key]"><Icon :name="g.c1" /><Icon :name="g.c2" />{{ g.label }}</button>
            <div v-if="filters.messages[g.key]" style="display: flex; flex-wrap: wrap; gap: var(--sp-4); align-items: flex-start">
              <div v-for="m in g.list" :key="m.id" class="mi-bubble" style="align-self: flex-start; max-width: 100%; border-radius: 12px; background: var(--bg-card); border-color: var(--bd-card)">
                <span style="color: var(--c-8a8f99); font-size: 11.5px"><template v-if="isAll"><b style="color: var(--c-1f2329); font-weight: 500">{{ m.sender }}</b> {{ m.performative }} <b style="color: var(--c-1f2329); font-weight: 500">{{ m.receiver }}</b></template><template v-else>{{ m.performative }}</template></span>
                <span class="mi-bchip" :style="{ background: chipColors(m).bg, color: chipColors(m).fg, border: 'none' }">{{ m.content }}</span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
