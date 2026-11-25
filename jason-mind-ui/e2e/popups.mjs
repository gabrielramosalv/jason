// Popups e diálogos: abre cada um e confere o fundo opaco, o contraste com o texto, a posição dentro da tela e o empilhamento.
//   node e2e/popups.mjs <url> [tema: dark|light]
import { chromium } from 'playwright-core'
import { existsSync } from 'node:fs'

const base = process.argv[2] ?? 'http://127.0.0.1:5173'
const theme = process.argv[3] ?? 'dark'
const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe', 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', '/usr/bin/google-chrome'].find(existsSync)
const b = await chromium.launch({ executablePath, headless: true })
const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
await p.addInitScript((t) => localStorage.setItem('mind-inspector:theme', t), theme)
const errors = []
p.on('pageerror', (e) => errors.push(String(e)))
await p.goto(base, { waitUntil: 'networkidle' })
await p.waitForTimeout(2500)

let failures = 0
/** Mede um popup: fundo opaco, dentro da tela e, no ponto central dele, o elemento do topo é ele mesmo (não está coberto). */
async function inspect(name, selector) {
  const r = await p.evaluate((sel) => {
    const el = document.querySelector(sel)
    if (!el) return null
    const cs = getComputedStyle(el)
    const rect = el.getBoundingClientRect()
    const m = cs.backgroundColor.match(/rgba?\(([^)]+)\)/)?.[1].split(',').map(Number) ?? []
    const alpha = m.length === 4 ? m[3] : 1
    const top = document.elementFromPoint(rect.left + rect.width / 2, rect.top + Math.min(rect.height - 2, 14))
    const text = getComputedStyle(el).color
    return { bg: cs.backgroundColor, alpha, text, left: Math.round(rect.left), top: Math.round(rect.top), right: Math.round(rect.right), bottom: Math.round(rect.bottom), covered: !(top && (el === top || el.contains(top))), z: cs.zIndex, root: !!el.closest('.mi-root') }
  }, selector)
  if (!r) {
    failures++
    console.log(`FAIL ${name}: não abriu (${selector})`)
    return
  }
  const issues = []
  if (r.alpha < 0.95) issues.push(`fundo transparente (${r.bg})`)
  if (r.bg === r.text) issues.push('texto da cor do fundo')
  if (r.left < -1 || r.top < -1 || r.right > 1441 || r.bottom > 901) issues.push(`fora da tela (${r.left},${r.top},${r.right},${r.bottom})`)
  if (r.covered) issues.push('coberto por outro elemento')
  if (!r.root) issues.push('fora do contêiner do tema')
  if (issues.length) failures++
  console.log(`${issues.length ? 'FAIL' : 'ok  '} ${name.padEnd(34)} fundo=${r.bg} z=${r.z}${issues.length ? '  ' + issues.join('; ') : ''}`)
}
const close = async () => {
  await p.keyboard.press('Escape')
  await p.mouse.click(700, 4)
  await p.waitForTimeout(250)
}

await p.locator('.hd-btn[aria-label="Help"]').click()
await inspect('menu Help', '.mi-menu')
await p.locator('[role="menuitem"]:has-text("Legend")').click()
await inspect('Legend', '.mi-legend')
await p.locator('[aria-label="Close legend"]').click()
await p.locator('.hd-btn[aria-label="Help"]').click()
await p.locator('[role="menuitem"]:has-text("Tutorial")').click()
await inspect('cartão do tour', '[data-tut="card"]')
await p.keyboard.press('Escape')
await p.locator('.hd-btn[aria-label="Settings"]').click()
await inspect('Settings', '.mi-settings')
await close()
await p.locator('button[aria-label="Agents"]').click()
await p.waitForTimeout(300)
await p.locator('button[aria-label="Switch agent"]').click()
await inspect('lista de agentes', '[role="listbox"]')
await close()
await p.locator('section[data-k="console"] [aria-label="Filter agents"]').click()
await inspect('filtro do Console', '.mi-menu2')
await close()
await p.locator('[data-tour="tools"] button').first().click()
await inspect('busca nos ciclos', '[role="search"]')
await p.locator('[aria-label="Minimize search"]').click()
await p.locator('section[data-k="bel"] .mi-belief').first().click()
await inspect('Life cycle', '[role="dialog"][aria-label="Life cycle"]')
await p.locator('[aria-label="Life cycle"] [aria-label="Close"]').click()
await p.locator('section[data-k="console"] [data-tab="reason"] .mi-tabbtn').click()
await p.waitForTimeout(800)
const chip = p.locator('.mi-stepbox button.mi-tchip[title="Chosen"], .mi-stepbox button.mi-tchip[title="Not chosen"]').first()
if (await chip.count()) {
  await chip.click()
  await inspect('Explanation', '[role="dialog"][aria-label="Explanation"]')
  await close()
}
const more = p.locator('.mi-stepbox button:has-text("Show more")').first()
if (await more.count()) {
  await more.click()
  await inspect('Show more (Reasoning)', '.mi-dialog[role="dialog"]:not([aria-label="Explanation"])')
  await close()
}
await p.locator('[data-tour="view-toggle"]').click()
await p.waitForTimeout(800)
await inspect('janela flutuante', '.mi-grid[data-fl="1"] section')
console.log(errors.length ? `erros de página: ${errors[0]}` : 'sem erro de página')
await b.close()
console.log(failures ? `${failures} problema(s) (tema ${theme})` : `popups e diálogos conferem (tema ${theme})`)
process.exit(failures ? 1 : 0)
