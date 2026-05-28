import { DingTalkBrowserLogin } from './dingtalk-browser-login'
import { DingTalkInAppLogin } from './dingtalk-inapp-login'
import { isDingTalkContainer } from './dingtalk-env'

type DingTalkLoginPanelProps = {
  returnTo: string
  onAuthenticated: () => Promise<void> | void
  oauthFailed?: boolean
}

/**
 * DingTalk-only login surface: in-app requestAuthCode or browser OAuth redirect.
 */
export function DingTalkLoginPanel({ returnTo, onAuthenticated, oauthFailed }: DingTalkLoginPanelProps) {
  if (isDingTalkContainer()) {
    return <DingTalkInAppLogin onAuthenticated={onAuthenticated} />
  }

  return <DingTalkBrowserLogin returnTo={returnTo} oauthFailed={oauthFailed} />
}
