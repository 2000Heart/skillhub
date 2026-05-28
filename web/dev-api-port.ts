import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/**
 * Resolves the local backend port for Vite `/api` proxying.
 * Prefers SKILLHUB_DEV_API_PORT, then SERVER_PORT from `.dev/dingtalk.env`.
 */
export function resolveDevApiPort(): string {
  const fromEnv = process.env.SKILLHUB_DEV_API_PORT?.trim()
  if (fromEnv) {
    return fromEnv
  }

  const dingtalkEnvPath = path.join(repoRoot, '.dev', 'dingtalk.env')
  if (!existsSync(dingtalkEnvPath)) {
    return '8080'
  }

  const content = readFileSync(dingtalkEnvPath, 'utf8')
  const match = content.match(/^SERVER_PORT=(\d+)\s*$/m)
  return match?.[1] ?? '8080'
}
