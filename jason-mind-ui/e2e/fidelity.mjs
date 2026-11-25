// Fidelidade ao protótipo: mede a geometria e os estilos dos mesmos elementos na UI e no protótipo (Agent-view.dc.html servido
// com o runtime dele) e compara. Uso: node e2e/fidelity.mjs <url da UI> <url do protótipo> [--save e2e/prototype-metrics.json]
// Sem a URL do protótipo, compara a UI com os números já medidos em e2e/prototype-metrics.json.
import { chromium } from 'playwright-core'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'

const [ui, proto] = process.argv.slice(2).filter((a) => !a.startsWith('--'))
const save = process.argv.includes('--save') ? process.argv[process.argv.indexOf('--save') + 1] : null
const METRICS = new URL('./prototype-metrics.json', import.meta.url)
const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', '/usr/bin/google-chrome'].find(existsSync)

/** Seletor de cada elemento na UI (mi-) e no protótipo (av-). */
const ELEMENTS = {
  header: ['.mi-header', 'header'],
  railLeft: ['.mi-rail-l', 'aside.av-rail >> nth=0'],
  railRight: ['.mi-rail-r', 'aside[aria-label="Sidebar"]'],
  agentSidebar: ['.mi-agside', '.av-agside'],
  agentRow: ['.mi-agside .mi-row', '.av-agside .av-row'],
  topBar: ['.mi-bar', 'main.av-agent > div:first-child'],
  iconButton: ['.mi-rail-l .mi-icon', 'aside.av-rail .av-icon'],
  panelCode: ['section[data-k="code"]', 'section[data-k="code"]'],
  panelInt: ['section[data-k="int"]', 'section[data-k="int"]'],
  panelBel: ['section[data-k="bel"]', 'section[data-k="bel"]'],
  panelConsole: ['section[data-k="console"]', 'section[data-k="console"]'],
  panelHeader: ['section[data-k="code"] .mi-ph', 'section[data-k="code"] .av-ph'],
  tabButton: ['section[data-k="console"] .mi-tabbtn', 'section[data-k="console"] .av-tabbtn'],
  sectionHead: ['section[data-k="bel"] .mi-sechead', 'section[data-k="bel"] .av-sechead'],
  chip: ['section[data-k="bel"] .mi-bchip', 'section[data-k="bel"] .av-bchip'],
}

async function measure(url, which) {
  const browser = await chromium.launch({ executablePath, headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  await page.addInitScript(() => localStorage.setItem('mind-inspector:theme', 'dark'))
  await page.goto(url, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2500)
  const out = {}
  for (const [name, sels] of Object.entries(ELEMENTS)) {
    const sel = sels[which]
    out[name] = await page.evaluate((s) => {
      const [css, nth] = s.split(' >> nth=')
      const el = document.querySelectorAll(css)[Number(nth ?? 0)]
      if (!el) return null
      const r = el.getBoundingClientRect()
      const c = getComputedStyle(el)
      return { w: Math.round(r.width), h: Math.round(r.height), radius: c.borderTopLeftRadius, font: c.fontSize, weight: c.fontWeight, bg: c.backgroundColor, color: c.color, gap: c.gap }
    }, sel)
  }
  // espaços entre painéis vizinhos
  out.gapCodeInt = await page.evaluate(() => {
    const a = document.querySelector('section[data-k="code"]')?.getBoundingClientRect()
    const b = document.querySelector('section[data-k="int"]')?.getBoundingClientRect()
    return a && b ? Math.round(b.left - a.right) : null
  })
  out.gapTopConsole = await page.evaluate(() => {
    const a = document.querySelector('section[data-k="code"]')?.getBoundingClientRect()
    const b = document.querySelector('section[data-k="console"]')?.getBoundingClientRect()
    return a && b ? Math.round(b.top - a.bottom) : null
  })
  // distância dos painéis às bordas do palco (as barras laterais e a barra de cima)
  out.edges = await page.evaluate(() => {
    const q = (s) => document.querySelector(s)?.getBoundingClientRect()
    const rails = [...document.querySelectorAll('aside.mi-rail, aside.av-rail')].map((e) => e.getBoundingClientRect())
    const code = q('section[data-k="code"]'), bel = q('section[data-k="bel"]'), con = q('section[data-k="console"]')
    const bar = q('.mi-bar, main.av-agent > div:first-child')
    const left = rails.filter((r) => r.right <= code.left).pop()
    const right = rails[rails.length - 1]
    return { left: Math.round(code.left - left.right), top: Math.round(code.top - bar.bottom), bottom: Math.round(innerHeight - con.bottom), right: Math.round(right.left - bel.right) }
  })
  out.rootBg = await page.evaluate(() => getComputedStyle(document.querySelector('.mi-root, .av-root')).backgroundColor)
  out.rootColor = await page.evaluate(() => getComputedStyle(document.querySelector('.mi-root, .av-root')).color)
  await browser.close()
  return out
}

const mine = await measure(ui, 0)
let reference
if (proto) {
  reference = await measure(proto, 1)
  if (save) writeFileSync(save, JSON.stringify(reference, null, 2))
} else reference = JSON.parse(readFileSync(METRICS, 'utf-8'))

const TOLERANCE_PX = 2
let diffs = 0
const rows = []
const same = (a, b) => (typeof a === 'number' && typeof b === 'number' ? Math.abs(a - b) <= TOLERANCE_PX : JSON.stringify(a) === JSON.stringify(b))
for (const key of Object.keys(reference)) {
  const a = mine[key]
  const b = reference[key]
  if (key === 'edges') {
    for (const side of ['left', 'top', 'bottom', 'right']) {
      const ok = a && same(a[side], b[side])
      if (!ok) diffs++
      rows.push([ok ? 'ok  ' : 'DIFF', `edges.${side}`, a?.[side], b[side]])
    }
  } else if (b && typeof b === 'object') {
    for (const prop of ['w', 'h', 'radius', 'font', 'weight', 'bg', 'color']) {
      // largura e altura dependem dos dados e do espaço (painéis); só comparo o que é intrínseco ao componente
      const layoutDependent = ['panelCode', 'panelInt', 'panelBel', 'panelConsole', 'agentRow', 'sectionHead', 'chip', 'panelHeader', 'tabButton'].includes(key) && (prop === 'w' || (prop === 'h' && /panel/.test(key) && key !== 'panelHeader'))
      // altura do chip (quebra de linha) e fundo da linha de agente (seleção) dependem dos dados
      const dataDependent = (key === 'chip' && prop === 'h') || (key === 'agentRow' && prop === 'bg')
      if (layoutDependent || dataDependent) continue
      const ok = a && same(a[prop], b[prop])
      if (!ok) diffs++
      rows.push([ok ? 'ok  ' : 'DIFF', `${key}.${prop}`, a?.[prop], b[prop]])
    }
  } else {
    const ok = same(a, b)
    if (!ok) diffs++
    rows.push([ok ? 'ok  ' : 'DIFF', key, a, b])
  }
}
for (const r of rows) console.log(`${r[0]} ${String(r[1]).padEnd(28)} ui=${r[2]}  prototype=${r[3]}`)
console.log(diffs ? `${diffs} diferença(s) acima da tolerância` : 'geometria e estilos conferem com o protótipo')
process.exit(diffs ? 1 : 0)
