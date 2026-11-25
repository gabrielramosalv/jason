/**
 * Árvore de divisões do layout (como nos editores modernos). Uma folha é um painel ou um grupo de abas;
 * um nó interno divide o espaço em linha (`h`) ou em coluna (`v`), com pesos relativos.
 */
export type PanelKey = 'code' | 'int' | 'ev' | 'bel' | 'console' | 'msgs' | 'reason'

export interface Leaf {
  /** Painel ativo da folha. */
  k: PanelKey
  /** Abas do grupo, quando há mais de um painel (inclui `k`). */
  t?: PanelKey[]
}

export interface Split {
  o: 'h' | 'v'
  ch: DockNode[]
  w: number[]
}

export type DockNode = Leaf | Split

export const isLeaf = (n: DockNode): n is Leaf => 'k' in n

export const PANEL_KEYS: PanelKey[] = ['code', 'int', 'ev', 'bel', 'console', 'msgs', 'reason']

/** Disposição inicial: código, intenções (com Events em aba) e crenças em cima; console com Messages e Reasoning em abas embaixo. */
export function defaultTree(): DockNode {
  return {
    o: 'v',
    w: [38, 62],
    ch: [
      { o: 'h', w: [11, 5, 5], ch: [{ k: 'code' }, { k: 'int', t: ['int', 'ev'] }, { k: 'bel' }] },
      { k: 'console', t: ['console', 'msgs', 'reason'] },
    ],
  }
}

/** Painéis que começam escondidos no layout padrão (no protótipo, todos começam à vista). */
export const DEFAULT_HIDDEN: PanelKey[] = []

/** Onde um painel reaparece quando é mostrado de novo: ao lado de um irmão, em aba, ou na borda. */
export const DEFAULT_PLACE: Record<PanelKey, { sib?: PanelKey; side?: 'tab' | 'left' | 'right'; root?: 'bottom' }> = {
  reason: { sib: 'console', side: 'tab' },
  msgs: { sib: 'console', side: 'tab' },
  bel: { sib: 'int', side: 'right' },
  int: { sib: 'code', side: 'right' },
  ev: { sib: 'int', side: 'tab' },
  code: { sib: 'int', side: 'left' },
  console: { root: 'bottom' },
}

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T

export function leavesOf(node: DockNode | null, out: PanelKey[] = []): PanelKey[] {
  if (!node) return out
  if (isLeaf(node)) (node.t ?? [node.k]).forEach((k) => out.push(k))
  else node.ch.forEach((c) => leavesOf(c, out))
  return out
}

/** Caminho (índices de filhos) até a folha que contém o painel, ou nulo. */
export function pathOf(node: DockNode | null, key: PanelKey, path: number[] = []): number[] | null {
  if (!node) return null
  if (isLeaf(node)) return (node.t ?? [node.k]).includes(key) ? path : null
  for (let i = 0; i < node.ch.length; i++) {
    const found = pathOf(node.ch[i]!, key, [...path, i])
    if (found) return found
  }
  return null
}

export function nodeAt(node: DockNode, path: number[]): DockNode {
  return path.reduce<DockNode>((n, i) => (isLeaf(n) ? n : n.ch[i]!), node)
}

/** Funde divisões na mesma direção e remove nós vazios, para a árvore ficar sempre canônica. */
export function normalize(node: DockNode | null): DockNode | null {
  if (!node || isLeaf(node)) return node
  const ch: DockNode[] = []
  const w: number[] = []
  node.ch.forEach((child, i) => {
    const n = normalize(child)
    if (!n) return
    const weight = node.w[i]! > 0 ? node.w[i]! : 1
    if (!isLeaf(n) && n.o === node.o) {
      const sum = n.w.reduce((a, b) => a + b, 0)
      n.ch.forEach((g, j) => {
        ch.push(g)
        w.push((weight * n.w[j]!) / sum)
      })
    } else {
      ch.push(n)
      w.push(weight)
    }
  })
  if (!ch.length) return null
  if (ch.length === 1) return ch[0]!
  return { o: node.o, ch, w }
}

/** Tira um painel da árvore (some a aba; se era a única da folha, some a folha). */
export function removeLeaf(tree: DockNode | null, key: PanelKey): DockNode | null {
  const root = clone(tree)
  const strip = (n: DockNode): DockNode | null => {
    if (isLeaf(n)) {
      if (n.t) {
        const t = n.t.filter((x) => x !== key)
        if (!t.length) return null
        return t.length === 1 ? { k: t[0]! } : { k: t.includes(n.k) ? n.k : t[0]!, t }
      }
      return n.k === key ? null : n
    }
    const pairs = n.ch.map((c, i) => [strip(c), n.w[i]!] as const).filter(([c]) => c !== null)
    if (!pairs.length) return null
    return { o: n.o, ch: pairs.map(([c]) => c!), w: pairs.map(([, w]) => w) }
  }
  return normalize(root ? strip(root) : null)
}

/** Torna ativa a aba `key` do grupo que a contém. */
export function setActive(tree: DockNode, key: PanelKey): DockNode {
  const root = clone(tree)
  const p = pathOf(root, key)
  if (!p) return root
  const leaf = nodeAt(root, p)
  if (isLeaf(leaf) && leaf.t) leaf.k = key
  return root
}

