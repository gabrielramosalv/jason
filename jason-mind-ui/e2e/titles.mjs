// Distância do título dos popups até a borda esquerda e centro da linha do Timeline contra as bolinhas, no app e no protótipo.
//   node e2e/titles.mjs <url do app> <url do protótipo>
import { chromium } from 'playwright-core'
import { existsSync } from 'node:fs'

const [app = 'http://127.0.0.1:5173', proto = 'http://127.0.0.1:8791/Agent-view.dc.html'] = process.argv.slice(2)
const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(existsSync)
const b = await chromium.launch({ executablePath, headless: true })

for (const [name, url, pre] of [['app', app, 'mi'], ['proto', proto, 'av']]) {
  const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
  await p.goto(url, { waitUntil: 'networkidle' })
  await p.waitForTimeout(2500)
  const out = await p.evaluate(async (pre) => {
    const res = {}
    const click = (sel) => document.querySelector(sel)?.click()
    // popups com título
    const dialogs = []
    for (const label of ['Settings', 'Help']) {
      const btn = [...document.querySelectorAll('button')].find((x) => x.getAttribute('aria-label') === label || x.title === label)
      if (!btn) continue
      btn.click()
      await new Promise((r) => setTimeout(r, 300))
      const d = document.querySelector('[role=dialog], [role=menu]')
      if (d) {
        const h = d.querySelector('h2, strong, span')
        const dr = d.getBoundingClientRect()
        const hr = h?.getBoundingClientRect()
        const first = d.querySelector('.' + pre + '-ph, .' + pre + '-phead') || d.firstElementChild
        const cs = getComputedStyle(first)
        dialogs.push({ label, title: h?.textContent?.trim().slice(0, 20), titleLeft: hr ? Math.round(hr.left - dr.left) : null, headPadL: cs.paddingLeft, dialogLeft: Math.round(dr.left), dialogRight: Math.round(dr.right) })
      }
      document.body.click()
      document.querySelector('[role=dialog]')?.parentElement
      await new Promise((r) => setTimeout(r, 200))
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
      await new Promise((r) => setTimeout(r, 200))
    }
    res.dialogs = dialogs
    return res
  }, pre)
  console.log(name, JSON.stringify(out))
  await p.close()
}
await b.close()
