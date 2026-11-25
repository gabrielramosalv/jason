<script setup lang="ts">
const props = defineProps<{ direction: 'horizontal' | 'vertical'; label: string }>()
const emit = defineEmits<{ resize: [delta: number] }>()

let last = 0
function onDown(e: PointerEvent) {
  last = props.direction === 'vertical' ? e.clientX : e.clientY
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
}
function onMove(e: PointerEvent) {
  if (!(e.currentTarget as HTMLElement).hasPointerCapture(e.pointerId)) return
  const now = props.direction === 'vertical' ? e.clientX : e.clientY
  emit('resize', now - last)
  last = now
}
</script>

<template>
  <div
    role="separator"
    :aria-label="label"
    :aria-orientation="direction"
    tabindex="0"
    class="shrink-0 touch-none bg-transparent hover:bg-accent/40"
    :class="direction === 'vertical' ? 'w-1 cursor-col-resize' : 'h-1 cursor-row-resize'"
    @pointerdown="onDown"
    @pointermove="onMove"
  />
</template>