/** Insere um painel ao lado de `sib` (`left`, `right`, `top`, `bottom`) ou como aba dele. */
export function insertLeaf(
  tree: DockNode | null,
  key: PanelKey,
  target: { sib: PanelKey; side: 'tab' | 'left' | 'right' | 'top' | 'bottom' } | { root: 'bottom' | 'top' | 'left' | 'right' },
): DockNode {
  const root = clone(tree)
  if (!root) return { k: key }
  if ('root' in target) {
    const o = target.root === 'bottom' || target.root === 'top' ? 'v' : 'h'
    const first = target.root === 'top' || target.root === 'left'
    const split: Split = { o, ch: first ? [{ k: key }, root] : [root, { k: key }], w: first ? [30, 70] : [70, 30] }
    return normalize(split)!
  }
  const p = pathOf(root, target.sib)
  if (!p) return insertLeaf(root, key, { root: 'bottom' })
  const leaf = nodeAt(root, p)
  if (isLeaf(leaf) && target.side === 'tab') {
    const t = leaf.t ? [...leaf.t, key] : [leaf.k, key]
    leaf.t = t
    leaf.k = key
    return root
  }
  const o = target.side === 'left' || target.side === 'right' ? 'h' : 'v'
  const first = target.side === 'left' || target.side === 'top'
  const wrapped: Split = { o, ch: first ? [{ k: key }, clone(leaf)] : [clone(leaf), { k: key }], w: [1, 1] }
  if (!p.length) return normalize(wrapped)!
  const parent = nodeAt(root, p.slice(0, -1)) as Split
  parent.ch[p[p.length - 1]!] = wrapped
  return normalize(root)!
}

/** Retângulo (em frações de 0 a 1) de cada folha, e as divisórias entre os filhos de cada divisão. */
export interface Rect {
  x: number
  y: number
  w: number
  h: number
}
export interface Handle {
  path: number[]
  index: number
  o: 'h' | 'v'
  /** Posição da linha (fração) e extensão da divisão que ela separa. */
  at: number
  span: Rect
}

export function layoutOf(tree: DockNode | null): { leaves: { leaf: Leaf; rect: Rect }[]; handles: Handle[] } {
  const leaves: { leaf: Leaf; rect: Rect }[] = []
  const handles: Handle[] = []
  const walk = (n: DockNode, r: Rect, path: number[]) => {
    if (isLeaf(n)) {
      leaves.push({ leaf: n, rect: r })
      return
    }
    const sum = n.w.reduce((a, b) => a + b, 0)
    let at = n.o === 'h' ? r.x : r.y
    n.ch.forEach((c, i) => {
      const share = n.w[i]! / sum
      const cr: Rect = n.o === 'h' ? { x: at, y: r.y, w: r.w * share, h: r.h } : { x: r.x, y: at, w: r.w, h: r.h * share }
      walk(c, cr, [...path, i])
      at += n.o === 'h' ? r.w * share : r.h * share
      if (i < n.ch.length - 1) handles.push({ path, index: i, o: n.o, at, span: r })
    })
  }
  if (tree) walk(tree, { x: 0, y: 0, w: 1, h: 1 }, [])
  return { leaves, handles }
}

/** Move a divisória `index` da divisão em `path` por `fraction` da extensão dela, com um mínimo por filho. */
export function resizeSplit(tree: DockNode, path: number[], index: number, deltaFraction: number): DockNode {
  const root = clone(tree)
  const node = nodeAt(root, path)
  if (isLeaf(node)) return root
  const sum = node.w.reduce((a, b) => a + b, 0)
  const min = 0.08 * sum
  const d = deltaFraction * sum
  const a = node.w[index]! + d
  const b = node.w[index + 1]! - d
  if (a < min || b < min) return root
  node.w[index] = a
  node.w[index + 1] = b
  return root
}

export type DropSide = 'tab' | 'left' | 'right' | 'top' | 'bottom'

/**
 * Move painéis (uma aba ou um contêiner inteiro) para junto de `sib`: como aba dele ou numa divisão à
 * esquerda, à direita, em cima ou embaixo. Soltar sobre o próprio grupo não muda nada.
 */
export function moveKeys(tree: DockNode | null, keys: PanelKey[], sib: PanelKey, side: DropSide): DockNode | null {
  if (!tree || !keys.length || keys.includes(sib)) return tree
  const from = pathOf(tree, keys[0]!)
  const to = pathOf(tree, sib)
  // soltar sobre o próprio grupo, ou a única aba de um grupo ao redor dele mesmo, não muda nada
  if (from && to && from.join('.') === to.join('.') && (side === 'tab' || (nodeAt(tree, from) as Leaf).t === undefined)) return tree
  let next: DockNode | null = tree
  for (const k of keys) next = removeLeaf(next, k)
  if (!next) return tree
  const [first, ...rest] = keys
  let out = insertLeaf(next, first!, { sib, side })
  for (const k of rest) out = insertLeaf(out, k, { sib: first!, side: 'tab' })
  return setActive(out, first!)
}

/** Em qual zona de um painel (largura w, altura h) o ponteiro caiu: o cabeçalho e o centro viram aba. */
export function dropSide(x: number, y: number, w: number, h: number, headerHeight = 36): DropSide {
  if (y < headerHeight) return 'tab'
  const fx = x / w
  const fy = (y - headerHeight) / Math.max(1, h - headerHeight)
  if (fx < 0.25) return 'left'
  if (fx > 0.75) return 'right'
  if (fy < 0.25) return 'top'
  if (fy > 0.75) return 'bottom'
  return 'tab'
}
