import { useEffect } from 'react'
import { useNavigate, useRouterState } from '@tanstack/react-router'
import { getDingTalkRuntimeConfig } from '@/api/client'
import { useAuth } from '@/features/auth/use-auth'
import { isDingTalkContainer } from './dingtalk-env'

/**
 * When opened inside DingTalk, unauthenticated users on public pages are sent to /login
 * where in-app requestAuthCode auto-login runs.
 */
export function DingTalkInAppAuthGate() {
  const { user, isLoading } = useAuth()
  const navigate = useNavigate()
  const { pathname, searchStr } = useRouterState({
    select: (state) => ({
      pathname: state.location.pathname,
      searchStr: state.location.searchStr,
    }),
  })
  const runtime = getDingTalkRuntimeConfig()

  useEffect(() => {
    if (!runtime.enabled || !runtime.autoLogin || isLoading || user) {
      return
    }
    if (!isDingTalkContainer()) {
      return
    }
    if (pathname === '/login' || pathname === '/register' || pathname === '/cli/auth') {
      return
    }

    const returnTo = `${pathname}${searchStr}`.startsWith('/') ? `${pathname}${searchStr}` : '/dashboard'
    void navigate({
      to: '/login',
      search: { returnTo },
    })
  }, [isLoading, navigate, pathname, runtime.autoLogin, runtime.enabled, searchStr, user])

  return null
}
