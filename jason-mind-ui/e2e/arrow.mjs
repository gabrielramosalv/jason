// Compara a seta entre fases do Reasoning (caminhos, traço, camada) e salva um recorte de cada lado.
//   node e2e/arrow.mjs <url do app> <url do protótipo> <pasta de saída>
import { chromium } from 'playwright-core'
import { existsSync } from 'node:fs'

const [app = 'http://127.0.0.1:5173', proto = 'http://127.0.0.1:8791/Agent-view.dc.html', outDir = '.'] = process.argv.slice(2)
const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(existsSync)
const b = await chromium.launch({ executablePath, headless: true })
for (const [name, url] of [['app', app], ['proto', proto]]) {
  const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
  await p.goto(url, { waitUntil: 'networkidle' })
  await p.waitForTimeout(2000)
  if (name === 'app') {
    for (let n = 3; n < 60; n += 3) {
      await p.locator('.mi-cyc .mi-cyclink').click()
      await p.locator('.mi-cycinput').fill(String(n))
      await p.keyboard.press('Enter')
      await p.waitForTimeout(500)
      if (await p.locator('.mi-runbar').count()) break
    }
    await p.locator('section[data-k="console"] [data-tab="reason"] .mi-tabbtn').click()
  } else await p.locator('section[data-k="console"] .av-tabbtn:has-text("Reasoning")').click()
  await p.waitForTimeout(1800)
  const info = await p.evaluate(() => {
    const paths = [...document.querySelectorAll('[data-k="reason"] svg path')].map((x) => {
      const cs = getComputedStyle(x)
      const r = x.getBoundingClientRect()
      const svg = x.closest('svg')
      return { d: x.getAttribute('d'), stroke: cs.stroke, sw: cs.strokeWidth, cap: cs.strokeLinecap, join: cs.strokeLinejoin, op: cs.opacity, x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height), svgZ: getComputedStyle(svg).zIndex, svgPos: getComputedStyle(svg).position, svgOverflow: getComputedStyle(svg).overflow }
    })
    return paths.filter((x) => x.d && x.d.startsWith('M') && /^M[\d.]+ [\d.]+ L[\d.]+ [\d.]+$/.test(x.d)).slice(0, 4)
  })
  console.log(name, JSON.stringify(info, null, 1))
  const el = await p.$('[data-k="reason"]')
  if (el) await el.screenshot({ path: `${outDir}/arrow-${name}.png`, timeout: 5000 })
  await p.close()
}
await b.close()
