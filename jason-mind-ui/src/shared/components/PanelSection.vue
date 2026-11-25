<script setup lang="ts">
import { Icon } from '@/shared/ui'
import type { IconName } from '@/shared/ui'

/** Seção recolhível de um painel: botão com chevron, ícone, rótulo e contagem, e o conteúdo recuado. */
defineProps<{ label: string; icon?: IconName; count?: number | string | null; open: boolean; divider?: boolean; chevron?: IconName }>()
const emit = defineEmits<{ toggle: [] }>()
</script>

<template>
  <div v-if="divider" class="mi-divider" />
  <button type="button" class="mi-sechead" :aria-expanded="open" @click="emit('toggle')">
    <span :style="{ display: 'inline-grid', transform: open ? 'none' : 'rotate(-90deg)' }"><Icon :name="chevron ?? 'section-chevron'" /></span>
    <Icon v-if="icon" :name="icon" />
    <span>{{ label }}</span>
    <span v-if="count !== null && count !== undefined" class="mi-count">{{ count }}</span>
  </button>
  <div v-if="open" class="mi-indent"><slot /></div>
</template>
