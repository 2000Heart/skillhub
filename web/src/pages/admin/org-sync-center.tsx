import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { useBootstrapOrgPermissions, useOrgSyncStatus, useRunOrgFullSync } from '@/features/admin/use-org-sync'

export function OrgSyncCenterPage() {
  const { t } = useTranslation()
  const { data, isLoading } = useOrgSyncStatus()
  const fullSyncMutation = useRunOrgFullSync()
  const bootstrapMutation = useBootstrapOrgPermissions()

  return (
    <div className="space-y-6 animate-fade-up">
      <div>
        <h1 className="text-3xl font-bold font-heading mb-2">{t('orgSync.title', '组织同步中心')}</h1>
        <p className="text-muted-foreground">{t('orgSync.subtitle', '管理钉钉组织全量同步、权限初始化和状态巡检')}</p>
      </div>

      <Card className="p-6 space-y-3">
        <h2 className="font-semibold">{t('orgSync.status', '同步状态')}</h2>
        {isLoading ? <p className="text-sm text-muted-foreground">Loading...</p> : (
          <div className="space-y-1 text-sm">
            <p>Phase: {data?.phase ?? 'NEVER_RUN'}</p>
            <p>Success: {String(Boolean(data?.success))}</p>
            <p>Last Run: {data?.lastRunAt ?? '-'}</p>
            <p>Error: {data?.lastError ?? '-'}</p>
          </div>
        )}
        <div className="flex gap-3 pt-2">
          <Button onClick={() => fullSyncMutation.mutate()} disabled={fullSyncMutation.isPending}>
            {t('orgSync.runFullSync', '立即全量同步')}
          </Button>
          <Button
            variant="outline"
            onClick={() => bootstrapMutation.mutate(false)}
            disabled={bootstrapMutation.isPending}
          >
            {t('orgSync.bootstrapPermissions', '执行权限补齐')}
          </Button>
          <Button
            variant="outline"
            onClick={() => bootstrapMutation.mutate(true)}
            disabled={bootstrapMutation.isPending}
          >
            {t('orgSync.previewBootstrap', '预览补齐数量')}
          </Button>
        </div>
        {bootstrapMutation.data !== undefined ? (
          <p className="text-xs text-muted-foreground">
            {t('orgSync.bootstrapCount', '本次处理用户数')}: {bootstrapMutation.data}
          </p>
        ) : null}
      </Card>
    </div>
  )
}
