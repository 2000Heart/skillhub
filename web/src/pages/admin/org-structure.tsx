import { useTranslation } from 'react-i18next'
import { Card } from '@/shared/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/shared/ui/table'
import { useOrgDepartments } from '@/features/admin/use-org-sync'

export function OrgStructurePage() {
  const { t } = useTranslation()
  const { data, isLoading } = useOrgDepartments()

  return (
    <div className="space-y-6 animate-fade-up">
      <div>
        <h1 className="text-3xl font-bold font-heading mb-2">{t('orgStructure.title', '组织架构')}</h1>
        <p className="text-muted-foreground">{t('orgStructure.subtitle', '展示钉钉同步后的部门树快照')}</p>
      </div>
      <Card>
        {isLoading ? <div className="p-6 text-sm text-muted-foreground">Loading...</div> : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Dept ID</TableHead>
                <TableHead>Parent</TableHead>
                <TableHead>{t('orgStructure.name', '名称')}</TableHead>
                <TableHead>{t('orgStructure.order', '排序')}</TableHead>
                <TableHead>{t('orgStructure.syncedAt', '同步时间')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(data ?? []).map((dept) => (
                <TableRow key={dept.deptId}>
                  <TableCell>{dept.deptId}</TableCell>
                  <TableCell>{dept.parentDeptId ?? '-'}</TableCell>
                  <TableCell>{dept.name}</TableCell>
                  <TableCell>{dept.order ?? '-'}</TableCell>
                  <TableCell>{dept.lastSyncAt ?? '-'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Card>
    </div>
  )
}
