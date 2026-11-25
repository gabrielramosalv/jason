// :hover — passa o mouse sobre pares de elementos da UI e do protótipo e compara o que muda (fundo, cor, filtro, transformação, sublinhado).
//   node e2e/hover.mjs <url da UI> <url do protótipo>
import { chromium } from 'playwright-core'
const [ui, proto] = process.argv.slice(2)
const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })

/** [nome, seletor na UI, seletor no protótipo, preparo opcional] */
const PAIRS = [
  ['botão do header (Help)', '.hd-btn[aria-label="Help"]', 'header button[aria-label="Help"]'],
  ['botão da barra esquerda (Agents)', '.mi-rail-l button[aria-label="Agents"]', 'aside.av-rail button[aria-label="Agents"]'],
  ['botão da barra (Search cycles)', '[data-tour="tools"] button >> nth=0', 'aside[aria-label="Timeline tools"] button >> nth=0'],
  ['botão da barra direita (Beliefs)', '.mi-rail-r button[aria-label="Beliefs"]', 'aside[aria-label="Sidebar"] button[aria-label="Beliefs"]'],
  ['reset layout', '.mi-rail-r button[aria-label="Reset layout"]', 'aside[aria-label="Sidebar"] button[aria-label="Reset layout"]'],
  ['linha de agente', '.mi-agside .mi-row >> nth=1', '.av-agside .av-row >> nth=1'],
  ['ciclo anterior', 'button[aria-label="Previous cycle"]', 'button[aria-label="Previous cycle"]'],
  ['número do ciclo', '.mi-cyc .mi-cyclink', 'button[aria-label="Go to a specific cycle"]'],
  ['hide do painel', 'section[data-k="code"] button[aria-label="Hide"]', 'section[data-k="code"] button[aria-label="Hide"]'],
  ['aba do console', 'section[data-k="console"] [data-tab="msgs"] .mi-tabbtn', 'section[data-k="console"] .av-tabwrap >> nth=1 >> .av-tabbtn'],
  ['cabeçalho de seção (crenças)', 'section[data-k="bel"] .mi-sechead >> nth=0', 'section[data-k="bel"] .av-sechead >> nth=0'],
  ['chip de crença', 'section[data-k="bel"] .mi-bchip >> nth=0', 'section[data-k="bel"] .av-bchip >> nth=0'],
  ['filtro de crenças (Mental notes)', 'section[data-k="bel"] button[aria-label="Mental notes"]', 'section[data-k="bel"] button[aria-label="Mental notes"]'],
  ['botão de busca do console', 'section[data-k="console"] button[aria-label="Search console"]', 'section[data-k="console"] button[aria-label="Search console"]'],
  ['filtro de agentes do console', 'section[data-k="console"] button[aria-label="Filter agents"]', 'section[data-k="console"] button[aria-label="Filter agents"]'],
  ['chip de ciclo do console', '.mi-logrow .mi-cyclink >> nth=0', 'section[data-k="console"] .av-cyclink >> nth=0'],
  ['variáveis: minimizar', 'button[aria-label="Minimize variables"]', 'button[aria-label="Minimize variables"]'],
  ['intenção na runbar', '.mi-runbar .mi-bchip >> nth=0', '.av-runbar .av-bchip >> nth=0'],
]

async function snap(url, which) {
  const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
  await p.addInitScript(() => localStorage.setItem('mind-inspector:theme', 'dark'))
  await p.goto(url, { waitUntil: 'networkidle' })
  await p.waitForTimeout(2200)
  if (which === 0) {
    for (let n = 3; n < 60; n += 3) {
      await p.locator('.mi-cyc .mi-cyclink').click(); await p.locator('.mi-cycinput').fill(String(n)); await p.keyboard.press('Enter'); await p.waitForTimeout(500)
      if (await p.locator('.mi-runbar').count()) break
    }
  }
  const out = {}
  for (const pair of PAIRS) {
    const [css, nth] = pair[1 + which].split(' >> nth=')
    const loc = p.locator(css).nth(Number(nth ?? 0))
    if (!(await loc.count())) { out[pair[0]] = null; continue }
    const read = () => loc.evaluate((e) => { const c = getComputedStyle(e); return { bg: c.backgroundColor, color: c.color, filter: c.filter, transform: c.transform, deco: c.textDecorationLine, cursor: c.cursor, opacity: c.opacity, shadow: c.boxShadow } })
    await p.mouse.move(2, 2)
    await p.waitForTimeout(220)
    const before = await read()
    await loc.hover({ force: true })
    await p.waitForTimeout(260)
    const after = await read()
    out[pair[0]] = { before, after, changed: Object.keys(after).filter((k) => after[k] !== before[k]) }
  }
  await p.close()
  return out
}

const mine = await snap(ui, 0)
const ref = await snap(proto, 1)
let diffs = 0
for (const [name] of PAIRS) {
  const a = mine[name]
  const c = ref[name]
  if (!a || !c) { console.log(`?    ${name.padEnd(36)} ${!a ? 'ausente na UI' : 'ausente no protótipo'}`); continue }
  const keys = ['bg', 'color', 'filter', 'transform', 'deco', 'cursor', 'opacity']
  const bad = keys.filter((k) => a.after[k] !== c.after[k])
  const noHover = !a.changed.length && c.changed.length
  if (bad.length || noHover) diffs++
  console.log(`${bad.length || noHover ? 'DIFF' : 'ok  '} ${name.padEnd(36)} ${noHover ? 'SEM :hover na UI (protótipo muda ' + c.changed.join(',') + ')  ' : ''}${bad.map((k) => `${k}: ui=${a.after[k]} proto=${c.after[k]}`).join(' | ')}`)
}
console.log(diffs ? `${diffs} diferença(s) de :hover` : ':hover confere')
await b.close()
process.exit(diffs ? 1 : 0)
