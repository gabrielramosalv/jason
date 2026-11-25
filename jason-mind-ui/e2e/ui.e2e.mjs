// Teste de ponta a ponta no navegador (Chrome ou Edge instalados) contra um Mind API em execução com a UI empacotada.
//   npm run e2e -- http://127.0.0.1:3272
// O MAS de exemplo precisa ter mais de um agente e ciclos com intenções (o contract-net-protocol serve).
import { chromium } from 'playwright-core'
import { existsSync } from 'node:fs'

const base = (process.argv[2] ?? 'http://127.0.0.1:3272').replace(/\/$/, '')
const CANDIDATES = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/usr/bin/google-chrome',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
]
const executablePath = process.env.BROWSER_PATH ?? CANDIDATES.find(existsSync)
if (!executablePath) throw new Error('nenhum Chrome ou Edge encontrado: defina BROWSER_PATH')

const browser = await chromium.launch({ executablePath, headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
const consoleErrors = []
page.on('console', (m) => m.type() === 'error' && !m.text().includes('410') && consoleErrors.push(m.text()))
page.on('pageerror', (e) => consoleErrors.push(String(e)))

let failures = 0
async function step(name, fn) {
  try {
    await fn()
    console.log(`ok   ${name}`)
  } catch (cause) {
    failures++
    console.log(`FAIL ${name}: ${cause.message.split('\n')[0]}`)
  }
}
const expect = (cond, message) => {
  if (!cond) throw new Error(message)
}
const text = (sel) => page.locator(sel).first().innerText()

async function goToCycle(n) {
  await page.locator('.mi-cyc .mi-cyclink').click()
  await page.locator('.mi-cycinput').fill(String(n))
  await page.keyboard.press('Enter')
  await page.waitForFunction((x) => (document.querySelector('.mi-cycinfo')?.innerText ?? '').trim().startsWith(String(x)), n)
}

await page.goto(base, { waitUntil: 'networkidle' })
await page.evaluate(() => localStorage.clear())
await page.reload({ waitUntil: 'networkidle' })

await step('a UI carrega com o nome do MAS e os agentes', async () => {
  await page.waitForSelector('.mi-agside button.mi-row')
  expect((await page.locator('.mi-header').innerText()).includes('Mind Inspector'), 'sem título')
  expect((await page.locator('.mi-agside button.mi-row').count()) >= 2, 'poucos agentes')
  expect((await text('.mi-header h1')).length > 0, 'sem nome do MAS')
})

await step('o layout padrão tem código, intenções com Events, crenças e o console com abas', async () => {
  for (const k of ['code', 'int', 'bel', 'console']) expect(await page.locator(`section[data-k="${k}"]`).count(), `falta ${k}`)
  expect((await page.locator('section[data-k="console"] .mi-tabbtn').count()) === 3, 'abas do console')
  expect((await page.locator('section[data-k="int"] .mi-tabbtn').count()) === 2, 'abas de intenções')
})

await step('navegar a um ciclo antigo congela a tela e "ao vivo" volta', async () => {
  await goToCycle(20)
  expect((await page.locator('.mi-live').count()) === 0, 'ainda mostra Real time')
  expect((await text('.mi-cycinfo')).includes('20'), 'ciclo 20 não aparece')
  await page.locator('[aria-label="Go to the last cycle"]').click()
  await page.waitForSelector('.mi-live')
})

await step('no ciclo 20 do agente c há intenção executando, linha destacada e variáveis', async () => {
  await goToCycle(20)
  await page.waitForSelector('section[data-k="int"] .mi-sechead')
  expect((await text('section[data-k="int"]')).includes('Executing'), 'sem Executing')
  expect(await page.locator('.mi-eline[data-cur="true"]').count(), 'sem linha atual')
  expect(await page.locator('.mi-vars .mi-vrow').count(), 'sem variáveis')
})

await step('Reasoning mostra as três fases e os nove passos', async () => {
  await page.locator('section[data-k="console"] [data-tab="reason"] .mi-tabbtn').click()
  await page.waitForSelector('.mi-phase')
  expect((await page.locator('.mi-phase').count()) === 3, 'fases')
  expect((await page.locator('.mi-stepbox').count()) === 9, `passos: ${await page.locator('.mi-stepcol').count()}`)
  expect((await page.locator('.mi-lane').count()) === 2, 'filas')
})

await step('clicar num candidato abre a explicação e um chip abre o ciclo de vida', async () => {
  const chip = page.locator('.mi-stepbox button.mi-tchip').first()
  await chip.click()
  await page.waitForSelector('[role="dialog"]')
  await page.keyboard.press('Escape')
  await page.mouse.click(5, 5)
  await page.locator('section[data-k="int"] .mi-belief').first().click()
  await page.waitForSelector('[aria-label="Life cycle"]')
  await page.locator('[aria-label="Life cycle"] [aria-label="Close"]').click()
})

await step('arrastar a aba Messages para o painel Beliefs a move de grupo', async () => {
  await page.locator('[aria-label="Reset layout"]').click()
  const tab = page.locator('section[data-k="console"] [data-tab="msgs"] .mi-tabbtn')
  const target = page.locator('section[data-k="bel"]')
  const a = await tab.boundingBox()
  const b = await target.boundingBox()
  await page.mouse.move(a.x + 10, a.y + 10)
  await page.mouse.down()
  await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2, { steps: 8 })
  expect(await page.locator('.mi-drophint').count(), 'sem dica de destino')
  await page.mouse.up()
  expect((await page.locator('section[data-k="msgs"] .mi-tabbtn').count()) === 2, 'Messages não entrou no grupo de Beliefs')
  await page.locator('[aria-label="Reset layout"]').click()
})

await step('esconder e mostrar um painel pela barra direita', async () => {
  await page.locator('.mi-rail-r [aria-label="Beliefs"]').click()
  expect((await page.locator('section[data-k="bel"]').count()) === 0, 'Beliefs continua visível')
  await page.locator('.mi-rail-r [aria-label="Beliefs"]').click()
  expect((await page.locator('section[data-k="bel"]').count()) === 1, 'Beliefs não voltou')
})

await step('a busca encontra resultados e navega entre eles', async () => {
  await page.locator('[data-tour="tools"] button').first().click()
  await page.locator('.mi-tlq').fill('cnp')
  await page.waitForFunction(() => /\d+ of \d+/.test(document.querySelector('[role="search"]')?.innerText ?? ''), null, { timeout: 8000 })
  const before = await text('.mi-cycinfo')
  await page.locator('[aria-label="Next result"]').click()
  await page.waitForTimeout(600)
  expect((await text('[role="search"]')).includes(' of '), 'sem contador')
  void before
})

await step('a Timeline mostra a faixa, as janelas flutuantes e as travas de eixo', async () => {
  await page.locator('[data-tour="view-toggle"]').click()
  await page.waitForSelector('.mi-tlcell')
  expect((await page.locator('.mi-tlcell').count()) > 5, 'faixa vazia')
  expect(await page.locator('.mi-grid[data-fl="1"] section').count(), 'sem janelas flutuantes')
  await page.locator('[aria-label="Lock the horizontal axis"]').click()
  expect((await page.locator('[aria-label="Unlock the horizontal axis"][aria-pressed="true"]').count()) === 1, 'trava X')
  await page.locator('[data-tour="view-toggle"]').click()
  await page.waitForSelector('.mi-grid:not([data-fl="1"])')
})

await step('Settings troca o tema e confirma a poda antes de aplicar', async () => {
  await page.locator('.hd-btn[aria-label="Settings"]').click()
  const before = await page.locator('.mi-root').getAttribute('data-theme')
  await page.locator('[role="switch"][aria-label="Theme"]').click()
  expect((await page.locator('.mi-root').getAttribute('data-theme')) !== before, 'tema não mudou')
  await page.locator('input[aria-label="Keep from cycle"]').fill('50')
  await page.locator('button:has-text("Prune…")').click()
  expect(await page.locator('text=This cannot be undone').count(), 'sem confirmação')
  await page.locator('button:has-text("Cancel")').last().click()
  await page.locator('[role="switch"][aria-label="Theme"]').click()
  await page.locator('.hd-btn[aria-label="Settings"]').click()
})

await step('o tour guiado anda pelos passos e fecha com Esc', async () => {
  await page.locator('.hd-btn[aria-label="Help"]').click()
  await page.locator('[role="menuitem"]:has-text("Tutorial")').click()
  await page.waitForSelector('[data-tut="card"]')
  expect((await text('[data-tut="card"]')).includes('1 / 67'), 'não são 67 passos')
  await page.keyboard.press('ArrowRight')
  await page.keyboard.press('ArrowRight')
  expect((await text('[data-tut="card"]')).includes('3 / 67'), 'não avançou')
  await page.keyboard.press('Escape')
  expect((await page.locator('[data-tut="card"]').count()) === 0, 'não fechou')
})

await step('a legenda abre pelo menu Help', async () => {
  await page.locator('.hd-btn[aria-label="Help"]').click()
  await page.locator('[role="menuitem"]:has-text("Legend")').click()
  await page.waitForSelector('[aria-label="Legend"]')
  await page.locator('[aria-label="Close legend"]').click()
})

await step('não houve erro de console nem exceção na página', async () => {
  expect(consoleErrors.length === 0, consoleErrors.slice(0, 2).join(' | '))
})

await browser.close()
console.log(failures ? `${failures} falha(s)` : 'tudo certo')
process.exit(failures ? 1 : 0)
