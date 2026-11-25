// Espaçamento: compara padding, margin e gap calculados de pares de elementos da UI e do protótipo.
//   node e2e/spacing.mjs <url da UI> <url do protótipo>
import { chromium } from 'playwright-core'
import { existsSync } from 'node:fs'

const [ui, proto] = process.argv.slice(2)
const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', '/usr/bin/google-chrome'].find(existsSync)

/** [nome, seletor na UI, seletor no protótipo]; `>> nth=N` escolhe o N-ésimo. */
const PAIRS = [
  ['header', '.mi-header', 'header'],
  ['header botão', '.mi-header .hd-btn', 'header .hd-btn'],
  ['barra de cima', '.mi-bar', 'main.av-agent > div:first-child'],
  ['barra lateral esquerda', '.mi-rail-l', 'aside.av-rail >> nth=0'],
  ['barra lateral direita', '.mi-rail-r', 'aside[aria-label="Sidebar"]'],
  ['lista de agentes (corpo)', '.mi-agside > div:nth-child(2)', '.av-agside > div:nth-child(2)'],
  ['linha de agente', '.mi-agside .mi-row', '.av-agside .av-row'],
  ['painel código: cabeçalho', 'section[data-k="code"] .mi-ph', 'section[data-k="code"] .av-ph'],
  ['barra de intenções (runbar)', '.mi-runbar', '.av-runbar'],
  ['runbar interno', '.mi-runbar-in', '.av-runbar > div'],
  ['editor', '.mi-editor', '.av-editor'],
  ['linha do editor', '.mi-eline', '.av-eline'],
  ['número da linha', '.mi-enum', '.av-enum'],
  ['código da linha', '.mi-ecode', '.av-ecode'],
  ['variáveis', '.mi-vars', '.av-vars'],
  ['variáveis: cabeçalho', '.mi-vhead', '.av-vhead'],
  ['variável: grupo', '.mi-vgroup', '.av-vgroup'],
  ['variável: linha', '.mi-vrow', '.av-vrow'],
  ['intenções: corpo', 'section[data-k="int"] .mi-body-scroll', 'section[data-k="int"] .av-ph + div'],
  ['seção (cabeçalho)', 'section[data-k="int"] .mi-sechead', 'section[data-k="int"] .av-sechead'],
  ['seção: conteúdo recuado', 'section[data-k="int"] .mi-indent', 'section[data-k="int"] .av-ph + div > div:nth-child(2)'],
  ['crenças: seção', 'section[data-k="bel"] .mi-sechead', 'section[data-k="bel"] .av-sechead'],
  ['crenças: chips (contêiner)', 'section[data-k="bel"] .mi-chips', 'section[data-k="bel"] .av-ph + div div[style*="flex-wrap"]'],
  ['chip', 'section[data-k="bel"] .mi-bchip', 'section[data-k="bel"] .av-bchip'],
  ['abas (contêiner)', 'section[data-k="console"] .mi-tabs', 'section[data-k="console"] .av-tabs'],
  ['aba', 'section[data-k="console"] .mi-tabbtn', 'section[data-k="console"] .av-tabbtn'],
  ['console: corpo', 'section[data-k="console"] .mi-code', 'section[data-k="console"] .av-code'],
  ['console: linha', '.mi-logrow', 'section[data-k="console"] .av-code > div'],
  ['botão de ícone', '.mi-rail-l .mi-icon', 'aside.av-rail .av-icon'],
  ['controles de ciclo (grupo)', '.mi-cycgroup', 'button[aria-label="First cycle"] >> parent'],
  ['controles de ciclo (botão)', '.mi-cycgroup .mi-icon', 'button[aria-label="First cycle"]'],
  ['número do ciclo', '.mi-cycinfo', 'button[aria-label="Go to a specific cycle"] >> parent'],
]

