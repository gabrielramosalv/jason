<script setup lang="ts">
import { useLifecycleStore } from '@/stores/lifecycle.store'
import type { LifecycleWindow } from '@/stores/lifecycle.store'
import LifecycleWindow_ from './LifecycleWindowView.vue'

/** Hospeda as janelas de Life cycle abertas por cima da tela, com o fundo clicável para fechar todas. */
const lifecycle = useLifecycleStore()

function drag(w: LifecycleWindow, e: PointerEvent, mode: 'move' | 'e' | 's' | 'se') {
  if ((e.target as HTMLElement).closest('button')) return
  lifecycle.raise(w.key)
  const o = { px: e.clientX, py: e.clientY, x: w.x, y: w.y, w: w.w, h: w.h }
  const move = (ev: PointerEvent) => {
    const dx = ev.clientX - o.px
    const dy = ev.clientY - o.py
    if (mode === 'move') {
      w.x = Math.max(0, o.x + dx)
      w.y = Math.max(0, o.y + dy)
      return
    }
    if (mode !== 's') w.w = Math.max(280, o.w + dx)
    if (mode !== 'e') w.h = Math.max(160, o.h + dy)
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
  <template v-if="lifecycle.windows.length">
    <div style="position: fixed; inset: 0; z-index: 36" @click="lifecycle.closeAll()" />
    <LifecycleWindow_ v-for="w in lifecycle.windows" :key="w.key" :win="w" @close="lifecycle.close(w.key)" @drag="(e, mode) => drag(w, e, mode)" />
  </template>
</template>
