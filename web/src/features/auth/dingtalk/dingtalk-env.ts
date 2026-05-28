import { getDingTalkRuntimeConfig } from '@/api/client'

export function isDingTalkContainer(): boolean {
  if (typeof navigator === 'undefined') {
    return false
  }
  return /DingTalk/i.test(navigator.userAgent)
}

export function resolveDingTalkCorpId(searchParams?: URLSearchParams): string | null {
  try {
    const params = searchParams ?? new URLSearchParams(window.location.search)
    const fromUrl = params.get('corpid')?.trim()
    if (fromUrl) {
      return fromUrl
    }
  } catch {
    // ignore malformed URL
  }

  const configured = getDingTalkRuntimeConfig().defaultCorpId?.trim()
  return configured || null
}
