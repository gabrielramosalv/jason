<script setup lang="ts">
import Icon from './Icon.vue'
import type { IconName } from './icons'

/** Botão quadrado de 28 px só com ícone (a base de quase toda a barra de ferramentas). */
withDefaults(
  defineProps<{ icon: IconName; label: string; selected?: boolean; pressed?: boolean; muted?: boolean; dot?: boolean; size?: number; strong?: boolean }>(),
  { selected: undefined, pressed: undefined, muted: false, dot: false, strong: false },
)
</script>

<template>
  <button
    type="button"
    class="mi-btn mi-icon"
    :class="{ 'mi-x': muted }"
    :title="label"
    :aria-label="label"
    :aria-selected="selected"
    :aria-pressed="pressed"
    :style="{ position: 'relative', color: selected || strong ? 'var(--c-1f2329)' : 'var(--c-5b5f6b)', ...(selected ? { background: 'var(--sel-hover)' } : {}) }"
  >
    <Icon :name="icon" :size="size" />
    <span v-if="dot" class="mi-newdot" aria-hidden="true" title="New" />
  </button>
</template>
