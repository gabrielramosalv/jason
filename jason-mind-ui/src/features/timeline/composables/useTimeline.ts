import { computed, ref, shallowRef, watch } from 'vue'
import type { Ref } from 'vue'
import { cyclesService } from '@/features/cycles/services/cycles.service'
import type { CyclePhases } from '@/features/cycles/types/cycle.types'
import { usePolling } from '@/shared/composables/usePolling'

/** Intervalos pequenos por consulta: a D-022 mede `cycles` com até 200 ciclos. */
const MAX_RANGE = 200
const LIVE_POLL_MS = 1000

interface Bounds {
  oldest: number
  newest: number
}

/**
 * Faixa de ciclos da Timeline. Guarda as fases já lidas, busca o dobro do que aparece na tela (meia tela de folga
 * de cada lado, em intervalos de até 200 ciclos) e, ao vivo, lê só o incremento a partir do último ciclo conhecido.
 */
export function useTimeline(agent: Ref<string | null>, bounds: Ref<Bounds | null>, selected: Ref<number | null>, live: Ref<boolean>, anchorX: Ref<number>, lockX: Ref<boolean>) {
  const phases = shallowRef(new Map<number, CyclePhases>())
  const viewStart = ref(0)
  const visibleCount = ref(40)
  const userPanned = ref(false)
  const error = ref<string | null>(null)
  let owner: string | null = null
  let inflight = 0
  let fetchTimer: ReturnType<typeof setTimeout> | null = null

  const viewEnd = computed(() => viewStart.value + visibleCount.value - 1)
  const visible = computed(() => {
    const b = bounds.value
    if (!b) return []
    const out: number[] = []
    for (let c = Math.max(viewStart.value, b.oldest); c <= Math.min(viewEnd.value, b.newest); c++) out.push(c)
    return out
  })

  /** O dobro do que aparece: meia tela de folga de cada lado, lida e desenhada fora da vista para a rolagem não esperar dados. */
  const buffered = computed(() => {
    const b = bounds.value
    if (!b) return []
    const margin = Math.ceil(visibleCount.value / 2)
    const out: number[] = []
    for (let c = Math.max(viewStart.value - margin, b.oldest); c <= Math.min(viewEnd.value + margin, b.newest); c++) out.push(c)
    return out
  })

  function store(list: { cycle: number; phases: CyclePhases }[]) {
    const next = new Map(phases.value)
    for (const c of list) next.set(c.cycle, c.phases)
    phases.value = next
  }

  async function fetchRange(from: number, to: number) {
    const a = agent.value
    if (!a || to < from) return
    inflight++
    try {
      for (let start = from; start <= to; start += MAX_RANGE) {
        const end = Math.min(to, start + MAX_RANGE - 1)
        const res = await cyclesService.range(a, start, end)
        if (owner !== a) return
        if (res.data) store(res.data.cycles)
      }
      error.value = null
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : 'Falha ao ler os ciclos.'
    } finally {
      inflight--
    }
  }

  function missingRanges(): [number, number][] {
    const out: [number, number][] = []
    let start: number | null = null
    for (const c of buffered.value) {
      if (!phases.value.has(c)) start ??= c
      else if (start !== null) {
        out.push([start, c - 1])
        start = null
      }
    }
    const last = buffered.value.at(-1)
    if (start !== null && last !== undefined) out.push([start, last])
    return out
  }

  function scheduleFetch() {
    if (fetchTimer) clearTimeout(fetchTimer)
    fetchTimer = setTimeout(() => {
      for (const [from, to] of missingRanges()) void fetchRange(from, to)
    }, 120)
  }

  function reset() {
    owner = agent.value
    phases.value = new Map()
    userPanned.value = false
    follow()
  }

  /** Centra a janela no ciclo selecionado (ou no mais novo, ao vivo). */
  function follow() {
    const center = selected.value ?? bounds.value?.newest
    if (center == null) return
    viewStart.value = Math.max(bounds.value?.oldest ?? 0, center - Math.floor(visibleCount.value * anchorX.value))
  }

  function pan(delta: number) {
    const b = bounds.value
    if (!b || lockX.value) return
    userPanned.value = true
    const max = Math.max(b.oldest, b.newest - visibleCount.value + 1)
    viewStart.value = Math.min(max, Math.max(b.oldest, viewStart.value + delta))
  }

  function setVisibleCount(n: number) {
    visibleCount.value = Math.max(10, Math.min(MAX_RANGE, n))
  }

  const offscreen = computed(() => selected.value != null && (selected.value < viewStart.value || selected.value > viewEnd.value))

  // ao vivo: só o incremento depois do último ciclo conhecido
  usePolling(
    async () => {
      const b = bounds.value
      if (!b || !agent.value) return
      const known = [...phases.value.keys()].reduce((m, c) => Math.max(m, c), b.oldest - 1)
      if (known < b.newest) await fetchRange(Math.max(known + 1, b.newest - MAX_RANGE + 1), b.newest)
      if (!userPanned.value) follow()
    },
    LIVE_POLL_MS,
    live,
  )

  // mover a âncora ou travar o eixo X recoloca a faixa em torno do ciclo escolhido
  watch([anchorX, lockX], () => {
    userPanned.value = false
    follow()
  })
  watch(agent, reset, { immediate: true })
  // navegar (botões, busca, clique) volta a faixa para o ciclo escolhido
  watch(selected, () => {
    userPanned.value = false
    follow()
  })
  watch([buffered, phases], scheduleFetch)
  watch(
    () => bounds.value?.oldest,
    (oldest) => {
      if (oldest == null) return
      const next = new Map(phases.value)
      for (const c of next.keys()) if (c < oldest) next.delete(c)
      phases.value = next
    },
  )

  return { phases, visible, rendered: buffered, viewStart, visibleCount, offscreen, error, pan, follow, setVisibleCount, userPanned, inflight: () => inflight }
}
