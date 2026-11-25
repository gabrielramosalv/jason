<script setup lang="ts">
import { watch } from 'vue'
import { useVueFlow } from '@xyflow/vue'
import { Icon } from '@/shared/ui'

/** Zoom e enquadramento do grafo. Fica dentro do VueFlow porque `useVueFlow` só enxerga o grafo do ancestral. */
const props = defineProps<{ signature: string }>()
const { zoomIn, zoomOut, fitView, onNodesInitialized } = useVueFlow()
const fit = () => void fitView({ padding: 0.12, maxZoom: 1.1 })
onNodesInitialized(fit)
watch(() => props.signature, () => window.setTimeout(fit, 60))
</script>

<template>
  <div class="mi-gzoom">
    <div class="mi-zoombox" role="group" aria-label="Zoom" style="opacity: 1">
      <button type="button" class="mi-zbtn" title="Fit to view" aria-label="Fit to view" @click="fit"><Icon name="fit" /></button>
      <button type="button" class="mi-zbtn" title="Zoom out" aria-label="Zoom out" @click="zoomOut()"><Icon name="minimize" /></button>
      <button type="button" class="mi-zbtn" title="Zoom in" aria-label="Zoom in" @click="zoomIn()"><Icon name="zoom-in" /></button>
    </div>
  </div>
</template>
