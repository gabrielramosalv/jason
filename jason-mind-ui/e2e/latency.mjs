// Mede a UI real contra os limiares por grupo de rotas da D-022 (experiments/README.md §1.2): abre a UI num navegador, usa o modo
// ao vivo com os painéis de cada disposição e agrupa a latência de cada requisição (do envio ao fim da resposta).
//   node e2e/latency.mjs <url> [segundos por disposição]
import { chromium } from 'playwright-core'
import { existsSync } from 'node:fs'

const base = (process.argv[2] ?? 'http://127.0.0.1:3272').replace(/\/$/, '')
const seconds = Number(process.argv[3] ?? 20)
const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', '/usr/bin/google-chrome'].find(existsSync)

/** Grupos e limites (p95, p99 em ms) da D-022. */
const GROUPS = [
  ['A1 batimento (mas)', /\/api\/mas(\?|$)/, 50, 150],
  ['A2 tempo real (state, console, messages, cycles)', /\/api\/(agents\/[^/]+(\?|$)|console|messages|agents\/[^/]+\/cycles\?)/, 50, 150],
  ['B por ciclo (reasoning, sources, plans, metrics)', /\/api\/(agents\/[^/]+\/cycles\/\d+\/reasoning|agents\/[^/]+\/(sources|plans)|metrics)/, 100, 300],
  ['C sob demanda (lifecycle, search)', /\/api\/agents\/[^/]+\/(entities|search)/, 300, 1000],
  ['D raras (settings)', /\/api\/settings/, 1000, Infinity],
]
const pct = (xs, p) => (xs.length ? [...xs].sort((a, b) => a - b)[Math.min(xs.length - 1, Math.ceil((p / 100) * xs.length) - 1)] : NaN)

const browser = await chromium.launch({ executablePath, headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
const samples = []
page.on('requestfinished', async (req) => {
  if (!req.url().includes('/api/')) return
  const t = req.timing()
  if (t.responseEnd > 0) samples.push({ url: req.url().replace(base, ''), ms: t.responseEnd })
})

async function disposition(name, setup) {
  await page.evaluate(() => localStorage.clear())
  await page.reload({ waitUntil: 'networkidle' })
  await setup()
  const before = samples.length
  await page.waitForTimeout(seconds * 1000)
  console.log(`disposição ${name}: ${samples.length - before} requisições em ${seconds} s`)
}

await page.goto(base, { waitUntil: 'networkidle' })
await disposition('T1 (código, intenções, crenças, console)', async () => {})
await disposition('T2 (messages no lugar do console)', async () => {
  await page.locator('section[data-k="console"] [data-tab="msgs"] .mi-tabbtn').click()
})
await disposition('T3 (reasoning no lugar do console)', async () => {
  await page.locator('section[data-k="console"] [data-tab="reason"] .mi-tabbtn').click()
})
await disposition('Timeline', async () => {
  await page.locator('[data-tour="view-toggle"]').click()
})
await disposition('tudo aberto (todos os painéis e Settings com métricas)', async () => {
  await page.locator('section[data-k="console"] [data-tab="reason"] .mi-tabbtn').click()
  await page.locator('.hd-btn[aria-label="Settings"]').click({ timeout: 5000 })
})
// ações sob demanda: ciclo de vida e busca
await page.reload({ waitUntil: 'networkidle' })
await page.locator('section[data-k="bel"] .mi-belief').first().click({ timeout: 5000 }).catch(() => {})
await page.keyboard.press('Escape')
await page.mouse.click(700, 5)
await page.locator('[data-tour="tools"] button').first().click({ timeout: 5000, force: true })
await page.locator('.mi-tlq').fill('a')
await page.waitForTimeout(2500)

console.log('')
let failed = 0
for (const [name, re, p95, p99] of GROUPS) {
  const xs = samples.filter((s) => re.test(s.url)).map((s) => s.ms)
  if (!xs.length) {
    console.log(`${name.padEnd(52)} sem amostras`)
    continue
  }
  const a = pct(xs, 95)
  const b = pct(xs, 99)
  const ok = a <= p95 && b <= p99
  if (!ok) failed++
  console.log(`${ok ? 'ok  ' : 'FORA'} ${name.padEnd(52)} n=${String(xs.length).padStart(5)}  p50=${pct(xs, 50).toFixed(1)}  p95=${a.toFixed(1)} (limite ${p95})  p99=${b.toFixed(1)} (limite ${p99})`)
}
await browser.close()
process.exit(failed ? 1 : 0)
