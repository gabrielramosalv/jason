import { graphlib, layout } from '@dagrejs/dagre'
import type { ReasoningGraph } from './reasoning-graph'

export interface NodePosition {
  x: number
  y: number
}

/** O espaço mínimo entre o topo de uma etapa e o da etapa seguinte. */
const STEP_DROP = 44

/**
 * Posiciona os nós em camadas da esquerda para a direita (o tempo corre nesse sentido): quem causa vem antes de quem é causado.
 * As etapas seguem a ordem do ciclo: cada uma fica à direita de todas as anteriores ou abaixo delas (o Select event está sempre acima ou à
 * esquerda do Select plan), e só desce quando ainda não estaria à direita. Devolve o canto superior esquerdo de cada nó, como o Vue Flow espera.
 */
export function layoutGraph(graph: ReasoningGraph): Map<string, NodePosition> {
  const g = new graphlib.Graph()
  g.setGraph({ rankdir: 'LR', ranksep: 90, nodesep: 22, edgesep: 16, marginx: 16, marginy: 16 })
  g.setDefaultEdgeLabel(() => ({}))
  for (const n of graph.nodes) g.setNode(n.id, { width: n.width, height: n.height })
  // várias linhas entre o mesmo par contam como uma só no layout
  for (const e of graph.edges) if (!g.hasEdge(e.source, e.target)) g.setEdge(e.source, e.target)
  const steps = graph.nodes.filter((n) => n.kind === 'step').map((n) => n.id)
  layout(g)

  const out = new Map<string, NodePosition>()
  const centerX = new Map<string, number>()
  for (const n of graph.nodes) {
    const p = g.node(n.id)
    if (!p) continue
    out.set(n.id, { x: Math.round(p.x - n.width / 2), y: Math.round(p.y - n.height / 2) })
    centerX.set(n.id, p.x)
  }
  const heightOf = new Map(graph.nodes.map((n) => [n.id, n.height]))
  // cada etapa fica à direita de todas as anteriores ou abaixo delas (uma das duas basta)
  for (let j = 1; j < steps.length; j++) {
    const cur = out.get(steps[j]!)
    if (!cur) continue
    const column = centerX.get(steps[j]!)!
    const inColumn = [...out.keys()].filter((id) => Math.abs((centerX.get(id) ?? 0) - column) < 1)
    const wrong = steps.slice(0, j).filter((k) => out.has(k) && !(column > centerX.get(k)! + 1))
    if (!wrong.length) continue
    if (wrong.some((k) => inColumn.includes(k))) {
      // uma anterior está na mesma camada: a etapa desce para o fim dela, onde não cobre ninguém
      const bottom = Math.max(...inColumn.filter((id) => id !== steps[j]).map((id) => out.get(id)!.y + (heightOf.get(id) ?? 0)))
      cur.y = Math.max(cur.y, bottom + 22)
    } else {
      const delta = Math.max(...wrong.map((k) => out.get(k)!.y + STEP_DROP - cur.y))
      if (delta > 0) for (const id of inColumn) out.get(id)!.y += delta
    }
  }
  return out
}
