import { useTranslation } from 'react-i18next'
import { Card } from '@/shared/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/shared/ui/table'
import { useOrgUsers } from '@/features/admin/use-org-sync'

export function OrgUsersPage() {
  const { t } = useTranslation()
  const { data, isLoading } = useOrgUsers()

  return (
    <div className="space-y-6 animate-fade-up">
      <div>
        <h1 className="text-3xl font-bold font-heading mb-2">{t('orgUsers.title', '组织用户')}</h1>
        <p className="text-muted-foreground">{t('orgUsers.subtitle', '展示全量同步用户（含未登录用户）')}</p>
      </div>
      <Card>
        {isLoading ? <div className="p-6 text-sm text-muted-foreground">Loading...</div> : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>User ID</TableHead>
                <TableHead>{t('orgUsers.name', '姓名')}</TableHead>
                <TableHead>{t('orgUsers.department', '主部门')}</TableHead>
                <TableHead>{t('orgUsers.active', '在职')}</TableHead>
                <TableHead>{t('orgUsers.permissionSource', '权限来源')}</TableHead>
                <TableHead>{t('orgUsers.roles', '钉钉角色快照')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(data ?? []).map((user) => (
                <TableRow key={user.userId}>
                  <TableCell className="font-mono text-xs">{user.userId}</TableCell>
                  <TableCell>{user.name}</TableCell>
                  <TableCell>{user.primaryDeptId ?? '-'}</TableCell>
                  <TableCell>{String(user.active)}</TableCell>
                  <TableCell>{user.permissionSource ?? '-'}</TableCell>
                  <TableCell>{user.dingtalkRoles.join(', ') || '-'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Card>
    </div>
  )
}
