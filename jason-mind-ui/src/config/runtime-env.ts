/**
 * Configuração pública da UI. Em produção vem de /env-config.js (servido junto da UI);
 * em desenvolvimento, do .env do Vite. Nada aqui é segredo.
 */
export const PUBLIC_ENV_KEYS = ['VITE_API_BASE_URL'] as const

export type PublicEnvKey = (typeof PUBLIC_ENV_KEYS)[number]

export function publicEnv(key: PublicEnvKey): string | undefined {
  const runtime = (window as unknown as { _env_?: Record<string, string> })._env_?.[key]
  if (typeof runtime === 'string' && runtime !== '') return runtime
  const buildTime = (import.meta.env as Record<string, string | undefined>)[key]
  return buildTime === '' ? undefined : buildTime
}
