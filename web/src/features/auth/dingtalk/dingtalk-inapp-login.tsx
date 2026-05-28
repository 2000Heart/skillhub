import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import * as dd from 'dingtalk-jsapi'
import { Button } from '@/shared/ui/button'
import { authApi, getDingTalkRuntimeConfig } from '@/api/client'
import { isDingTalkContainer, resolveDingTalkCorpId } from './dingtalk-env'

type DingTalkInAppLoginProps = {
  onAuthenticated: () => Promise<void> | void
}

export function DingTalkInAppLogin({ onAuthenticated }: DingTalkInAppLoginProps) {
  const { t } = useTranslation()
  const runtime = getDingTalkRuntimeConfig()
  const corpId = resolveDingTalkCorpId()
  const inDingTalk = isDingTalkContainer()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const attemptedRef = useRef(false)

  const canLogin = runtime.enabled && !!corpId && !!runtime.clientId

  const doLogin = useCallback(async () => {
    if (!canLogin || !runtime.clientId || !corpId) {
      return
    }
    setError(null)
    setLoading(true)
    dd.requestAuthCode({
      corpId,
      clientId: runtime.clientId,
      onSuccess: async (result: { code: string }) => {
        try {
          await authApi.dingtalkLogin({ code: result.code, corpId })
          await onAuthenticated()
        } catch (e) {
          console.error('DingTalk in-app login failed:', e)
          setError('钉钉免登失败，请重试')
        } finally {
          setLoading(false)
        }
      },
      onFail: (err: unknown) => {
        console.error('DingTalk requestAuthCode failed:', err)
        setError('钉钉授权失败，请在钉钉内重试')
        setLoading(false)
      },
    })
  }, [canLogin, corpId, onAuthenticated, runtime.clientId])

  useEffect(() => {
    if (!runtime.enabled || !runtime.autoLogin || attemptedRef.current) {
      return
    }
    if (!corpId || !runtime.clientId) {
      return
    }
    attemptedRef.current = true
    void doLogin()
  }, [corpId, doLogin, runtime.autoLogin, runtime.clientId, runtime.enabled])

  if (!runtime.enabled) {
    return null
  }

  if (inDingTalk && !canLogin) {
    return (
      <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
        钉钉免登缺少 corpId：请在微应用首页 URL 带上
        {' '}
        <code className="text-xs">?corpid=你的企业ID</code>
        ，或在 runtime-config 中配置 dingtalkDefaultCorpId。
      </div>
    )
  }

  if (runtime.autoLogin && loading && !error) {
    return (
      <div className="space-y-3 text-center py-4">
        <p className="text-sm text-muted-foreground">
          {t('login.dingtalkSigningIn', { defaultValue: '正在使用钉钉登录…' })}
        </p>
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!canLogin) {
    return null
  }

  return (
    <div className="space-y-4">
      {error ? <p className="text-sm text-red-600 text-center">{error}</p> : null}
      <Button
        className="w-full"
        type="button"
        disabled={loading}
        onClick={() => {
          void doLogin()
        }}
      >
        {loading ? '登录中...' : '使用钉钉登录'}
      </Button>
    </div>
  )
}

