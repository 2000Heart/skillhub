import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '@/api/client'

export function useOrgDepartments() {
  return useQuery({
    queryKey: ['admin', 'org', 'departments'],
    queryFn: () => adminApi.listOrgDepartments(),
  })
}

export function useOrgUsers() {
  return useQuery({
    queryKey: ['admin', 'org', 'users'],
    queryFn: () => adminApi.listOrgUsers(),
  })
}

export function useOrgSyncStatus() {
  return useQuery({
    queryKey: ['admin', 'org', 'sync-status'],
    queryFn: () => adminApi.getOrgSyncStatus(),
    refetchInterval: 30000,
  })
}

export function useRunOrgFullSync() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => adminApi.runOrgFullSync(),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin', 'org', 'sync-status'] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'org', 'departments'] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'org', 'users'] }),
      ])
    },
  })
}

export function useBootstrapOrgPermissions() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dryRun: boolean) => adminApi.bootstrapOrgPermissions(dryRun),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
  })
}
