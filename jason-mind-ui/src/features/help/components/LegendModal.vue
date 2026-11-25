<script setup lang="ts">
import EntityChip from '@/shared/components/EntityChip.vue'
import { Icon } from '@/shared/ui'
import { useUiStore } from '@/stores/ui.store'

/** Legenda: o que cada cor e ícone de chip quer dizer (Message, Event, Plan, Intention, Goal, Subgoal, Action, Belief). */
const ui = useUiStore()

const ITEMS: { type: string; label: string; badge?: string; desc: string }[] = [
  { type: 'intention', label: 'Intention', badge: '12', desc: 'An <b>intention</b>: a course of action the agent committed to, shown only by its number (#12). The goals of its stack are listed under it.' },
  { type: 'goal', label: '+!entregar(42)', badge: '31', desc: 'The <b>goal</b> that started the intention. It sits at the bottom of the stack and stays last in the list.' },
  { type: 'subgoal', label: '+!ir_ate(E)', badge: '39', desc: 'A <b>subgoal</b> posted by a plan of the goal below it. The top of the stack runs; the goals under it wait for it to finish.' },
  { type: 'belief', label: 'entregas(2)', desc: 'A <b>belief</b>: something the agent currently holds true about itself or the world, for example entregue(41).' },
  { type: 'message', label: 'achieve entregar(42)', desc: 'A <b>message</b> received from, or sent to, another agent (tell, achieve, askOne...). Here an achieve request.' },
  { type: 'event', label: '+!entregar(42)[source(atendente)]', desc: 'An <b>event</b> selected in the cycle: a belief or goal change such as +!entregar(42), with its source.' },
  { type: 'plan', label: '+!entregar(P) : moto_ok & combustivel(C) & C > 10', desc: 'A <b>plan</b> chosen for the event: its trigger and the context that held true.' },
  { type: 'action', label: '.print("Saindo com o pedido ", P)', desc: 'An <b>action</b> executed in the cycle, such as .print(...) or an environment action.' },
]
</script>

<template>
  <div class="mi-legscrim" @click="ui.legend = false">
    <div role="dialog" aria-label="Legend" class="mi-legend" @click.stop>
      <div style="display: flex; align-items: center; gap: var(--sp-8); padding: var(--sp-16) var(--sp-16) var(--sp-8)">
        <strong style="flex: 1; font-size: 14px; font-weight: 600">Legend</strong>
        <button type="button" class="mi-btn mi-icon" title="Close legend" aria-label="Close legend" style="border-color: transparent; width: 24px; height: 24px" @click="ui.legend = false"><Icon name="close-2" /></button>
      </div>
      <div style="overflow: auto; padding: var(--sp-4) var(--sp-16) var(--sp-16); display: flex; flex-direction: column; gap: var(--sp-12)">
        <div v-for="it in ITEMS" :key="it.type" style="display: flex; align-items: center; gap: var(--sp-16); min-height: 28px">
          <span style="width: 230px; flex-shrink: 0; min-width: 0; display: flex; align-items: center"><EntityChip :type="it.type" :label="it.label" :id-badge="it.badge ?? null" icon static /></span>
          <span style="flex: 1; min-width: 0; font-size: 12.5px; line-height: 1.45; color: var(--c-5b5f6b)" v-html="it.desc" />
        </div>
        <div style="display: flex; align-items: center; gap: var(--sp-16); min-height: 28px">
          <span style="width: 230px; flex-shrink: 0"><EntityChip type="belief" label="entregue(41)" change="new" static /></span>
          <span style="flex: 1; font-size: 12.5px; line-height: 1.45; color: var(--c-5b5f6b)">A <b>new</b> marker: the entity was born in this cycle. A chip with a - marker is one that ended in it.</span>
        </div>
      </div>
    </div>
  </div>
</template>
