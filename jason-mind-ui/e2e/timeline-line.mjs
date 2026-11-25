// Confere se a linha do Timeline passa pelo centro das bolinhas e compara a seta entre fases do Reasoning.
//   node e2e/timeline-line.mjs <url do app> <url do protótipo>
import { chromium } from 'playwright-core'
import { existsSync } from 'node:fs'

const [app = 'http://127.0.0.1:5173', proto = 'http://127.0.0.1:8791/Agent-view.dc.html'] = process.argv.slice(2)
const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(existsSync)
const b = await chromium.launch({ executablePath, headless: true })
for (const [name, url, pre] of [['app', app, 'mi'], ['proto', proto, 'av']]) {
  const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
  await p.goto(url, { waitUntil: 'networkidle' })
  await p.waitForTimeout(2500)
  const out = await p.evaluate((pre) => {
    const dot = document.querySelector(`.${pre}-tldot`)
    const r = dot?.getBoundingClientRect()
    const lines = [...document.querySelectorAll('div')].filter((d) => d.style.height === '2px' && d.style.position === 'absolute').map((d) => d.getBoundingClientRect())
    const arrows = [...document.querySelectorAll('svg path')].filter((x) => /L/.test(x.getAttribute('d') ?? '') && x.getAttribute('stroke')?.includes('arw')).slice(0, 4).map((x) => ({ d: x.getAttribute('d'), stroke: x.getAttribute('stroke'), sw: x.getAttribute('stroke-width'), cap: x.getAttribute('stroke-linecap'), fill: x.getAttribute('fill') }))
    return { dotCenter: r ? r.top + r.height / 2 : null, lineCenters: lines.slice(0, 2).map((l) => l.top + l.height / 2), arrows }
  }, pre)
  console.log(name, JSON.stringify(out))
  await p.close()
}
await b.close()
