// Reasoning: compara as medidas do diagrama (em unidades sem zoom) com as do protótipo: caixas, títulos, chips, vãos e filas.
//   node e2e/reasoning-gaps.mjs <url da UI> <url do protótipo>
import { chromium } from 'playwright-core'

const [ui, proto] = process.argv.slice(2)
const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })

async function reasoning(url, which) {
  const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
  await p.addInitScript(() => localStorage.setItem('mind-inspector:theme', 'dark'))
  await p.goto(url, { waitUntil: 'networkidle' })
  await p.waitForTimeout(2000)
  if (which === 0) {
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
  const r = await p.evaluate((w) => {
    const px = (v) => Math.round(parseFloat(v))
    if (w === 1) {
      const sc = document.querySelector('[data-k="reason"] .av-tracer-scroll')
      const canvas = sc.firstElementChild.firstElementChild
      const geo = (e) => ({ x: px(e.style.left), y: px(e.style.top), w: px(e.style.width), h: px(e.style.height) })
      const boxes = [...canvas.querySelectorAll('[data-stepbox]')].map(geo)
      const chips = [...canvas.querySelectorAll('.av-tchip')].map(geo).map((g, i) => ({ ...g, h: 24 }))
      const labels = [...canvas.querySelectorAll('[data-step]')].map(geo)
      const lanes = [...canvas.querySelectorAll('div')].filter((d) => d.style.height === '28px' && d.style.position === 'absolute').map(geo)
      const heads = [...canvas.querySelectorAll('div')].filter((d) => d.style.top === '6px' && d.style.height === '20px').map(geo)
      return { boxes, chips, labels, lanes, heads, canvasH: px(canvas.style.height) }
    }
    const canvas = document.querySelector('.mi-trcanvas')
    const cs = getComputedStyle(canvas)
    const pad = { l: px(cs.paddingLeft), t: px(cs.paddingTop) }
    const rel = (el) => {
      let x = pad.l, y = pad.t, n = el
      while (n && n !== canvas) { x += n.offsetLeft; y += n.offsetTop; n = n.offsetParent }
      return { x: x - (el.offsetParent === canvas ? 0 : 0), y, w: el.offsetWidth, h: el.offsetHeight }
    }
    // offsetLeft/Top já são relativos ao padding box do canvas; o padding entra no deslocamento
    const geo = (el) => { let x = 0, y = 0, n = el; while (n && n !== canvas) { x += n.offsetLeft; y += n.offsetTop; n = n.offsetParent } return { x, y, w: el.offsetWidth, h: el.offsetHeight } }
    return {
      boxes: [...canvas.querySelectorAll('.mi-stepbox')].map(geo),
      chips: [...canvas.querySelectorAll('.mi-stepbox .mi-tchip')].map(geo),
      labels: [...canvas.querySelectorAll('.mi-steptitle')].map(geo),
      lanes: [...canvas.querySelectorAll('.mi-lane')].map((l) => { const g = geo(l); return { ...g, x: g.x - g.w / 2 } }),
      heads: [...canvas.querySelectorAll('.mi-phasehead')].map(geo),
      canvasH: canvas.scrollHeight,
      rel: rel,
    }
  }, which)
  await p.close()
  return r
}

function summarize(r) {
  const o = {}
  const bx = r.boxes
  const row = bx.filter((x) => x.y === bx[0].y)
  o['topo das caixas'] = bx[0].y
  o['altura da caixa'] = bx[0].h
  o['vão entre caixas do mesmo grupo'] = bx[1].x - (bx[0].x + bx[0].w)
  const gaps = row.slice(1).map((x, i) => x.x - (row[i].x + row[i].w)).filter((g) => g > 40)
  o['vão entre fases'] = gaps[0]
  o['margem esquerda do primeiro passo'] = bx[0].x
  o['título: x dentro da caixa'] = r.labels[0].x - bx[0].x
  o['título: y dentro da caixa'] = r.labels[0].y - bx[0].y
  o['título: altura'] = r.labels[0].h
  const c = r.chips.filter((k) => k.x >= bx[0].x && k.x < bx[0].x + bx[0].w && k.y >= bx[0].y && k.y < bx[0].y + bx[0].h)
  o['chip: altura'] = c[0]?.h
  o['chip: x dentro da caixa'] = c[0] ? c[0].x - bx[0].x : null
  o['chip: y dentro da caixa'] = c[0] ? c[0].y - bx[0].y : null
  const col = r.chips.filter((k) => bx[2] && k.x >= bx[3]?.x && k.x < bx[3]?.x + bx[3]?.w && k.y >= bx[3].y && k.y < bx[3].y + bx[3].h)
  o['chip: passo vertical'] = col.length > 1 ? col[1].y - col[0].y : null
  o['cabeçalho da fase: y'] = r.heads[0]?.y
  o['cabeçalho da fase: altura'] = r.heads[0]?.h
  const lane = r.lanes[0]
  o['fila: y abaixo das caixas'] = lane ? lane.y - (bx[0].y + bx[0].h) : null
  o['fila: altura'] = lane?.h
  return o
}

const mine = summarize(await reasoning(ui, 0))
const ref = summarize(await reasoning(proto, 1))
let diffs = 0
for (const k of Object.keys(ref)) {
  const ok = Math.abs((mine[k] ?? NaN) - (ref[k] ?? NaN)) <= 1 || mine[k] === ref[k]
  if (!ok) diffs++
  console.log(`${ok ? 'ok  ' : 'DIFF'} ${k.padEnd(36)} ui=${mine[k]}  prototype=${ref[k]}`)
}
console.log(diffs ? `${diffs} diferença(s)` : 'o diagrama do Reasoning confere com o protótipo')
await b.close()
process.exit(diffs ? 1 : 0)
