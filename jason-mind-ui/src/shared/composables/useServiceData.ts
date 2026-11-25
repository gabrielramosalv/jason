import { onMounted, shallowRef } from 'vue'
import type { ShallowRef } from 'vue'

export interface ServiceDataState<T> {
  data: ShallowRef<T | null>
  error: ShallowRef<Error | null>
  isLoading: ShallowRef<boolean>
  load: () => Promise<void>
}

export function useServiceData<T>(loader: () => Promise<T>, immediate = true): ServiceDataState<T> {
  const data = shallowRef<T | null>(null)
  const error = shallowRef<Error | null>(null)
  const isLoading = shallowRef(immediate)

  async function load() {
    isLoading.value = true
    error.value = null
    try {
      data.value = await loader()
    } catch (cause) {
      error.value = cause instanceof Error ? cause : new Error('Não foi possível carregar os dados.')
    } finally {
      isLoading.value = false
    }
  }

  if (immediate) onMounted(load)

  return { data, error, isLoading, load }
}
