<script setup lang="ts">
import { IconButton } from '@/shared/ui'
import { useInspectorStore } from '@/stores/inspector.store'

/** Os três filtros do cabeçalho do painel Beliefs: Mental notes, Messages e Environment. */
const inspector = useInspectorStore()
</script>

<template>
  <IconButton icon="filter-notes" label="Mental notes" :selected="inspector.beliefFilter.self" @click="inspector.beliefFilter.self = !inspector.beliefFilter.self" />
  <IconButton icon="filter-messages" label="Messages" :selected="inspector.beliefFilter.msg" @click="inspector.beliefFilter.msg = !inspector.beliefFilter.msg" />
  <IconButton icon="filter-environment" label="Environment" :selected="inspector.beliefFilter.env" @click="inspector.beliefFilter.env = !inspector.beliefFilter.env" />
</template>
