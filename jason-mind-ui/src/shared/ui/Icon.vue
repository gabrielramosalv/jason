<script setup lang="ts">
import { computed } from 'vue'
import { ICONS } from './icons'
import type { IconName } from './icons'

/** Ícone do conjunto do protótipo; sem `size`, usa o tamanho em que ele é desenhado. */
const props = defineProps<{ name: IconName; size?: number }>()

const def = computed(() => ICONS[props.name])
const px = computed(() => props.size ?? def.value.width)
</script>

<template>
  <svg :width="px" :height="px" :viewBox="def.viewBox" fill="none" aria-hidden="true" v-html="def.body" />
</template>