async function snap(url, which, pairs, prep) {
  const browser = await chromium.launch({ executablePath, headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  await page.addInitScript(() => localStorage.setItem('mind-inspector:theme', 'dark'))
  await page.goto(url, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2500)
  if (prep) await prep(page, which)
  const out = {}
  for (const pair of pairs) {
    const [css, nth] = pair[1 + which].split(' >> ')
    out[pair[0]] = await page.evaluate(([sel, n]) => {
      let el = document.querySelectorAll(sel)[n?.startsWith('nth=') ? Number(n.slice(4)) : 0]
      if (el && n === 'parent') el = el.parentElement
      if (!el) return null
      const c = getComputedStyle(el)
      return { padding: c.padding, margin: c.margin, gap: c.gap === 'normal' ? '0px' : c.gap }
    }, [css, nth])
  }
  await browser.close()
  return out
}

/** Leva a UI a um ciclo em que o agente tem intenção, para os mesmos painéis existirem nos dois lados. */
async function withIntentions(page, which) {
  if (which === 1) return
  for (let n = 3; n < 60; n += 3) {
    await page.locator('.mi-cyc .mi-cyclink').click()
    await page.locator('.mi-cycinput').fill(String(n))
    await page.keyboard.press('Enter')
    await page.waitForTimeout(700)
    if (await page.locator('.mi-runbar').count()) return
  }
}

const SCENARIOS = [
  ['tela principal', PAIRS, withIntentions],
  ['Messages', [
    ['contatos', '.mi-contacts', 'nav[aria-label="Contacts"]'],
    ['contato', '.mi-contacts .mi-row', 'nav[aria-label="Contacts"] .av-row'],
    ['título da conversa', '.mi-contacts + div > div:first-child', 'nav[aria-label="Contacts"] + div > div:first-child'],
  ], async (page, w) => {
    await page.locator(w ? 'section[data-k="console"] .av-tabbtn:has-text("Messages")' : 'section[data-k="console"] [data-tab="msgs"] .mi-tabbtn').click()
    await page.waitForTimeout(800)
  }],
  ['Settings', [
    ['diálogo', '.mi-settings', '[role="dialog"][aria-label="Settings"]'],
    ['cabeçalho', '.mi-settings .mi-ph', '[role="dialog"][aria-label="Settings"] .av-ph'],
    ['corpo', '.mi-setbody', '[role="dialog"][aria-label="Settings"] > div:nth-child(2)'],
    ['linha do tema', '.mi-setbody .mi-setrow', '[role="dialog"][aria-label="Settings"] > div:nth-child(2) > div:first-child'],
  ], async (page, w) => {
    await page.locator(w ? 'header button[aria-label="Settings"]' : '.hd-btn[aria-label="Settings"]').click()
    await page.waitForTimeout(500)
  }],
  ['Legend', [
    ['diálogo', '.mi-legend', '[role="dialog"][aria-label="Legend"]'],
    ['cabeçalho', '.mi-legend > div:first-child', '[role="dialog"][aria-label="Legend"] > div:first-child'],
    ['lista', '.mi-legend > div:nth-child(2)', '[role="dialog"][aria-label="Legend"] > div:nth-child(2)'],
    ['item', '.mi-legend > div:nth-child(2) > div', '[role="dialog"][aria-label="Legend"] > div:nth-child(2) > div'],
  ], async (page, w) => {
    await page.locator(w ? 'header button[aria-label="Help"]' : '.hd-btn[aria-label="Help"]').click()
    await page.locator('[role="menuitem"]:has-text("Legend")').click()
    await page.waitForTimeout(400)
  }],
  ['busca nos ciclos', [
    ['caixa', '[role="search"]', '[role="search"]'],
    ['campo', '.mi-tlq', '.av-tlq'],
    ['botão', '[role="search"] .mi-zbtn', '[role="search"] .av-zbtn'],
  ], async (page, w) => {
    await page.locator(w ? 'aside[aria-label="Timeline tools"] button' : '[data-tour="tools"] button').first().click()
    await page.waitForTimeout(400)
  }],
]

let diffs = 0
for (const [scenario, pairs, prep] of SCENARIOS) {
  const mine = await snap(ui, 0, pairs, prep)
  const ref = await snap(proto, 1, pairs, prep)
  console.log(`== ${scenario}`)
  for (const pair of pairs) {
    const name = pair[0]
    const a = mine[name]
    const b = ref[name]
    if (!a || !b) {
      console.log(`?    ${name.padEnd(34)} ${!a ? 'ausente na UI' : 'ausente no protótipo'}`)
      continue
    }
    for (const prop of ['padding', 'margin', 'gap']) {
      if (a[prop] === b[prop]) continue
      diffs++
      console.log(`DIFF ${name.padEnd(34)} ${prop.padEnd(8)} ui=${a[prop]}  prototype=${b[prop]}`)
    }
  }
}
console.log(diffs ? `${diffs} diferença(s) de espaçamento` : 'espaçamentos conferem')
process.exit(diffs ? 1 : 0)
