import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { describeError } from '@/shared/api/describe-error'
import { usePolling } from '@/shared/composables/usePolling'
import { useSessionStore } from '@/stores/session.store'
import { settingsService } from '../services/settings.service'
import type { Metrics, PruneRequest, Settings } from '../types/settings.types'

export const MIN_RETENTION = 1
export const MAX_RETENTION = 10_000_000
export const DEFAULT_RETENTION = 10_000
const MIB = 1024 * 1024
const METRICS_POLL_MS = 5000

/**
 * Settings do inspetor: retenção do MAS e por agente, orçamento de memória, poda manual e o monitoramento leve
 * (métricas só enquanto a tela de Settings está aberta, D-022). As escritas mandam JSON e tratam 403 e 415 [D-155].
 */
export function useSettings() {
  const session = useSessionStore()
  const { agents } = storeToRefs(session)

  const settings = ref<Settings | null>(null)
  const metrics = ref<Metrics | null>(null)
  const error = ref<string | null>(null)
  const notice = ref<string | null>(null)

  const retention = ref<number | string>('')
  const budgetMib = ref<number | string>('')
  const overrides = ref<Record<string, number | string>>({})
  const confirmSave = ref(false)

  function apply(s: Settings) {
    settings.value = s
    retention.value = s.retentionCycles
    budgetMib.value = Math.round((s.memoryBudgetBytes / MIB) * 100) / 100
    overrides.value = Object.fromEntries(s.agents.map((a) => [a.agent, a.retentionCycles]))
  }

  async function load() {
    try {
      const res = await settingsService.get()
      if (res.data) apply(res.data)
      error.value = null
    } catch (cause) {
      error.value = describeError(cause)
    }
  }
  onMounted(load)

  usePolling(async () => {
    const res = await settingsService.metrics()
    metrics.value = res.data
  }, METRICS_POLL_MS)

  const retentionNumber = computed(() => Number(retention.value))
  const budgetBytes = computed(() => Math.round(Number(budgetMib.value) * MIB))
  const retentionInvalid = computed(() => !Number.isInteger(retentionNumber.value) || retentionNumber.value < MIN_RETENTION || retentionNumber.value > MAX_RETENTION)
  const budgetInvalid = computed(() => !Number.isFinite(budgetBytes.value) || budgetBytes.value < MIB)
  const dirty = computed(() => !!settings.value && (retentionNumber.value !== settings.value.retentionCycles || budgetBytes.value !== settings.value.memoryBudgetBytes))
  /** Reduzir a retenção ou o orçamento apaga histórico sem desfazer, então a UI confirma antes. */
  const reduces = computed(() => !!settings.value && (retentionNumber.value < settings.value.retentionCycles || budgetBytes.value < settings.value.memoryBudgetBytes))

  async function save(confirmed = false) {
    if (!settings.value || retentionInvalid.value || budgetInvalid.value || !dirty.value) return
    if (reduces.value && !confirmed) {
      confirmSave.value = true
      return
    }
    confirmSave.value = false
    try {
      const body: { retentionCycles?: number; memoryBudgetBytes?: number } = {}
      if (retentionNumber.value !== settings.value.retentionCycles) body.retentionCycles = retentionNumber.value
      if (budgetBytes.value !== settings.value.memoryBudgetBytes) body.memoryBudgetBytes = budgetBytes.value
      const res = await settingsService.update(body)
      if (res.data) apply(res.data)
      notice.value = 'Saved. If the window shrank, the prune runs at the end of the next cycle of each agent; a stopped agent only applies it when it runs again. Values return to the default when the MAS restarts.'
      error.value = null
    } catch (cause) {
      error.value = describeError(cause)
    }
  }

  async function saveOverride(agent: string) {
    const n = Number(overrides.value[agent])
    if (!Number.isInteger(n) || n < MIN_RETENTION || n > MAX_RETENTION) {
      error.value = `Retention of ${agent}: use an integer from ${MIN_RETENTION} to ${MAX_RETENTION.toLocaleString('en-US')}.`
      return
    }
    try {
      const res = await settingsService.updateAgent(agent, n)
      if (res.data) apply(res.data)
      error.value = null
    } catch (cause) {
      error.value = describeError(cause)
    }
  }

  async function resetOverride(agent: string) {
    try {
      const res = await settingsService.resetAgent(agent)
      if (res.data) apply(res.data)
      error.value = null
    } catch (cause) {
      error.value = describeError(cause)
    }
  }

  /** X ciclos retidos de Y configurados, sem atribuir a causa (orçamento de memória ou poda manual). */
  function windowWarning(agent: string, configured: number): string | null {
    const a = agents.value.find((x) => x.name === agent)
    const kept = metrics.value?.agents.find((m) => m.name === agent)?.retainedCycles
    if (!a || kept == null || a.newestCycle == null) return null
    return kept < configured && a.newestCycle + 1 >= configured ? `${kept} cycles retained of ${configured} configured: the window shrank because of the memory budget or a manual prune.` : null
  }

  // poda manual [D-149]
  const pruneTarget = ref('*')
  const pruneUntil = ref<number | string>('')
  const confirmPrune = ref(false)
  const pruneRequested = ref<PruneRequest[] | null>(null)
  const untilNumber = computed(() => Number(pruneUntil.value))

  const pruneRanges = computed(() => {
    const list = pruneTarget.value === '*' ? agents.value : agents.value.filter((a) => a.name === pruneTarget.value)
    return list
      .filter((a) => a.oldestCycle != null && a.newestCycle != null)
      .map((a) => {
        const until = Math.min(untilNumber.value, a.newestCycle!)
        return { agent: a.name, from: a.oldestCycle!, until, count: Math.max(0, until - a.oldestCycle!) }
      })
  })
  const pruneInvalid = computed(() => !Number.isInteger(untilNumber.value) || untilNumber.value < 0)
  const pruneNoop = computed(() => !pruneInvalid.value && pruneRanges.value.every((r) => r.count === 0))

  async function pruneNow() {
    confirmPrune.value = false
    try {
      const res = pruneTarget.value === '*' ? await settingsService.pruneAll(untilNumber.value) : await settingsService.pruneAgent(pruneTarget.value, untilNumber.value)
      pruneRequested.value = res.data?.requests ?? []
      error.value = null
    } catch (cause) {
      error.value = describeError(cause)
    }
  }

  /** A poda está aplicada quando o `oldestCycle` da lista de agentes chega ao `until` pedido. */
  const pruneStatus = computed(() =>
    (pruneRequested.value ?? []).map((r) => {
      const a = agents.value.find((x) => x.name === r.agent)
      return { ...r, applied: a?.oldestCycle != null && a.oldestCycle >= r.until }
    }),
  )

  return {
    settings, metrics, error, notice, retention, budgetMib, overrides, confirmSave, retentionInvalid, budgetInvalid, dirty, budgetBytes,
    save, saveOverride, resetOverride, windowWarning,
    pruneTarget, pruneUntil, confirmPrune, pruneRequested, pruneRanges, pruneInvalid, pruneNoop, pruneNow, pruneStatus,
  }
}
