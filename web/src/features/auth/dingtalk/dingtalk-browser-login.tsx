import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui/button'
import { getDingTalkRuntimeConfig } from '@/api/client'

type DingTalkBrowserLoginProps = {
  returnTo: string
  oauthFailed?: boolean
}

function buildAuthorizeUrl(returnTo: string): string {
  const params = new URLSearchParams()
  if (returnTo) {
    params.set('returnTo', returnTo)
  }
  const query = params.toString()
  return `/api/v1/auth/dingtalk/oauth/authorize${query ? `?${query}` : ''}`
}

/**
 * Browser fallback for DingTalk mode: auto-redirect to SkillHub DingTalk OAuth.
 */
export function DingTalkBrowserLogin({ returnTo, oauthFailed }: DingTalkBrowserLoginProps) {
  const { t } = useTranslation()
  const runtime = getDingTalkRuntimeConfig()
  const redirectedRef = useRef(false)

  useEffect(() => {
    if (!runtime.enabled || oauthFailed || redirectedRef.current) {
      return
    }
    redirectedRef.current = true
    window.location.assign(buildAuthorizeUrl(returnTo))
  }, [oauthFailed, returnTo, runtime.enabled])

  if (!runtime.enabled) {
    return null
  }

  if (oauthFailed) {
    return (
      <div className="space-y-4 text-center">
        <p className="text-sm text-red-600">钉钉登录失败，请重试</p>
        <Button
          className="w-full"
          type="button"
          onClick={() => {
            window.location.assign(buildAuthorizeUrl(returnTo))
          }}
        >
          使用钉钉登录
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-3 text-center py-4">
      <p className="text-sm text-muted-foreground">
        {t('login.dingtalkRedirecting', { defaultValue: '正在跳转钉钉登录…' })}
      </p>
      <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
    </div>
  )
}
