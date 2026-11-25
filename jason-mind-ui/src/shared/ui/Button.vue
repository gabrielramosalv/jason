<script setup lang="ts">
withDefaults(defineProps<{ variant?: 'default' | 'primary' | 'soft'; disabled?: boolean; pressed?: boolean }>(), { variant: 'default' })
</script>

<template>
  <button type="button" class="mi-btn" :class="variant !== 'default' && variant" :disabled="disabled" :aria-pressed="pressed">
    <slot />
  </button>
</template>
