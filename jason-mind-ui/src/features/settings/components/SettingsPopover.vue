<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { formatBytes } from '@/shared/domain/entities'
import { Icon } from '@/shared/ui'
import { useLayoutStore } from '@/stores/layout.store'
import { useSessionStore } from '@/stores/session.store'
import { useUiStore } from '@/stores/ui.store'
import { DEFAULT_RETENTION, MAX_RETENTION, MIN_RETENTION, useSettings } from '../composables/useSettings'

/** Settings no canto: tema, horizonte do histórico, orçamento de memória, retenção por agente, métricas e poda. */
const layout = useLayoutStore()
const ui = useUiStore()
const session = useSessionStore()
const { agents } = storeToRefs(session)
const s = useSettings()

const targets = computed(() => [{ value: '*', label: 'All agents' }, ...agents.value.map((a) => ({ value: a.name, label: a.name }))])
const heapPercent = computed(() => (s.metrics.value ? Math.round((s.metrics.value.heapUsedBytes / s.metrics.value.heapMaxBytes) * 100) : 0))
const onKey = (e: KeyboardEvent) => {
  if (e.key === 'Escape') ui.popover = null
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
const liveOld = computed(() => (s.settings.value?.liveBeyondHorizon ?? []).filter((l) => l.count > 0))
</script>

<template>
  <div style="position: absolute; inset: 0; z-index: 40" @click="ui.popover = null" />
  <div role="dialog" aria-label="Settings" class="mi-dialog mi-settings" style="right: 8px; top: 6px">
    <div class="mi-ph"><h2 style="display: inline-flex; align-items: center; gap: var(--sp-4)"><Icon name="settings" />Settings</h2><button type="button" class="mi-btn mi-icon" title="Close" aria-label="Close" style="margin-left: auto; border-color: transparent; color: var(--c-8a8f99)" @click="ui.popover = null"><Icon name="close" /></button></div>
    <div class="mi-setbody">
      <div class="mi-setrow" style="height: 28px; padding: 0 0 0 var(--sp-2); gap: 0"><span>Theme</span>
        <button type="button" role="switch" :aria-checked="layout.theme === 'dark'" :title="layout.theme === 'dark' ? 'Back to light mode' : 'Turn on dark mode'" aria-label="Theme" class="mi-switch" @click="layout.toggleTheme()">
          <span class="mi-knob" :style="{ left: layout.theme === 'dark' ? '22px' : '2px' }"><Icon :name="layout.theme === 'dark' ? 'moon' : 'sun'" /></span>
        </button>
      </div>

      <p v-if="s.error.value" class="mi-warnbox" role="alert" style="border-radius: 6px">{{ s.error.value }}</p>
      <p v-if="s.notice.value" class="mi-notebox" role="status">{{ s.notice.value }}</p>

      <section class="mi-setsec">
        <div class="mi-setrow"><label for="mi-horizon">History horizon</label>
          <span style="display: inline-flex; align-items: center; gap: var(--sp-4)"><input id="mi-horizon" v-model="s.retention.value" type="number" :min="MIN_RETENTION" :max="MAX_RETENTION" class="mi-numin" :class="s.retentionInvalid.value && 'mi-bad'" style="width: 84px" /><span class="mi-muted">cycles</span></span>
        </div>
        <div class="mi-muted" style="line-height: 1.4">Older cycles are pruned: logs, messages, the Reasoning trace and finished entities. Live entities are never pruned.</div>
        <div class="mi-setrow"><label for="mi-budget">Memory budget</label>
          <span style="display: inline-flex; align-items: center; gap: var(--sp-4)"><input id="mi-budget" v-model="s.budgetMib.value" type="number" min="1" class="mi-numin" :class="s.budgetInvalid.value && 'mi-bad'" style="width: 84px" /><span class="mi-muted">MiB</span></span>
        </div>
        <div class="mi-muted" style="line-height: 1.4">{{ formatBytes(s.budgetBytes.value || 0) }} for the history of all agents; default 10% of the heap. Whichever limit comes first applies.</div>
        <div v-if="liveOld.length" role="status" class="mi-warnbox" style="border: 1px solid var(--c-d39b2a); border-radius: 6px; line-height: 1.4">
          <span>Live entities older than the horizon:</span>
          <div v-for="l in liveOld" :key="l.agent">{{ l.agent }}: {{ l.count }}<span v-if="l.oldest"> (oldest {{ l.oldest.type }} since cycle {{ l.oldest.sinceCycle }})</span></div>
        </div>
        <div v-if="s.confirmSave.value" class="mi-dangerbox">
          <span><b>This cannot be undone.</b> Lowering the horizon or the budget deletes history.</span>
          <div style="display: flex; gap: var(--sp-4)"><button type="button" class="mi-btn primary" @click="s.save(true)">Save and prune</button><button type="button" class="mi-btn" style="border: 1px solid var(--bd-idle) !important" @click="s.confirmSave.value = false">Cancel</button></div>
        </div>
        <div style="display: flex; gap: var(--sp-4)">
          <button type="button" class="mi-btn primary" :disabled="!s.dirty.value || s.retentionInvalid.value || s.budgetInvalid.value" @click="s.save()">Save</button>
          <button v-if="s.settings.value && s.settings.value.retentionCycles !== DEFAULT_RETENTION" type="button" class="mi-btn" style="border: 1px solid var(--bd-idle) !important" @click="s.retention.value = DEFAULT_RETENTION">Restore default ({{ DEFAULT_RETENTION }})</button>
        </div>
      </section>

      <section v-if="s.settings.value" class="mi-setsec">
        <strong style="font-weight: 500">Retention per agent</strong>
        <div v-for="a in s.settings.value.agents" :key="a.agent" style="display: flex; flex-direction: column; gap: var(--sp-2)">
          <div class="mi-setrow"><span>{{ a.agent }}<span v-if="a.overridden" class="mi-count" style="margin-left: var(--sp-4)">override</span></span>
            <span style="display: inline-flex; align-items: center; gap: var(--sp-4)"><input v-model="s.overrides.value[a.agent]" type="number" :min="MIN_RETENTION" :max="MAX_RETENTION" class="mi-numin" style="width: 72px" /><button type="button" class="mi-btn" style="height: 24px; border: 1px solid var(--bd-idle) !important" @click="s.saveOverride(a.agent)">Apply</button><button v-if="a.overridden" type="button" class="mi-btn mi-icon mi-x" title="Back to the default" aria-label="Back to the default" style="width: 24px; height: 24px" @click="s.resetOverride(a.agent)"><Icon name="close" /></button></span>
          </div>
          <div v-if="s.windowWarning(a.agent, a.retentionCycles)" class="mi-muted" style="color: var(--c-8a5a00); line-height: 1.4">{{ s.windowWarning(a.agent, a.retentionCycles) }}</div>
        </div>
      </section>

      <section class="mi-setsec">
        <strong style="font-weight: 500">Manual prune</strong>
        <div class="mi-muted" style="line-height: 1.4">Deletes history older than the chosen cycle. It cannot be undone, and the agent applies it at the end of its next cycle.</div>
        <div style="display: flex; align-items: center; gap: var(--sp-4); flex-wrap: wrap">
          <select v-model="s.pruneTarget.value" class="mi-numin" style="width: 110px"><option v-for="t in targets" :key="t.value" :value="t.value">{{ t.label }}</option></select>
          <input v-model="s.pruneUntil.value" type="number" min="0" placeholder="from cycle" class="mi-numin" style="width: 84px" aria-label="Keep from cycle" />
          <button type="button" class="mi-btn" :disabled="s.pruneInvalid.value || s.pruneNoop.value" style="border: 1px solid var(--c-b42318) !important; color: var(--c-b42318)" @click="s.confirmPrune.value = true">Prune…</button>
        </div>
        <div v-if="s.pruneNoop.value" class="mi-muted">That cycle is below the current horizon: nothing to prune.</div>
        <div v-if="s.confirmPrune.value" class="mi-dangerbox">
          <span><b>This cannot be undone.</b> Deleted:</span>
          <ul style="margin: 0; padding-left: var(--sp-16)"><li v-for="r in s.pruneRanges.value" :key="r.agent">{{ r.agent }}: cycles {{ r.from }} to {{ r.until - 1 }} ({{ r.count }})</li></ul>
          <div style="display: flex; gap: var(--sp-4)"><button type="button" class="mi-btn primary" @click="s.pruneNow()">Confirm prune</button><button type="button" class="mi-btn" style="border: 1px solid var(--bd-idle) !important" @click="s.confirmPrune.value = false">Cancel</button></div>
        </div>
        <div v-for="r in s.pruneStatus.value" :key="r.agent" role="status" style="display: flex; align-items: center; gap: var(--sp-8)">{{ r.agent }} up to {{ r.until }}<span class="mi-count">{{ r.applied ? 'applied' : 'requested' }}</span></div>
        <div v-if="s.pruneRequested.value && !s.pruneRequested.value.length" class="mi-muted">No request made: the agents have no closed cycle yet.</div>
      </section>

      <section v-if="s.metrics.value" class="mi-setsec">
        <strong style="font-weight: 500">Metrics</strong>
        <div class="mi-meter" role="progressbar" :aria-valuenow="heapPercent" aria-valuemin="0" aria-valuemax="100"><span :style="{ width: `${heapPercent}%` }" /></div>
        <div class="mi-muted">Heap {{ formatBytes(s.metrics.value.heapUsedBytes) }} of {{ formatBytes(s.metrics.value.heapMaxBytes) }} ({{ heapPercent }}%): the whole process, mixing Mind, Jason and the user application.</div>
        <div v-for="m in s.metrics.value.agents" :key="m.name" class="mi-setrow" style="font-variant-numeric: tabular-nums">
          <span>{{ m.name }}</span>
          <span class="mi-muted" :title="`${m.entities} entities · ${m.timelineEntries} entries · ${m.effects} effects`">{{ m.retainedCycles.toLocaleString('en-US') }} cycles · ≈ {{ formatBytes(m.estimatedHistoryBytes) }} · {{ m.meanCaptureMicros.toFixed(0) }} µs</span>
        </div>
        <div class="mi-muted" style="line-height: 1.4">≈ is an approximation. The capture time is the delay the inspector imposes on the agent per cycle (mean of the last 100).</div>
      </section>

      <div class="mi-setsec" style="border-top: 1px solid var(--bd-card); padding-top: var(--sp-12); color: var(--c-8a8f99); font-size: 11.5px">
        <div style="display: flex; justify-content: space-between"><span>Mind Inspector</span><span>v{{ session.mas?.versions.mind ?? '' }}</span></div>
        <div style="display: flex; justify-content: space-between"><span>Jason</span><span>{{ session.mas?.versions.jason ?? '' }}</span></div>
      </div>
    </div>
  </div>
</template>
