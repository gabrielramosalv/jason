/** Colorização leve de AgentSpeak para o painel de código: comentário, texto, gatilho, variável, número e operador. */
export type TokenKind = 'comment' | 'string' | 'trigger' | 'variable' | 'number' | 'operator' | 'plain'

export interface Token {
  text: string
  kind: TokenKind
}

const PATTERN = /(\/\/.*$)|("(?:[^"\\]|\\.)*")|([+-]{1,2}[!?]?[a-z_~][\w.]*|![a-z_~][\w.]*)|(\b[A-Z_]\w*\b)|(\b\d+(?:\.\d+)?\b)|(<-|:|&|\||\bnot\b|==|\\==|<=|>=|<|>|=\.\.|=)/g

export function tokenize(line: string): Token[] {
  const out: Token[] = []
  let last = 0
  for (const m of line.matchAll(PATTERN)) {
    const at = m.index ?? 0
    if (at > last) out.push({ text: line.slice(last, at), kind: 'plain' })
    const kind: TokenKind = m[1] ? 'comment' : m[2] ? 'string' : m[3] ? 'trigger' : m[4] ? 'variable' : m[5] ? 'number' : 'operator'
    out.push({ text: m[0], kind })
    last = at + m[0].length
  }
  if (last < line.length) out.push({ text: line.slice(last), kind: 'plain' })
  return out
}

export const TOKEN_COLOR: Record<TokenKind, string> = {
  comment: 'var(--c-8a8f99)',
  string: 'var(--c-a31515)',
  trigger: 'var(--c-0000ff)',
  variable: 'var(--c-001080)',
  number: 'var(--c-098658)',
  operator: 'var(--c-af00db)',
  plain: 'inherit',
}
