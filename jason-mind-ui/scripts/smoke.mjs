// Teste de fumaça de ponta a ponta contra um Mind API em execução com a UI empacotada (npm run smoke -- http://127.0.0.1:3272).
// Confere: a UI é servida na mesma origem, os estáticos saem com o tipo certo, a API responde e a guarda de origem das escritas (D-155) vale.
const base = (process.argv[2] ?? 'http://127.0.0.1:3272').replace(/\/$/, '')
let failures = 0

async function check(name, fn) {
  try {
    await fn()
    console.log(`ok   ${name}`)
  } catch (cause) {
    failures++
    console.log(`FAIL ${name}: ${cause.message}`)
  }
}
const expect = (cond, message) => {
  if (!cond) throw new Error(message)
}

await check('GET / devolve a página da UI', async () => {
  const res = await fetch(`${base}/`)
  const html = await res.text()
  expect(res.status === 200, `status ${res.status}`)
  expect(res.headers.get('content-type')?.startsWith('text/html'), 'content-type não é html')
  expect(html.includes('<div id="app">'), 'sem o ponto de montagem')
})

await check('os estáticos referenciados saem com o tipo certo', async () => {
  const html = await (await fetch(`${base}/`)).text()
  const assets = [...html.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map((m) => m[1])
  expect(assets.length > 0, 'nenhum asset no index.html')
  for (const a of assets) {
    const res = await fetch(base + a)
    expect(res.status === 200, `${a}: status ${res.status}`)
    const type = res.headers.get('content-type') ?? ''
    expect(a.endsWith('.js') ? type.includes('javascript') : a.endsWith('.css') ? type.includes('css') : true, `${a}: tipo ${type}`)
  }
})

let agent = null
await check('GET /api/mas lista os agentes', async () => {
  const body = await (await fetch(`${base}/api/mas`)).json()
  expect(Array.isArray(body.agents) && body.agents.length > 0, 'sem agentes')
  agent = body.agents[0].name
})

await check('o estado do agente traz as partes pedidas e responde 304 ao mesmo ETag', async () => {
  const url = `${base}/api/agents/${agent}?include=beliefs,intentions`
  const first = await fetch(url)
  expect(first.status === 200, `status ${first.status}`)
  const state = await first.json()
  expect('beliefs' in state && 'intentions' in state && !('events' in state), 'partes incorretas')
  const etag = first.headers.get('etag')
  expect(etag, 'sem ETag')
  const second = await fetch(url, { headers: { 'If-None-Match': etag } })
  expect(second.status === 304, `esperava 304, veio ${second.status}`)
})

await check('messages e console aceitam limit até 1000 e recusam acima (400)', async () => {
  expect((await fetch(`${base}/api/messages?limit=1000`)).status === 200, 'limit 1000')
  expect((await fetch(`${base}/api/messages?limit=1001`)).status === 400, 'limit 1001')
  expect((await fetch(`${base}/api/console?limit=1001`)).status === 400, 'console limit 1001')
})

await check('as escritas exigem JSON, o Host local e a origem do servidor (D-155)', async () => {
  const noJson = await fetch(`${base}/api/prune?until=0`, { method: 'POST' })
  expect(noJson.status === 415, `sem Content-Type: ${noJson.status}`)
  const evil = await fetch(`${base}/api/prune?until=0`, { method: 'POST', headers: { 'Content-Type': 'application/json', Origin: 'http://evil.example' } })
  expect(evil.status === 403, `Origin alheia: ${evil.status}`)
  const ok = await fetch(`${base}/api/prune?until=0`, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
  expect(ok.status === 202, `escrita válida: ${ok.status}`)
})

console.log(failures ? `${failures} falha(s)` : 'tudo certo')
process.exit(failures ? 1 : 0)
