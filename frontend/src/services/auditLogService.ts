import { api } from './api'
import type { PageResponse } from './userService'

export type AuditLogResponse = {
  id: number
  actorUserId: number | null
  actorUserName: string | null
  action: string
  entityType: string
  entityId: number | null
  details: string | null
  createdAt: string
}

export type AuditLogFilters = {
  action?: string
  entityType?: string
  actorUserId?: number | null
  keyword?: string
}

export async function getAuditLogs(
  pageNumber = 0,
  pageSize = 10,
  filters: AuditLogFilters = {},
) {
  const response = await api.get<PageResponse<AuditLogResponse>>('/audit-logs', {
    params: {
      page: pageNumber,
      size: pageSize,
      sort: 'id,desc',
      action: filters.action || undefined,
      entityType: filters.entityType || undefined,
      actorUserId: filters.actorUserId || undefined,
      keyword: filters.keyword || undefined,
    },
  })

  return response.data
}