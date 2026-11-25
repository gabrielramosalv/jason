import { describe, expect, it } from 'vitest'
import { defaultTree, dropSide, insertLeaf, layoutOf, leavesOf, moveKeys, normalize, removeLeaf, resizeSplit, setActive } from './dock-tree'
import type { DockNode } from './dock-tree'

describe('dock-tree', () => {
  it('o layout padrão tem os sete painéis: Events em aba das intenções e Messages e Reasoning em abas do console', () => {
    const tree = defaultTree()
    expect(leavesOf(tree).sort()).toEqual(['bel', 'code', 'console', 'ev', 'int', 'msgs', 'reason'])
    const { leaves } = layoutOf(tree)
    expect(leaves).toHaveLength(4)
    expect(leaves.find((l) => l.leaf.k === 'console')?.leaf.t).toEqual(['console', 'msgs', 'reason'])
  })

  it('esconder o único painel de uma folha a remove e a divisão colapsa', () => {
    const next = removeLeaf(defaultTree(), 'bel')!
    expect(leavesOf(next)).not.toContain('bel')
    const { handles } = layoutOf(next)
    // sobraram código e intenções em cima e o console embaixo: duas divisórias
    expect(handles).toHaveLength(2)
  })

  it('esconder uma aba mantém o grupo e uma aba só vira painel simples', () => {
    let tree = removeLeaf(defaultTree(), 'msgs')!
    tree = removeLeaf(tree, 'reason')!
    const console = layoutOf(tree).leaves.find((l) => l.leaf.k === 'console')!
    expect(console.leaf.t).toBeUndefined()
  })

  it('mostrar de novo coloca o painel ao lado do irmão padrão', () => {
    const hidden = removeLeaf(defaultTree(), 'bel')!
    const back = insertLeaf(hidden, 'bel', { sib: 'int', side: 'right' })
    expect(leavesOf(back)).toContain('bel')
    const order = layoutOf(back).leaves.map((l) => l.leaf.k)
    expect(order.indexOf('bel')).toBeGreaterThan(order.indexOf('int'))
  })

  it('setActive troca a aba ativa sem mudar a árvore', () => {
    const next = setActive(defaultTree(), 'reason')
    expect(layoutOf(next).leaves.find((l) => l.leaf.t?.includes('reason'))?.leaf.k).toBe('reason')
  })

  it('normalize funde divisões na mesma direção e as retângulos somam o todo', () => {
    const nested: DockNode = { o: 'h', w: [1, 1], ch: [{ o: 'h', w: [1, 1], ch: [{ k: 'code' }, { k: 'int' }] }, { k: 'bel' }] }
    const flat = normalize(nested)!
    expect('ch' in flat && flat.ch).toHaveLength(3)
    const area = layoutOf(flat).leaves.reduce((sum, l) => sum + l.rect.w * l.rect.h, 0)
    expect(area).toBeCloseTo(1)
  })

  it('resizeSplit move a divisória e respeita o mínimo de cada filho', () => {
    const tree = defaultTree()
    const moved = resizeSplit(tree, [], 0, 0.1)
    const before = layoutOf(tree).leaves.find((l) => l.leaf.k === 'console')!.rect.h
    const after = layoutOf(moved).leaves.find((l) => l.leaf.k === 'console')!.rect.h
    expect(after).toBeLessThan(before)
    const refused = resizeSplit(tree, [], 0, 0.9)
    expect(layoutOf(refused).leaves.find((l) => l.leaf.k === 'console')!.rect.h).toBeCloseTo(before)
  })

  it('moveKeys leva uma aba para outra folha, como aba ou ao lado', () => {
    const asTab = moveKeys(defaultTree(), ['reason'], 'bel', 'tab')!
    expect(layoutOf(asTab).leaves.find((l) => l.leaf.t?.includes('reason'))?.leaf.t).toEqual(['bel', 'reason'])
    expect(leavesOf(asTab).sort()).toEqual(['bel', 'code', 'console', 'ev', 'int', 'msgs', 'reason'])
    const beside = moveKeys(defaultTree(), ['reason'], 'code', 'left')!
    const order = layoutOf(beside).leaves.map((l) => l.leaf.k)
    expect(order.indexOf('reason')).toBeLessThan(order.indexOf('code'))
  })

  it('moveKeys move o contêiner inteiro com as abas e ignora soltar sobre si mesmo', () => {
    const moved = moveKeys(defaultTree(), ['console', 'msgs', 'reason'], 'code', 'right')!
    expect(layoutOf(moved).leaves.find((l) => l.leaf.t?.includes('msgs'))?.leaf.t).toEqual(['console', 'msgs', 'reason'])
    const same = defaultTree()
    expect(moveKeys(same, ['msgs'], 'console', 'tab')).toEqual(same)
  })

  it('dropSide: cabeçalho e centro viram aba; as bordas dividem', () => {
    expect(dropSide(100, 10, 400, 300)).toBe('tab')
    expect(dropSide(200, 150, 400, 300)).toBe('tab')
    expect(dropSide(20, 150, 400, 300)).toBe('left')
    expect(dropSide(380, 150, 400, 300)).toBe('right')
    expect(dropSide(200, 60, 400, 300)).toBe('top')
    expect(dropSide(200, 290, 400, 300)).toBe('bottom')
  })
})
