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

export async function getAuditLogs(page = 0, size = 10) {
  const response = await api.get<PageResponse<AuditLogResponse>>('/audit-logs', {
    params: {
      page,
      size,
      sort: 'id,desc',
    },
  })

  return response.data
}