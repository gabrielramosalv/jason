import { onBeforeUnmount, onMounted, watch } from 'vue'
import type { Ref } from 'vue'

/**
 * Executa `task` a cada `intervalMs`, sem sobrepor execuções, e pausa com a aba oculta (REQ-012).
 * Em falha, a espera cresce até 8 vezes o intervalo e volta ao normal no primeiro sucesso.
 */
export function usePolling(task: () => Promise<void>, intervalMs: number | Ref<number>, enabled?: Ref<boolean>) {
  let timer: ReturnType<typeof setTimeout> | null = null
  let stopped = false
  let failures = 0

  const interval = () => (typeof intervalMs === 'number' ? intervalMs : intervalMs.value)

  async function run() {
    timer = null
    if (stopped) return
    if (document.hidden || (enabled && !enabled.value)) {
      schedule()
      return
    }
    try {
      await task()
      failures = 0
    } catch {
      failures = Math.min(failures + 1, 3)
    }
    schedule()
  }

  function schedule() {
    if (stopped || timer !== null) return
    timer = setTimeout(run, interval() * 2 ** failures)
  }

  function trigger() {
    if (timer !== null) clearTimeout(timer)
    timer = null
    void run()
  }

  function onVisibility() {
    if (!document.hidden) trigger()
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', onVisibility)
    trigger()
  })
  onBeforeUnmount(() => {
    stopped = true
    if (timer !== null) clearTimeout(timer)
    document.removeEventListener('visibilitychange', onVisibility)
  })
  if (enabled) watch(enabled, (on) => on && trigger())

  return { trigger }
}
