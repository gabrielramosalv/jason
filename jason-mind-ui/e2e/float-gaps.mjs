// Janelas flutuantes da Timeline: posição e tamanho relativos ao palco, vãos entre elas e o cabeçalho, UI contra protótipo.
//   node e2e/float-gaps.mjs <url da UI> <url do protótipo>
import { chromium } from 'playwright-core'
const [ui, proto] = process.argv.slice(2)
const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })

async function floating(url, which) {
  const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
  await p.addInitScript(() => localStorage.setItem('mind-inspector:theme', 'dark'))
  await p.goto(url, { waitUntil: 'networkidle' })
  await p.waitForTimeout(2000)
  await p.locator(which ? 'button[aria-label="Timeline view"]' : '[data-tour="view-toggle"]').click()
  await p.waitForTimeout(1500)
  const r = await p.evaluate((w) => {
    const stage = document.querySelector(w ? '.av-stage' : '.mi-stage').getBoundingClientRect()
    const rect = (e) => { const r = e.getBoundingClientRect(); return { x: Math.round(r.left - stage.left), y: Math.round(r.top - stage.top), w: Math.round(r.width), h: Math.round(r.height) } }
    const wins = [...document.querySelectorAll(w ? '.av-grid[data-fl="1"] > [data-k]' : '.mi-grid[data-fl="1"] > section')].map((e) => ({ k: e.dataset.k, ...rect(e), header: Math.round(e.querySelector(w ? '.av-ph' : '.mi-ph').getBoundingClientRect().height), tabs: e.querySelectorAll(w ? '.av-tabbtn' : '.mi-tabbtn').length }))
    return { stage: { w: Math.round(stage.width), h: Math.round(stage.height) }, wins }
  }, which)
  await p.close()
  return r
}
const a = await floating(ui, 0)
const c = await floating(proto, 1)
console.log('palco   ui=', JSON.stringify(a.stage), ' prototype=', JSON.stringify(c.stage))
const pick = (r) => Object.fromEntries(r.wins.map((x) => [x.k, x]))
const A = pick(a), C = pick(c)
for (const k of new Set([...Object.keys(A), ...Object.keys(C)])) console.log(k.padEnd(4), 'ui=', JSON.stringify(A[k]), ' prototype=', JSON.stringify(C[k]))
const gap = (r) => { const w = r.wins; return w.length > 1 ? { x: w[1].x - (w[0].x + w[0].w), y: w[1].y - w[0].y } : null }
console.log('vão entre as duas primeiras: ui=', JSON.stringify(gap(a)), ' prototype=', JSON.stringify(gap(c)))
await b.close()
