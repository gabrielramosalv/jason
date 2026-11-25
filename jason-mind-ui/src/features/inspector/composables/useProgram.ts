import { shallowRef, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { usePolling } from '@/shared/composables/usePolling'
import { useSessionStore } from '@/stores/session.store'
import { inspectorService } from '../services/inspector.service'
import type { Plan, SourceFile } from '../types/state.types'

const REFRESH_MS = 5000

/** Código (`sources`) e planos (`plans`) do agente, cada um com `ETag`; o painel de código os relê a cada 5 s. */
export function useProgram() {
  const session = useSessionStore()
  const { selectedAgent } = storeToRefs(session)

  const files = shallowRef<SourceFile[]>([])
  const plans = shallowRef<Plan[]>([])

  let sourcesEtag: string | null = null
  let plansEtag: string | null = null
  let owner: string | null = null

  async function refresh() {
    const agent = selectedAgent.value
    if (!agent) return
    if (owner !== agent) {
      owner = agent
      sourcesEtag = null
      plansEtag = null
      files.value = []
      plans.value = []
    }
    const [sources, planList] = await Promise.all([
      inspectorService.sources(agent, sourcesEtag),
      inspectorService.plans(agent, plansEtag),
    ])
    if (owner !== agent) return
    if (!sources.notModified && sources.data) {
      files.value = sources.data.files
      sourcesEtag = sources.etag
    }
    if (!planList.notModified && planList.data) {
      plans.value = planList.data.plans
      plansEtag = planList.etag
    }
  }

  const { trigger } = usePolling(refresh, REFRESH_MS)
  watch(selectedAgent, () => trigger())

  return { files, plans }
}
