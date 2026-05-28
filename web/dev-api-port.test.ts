import { describe, expect, it } from 'vitest'
import { resolveDevApiPort } from './dev-api-port'

describe('resolveDevApiPort', () => {
  it('prefers SKILLHUB_DEV_API_PORT when set', () => {
    const previous = process.env.SKILLHUB_DEV_API_PORT
    process.env.SKILLHUB_DEV_API_PORT = '9090'
    try {
      expect(resolveDevApiPort()).toBe('9090')
    } finally {
      if (previous === undefined) {
        delete process.env.SKILLHUB_DEV_API_PORT
      } else {
        process.env.SKILLHUB_DEV_API_PORT = previous
      }
    }
  })

  it('reads SERVER_PORT from .dev/dingtalk.env when present', () => {
    const previous = process.env.SKILLHUB_DEV_API_PORT
    delete process.env.SKILLHUB_DEV_API_PORT
    try {
      expect(resolveDevApiPort()).toBe('8081')
    } finally {
      if (previous !== undefined) {
        process.env.SKILLHUB_DEV_API_PORT = previous
      }
    }
  })
})
