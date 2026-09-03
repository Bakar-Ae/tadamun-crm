import type { PermissionName } from './permissionService'
import type { RoleName } from './userService'
import { api } from './api'

export type Workspace = {
  organizationId: number
  membershipId: number
  name: string
  slug: string
  timeZone: string
  role: RoleName
  dataScope: 'ALL' | 'TEAM' | 'OWN'
  permissions: PermissionName[]
}

export async function getMyWorkspaces() {
  const response = await api.get<Workspace[]>('/workspaces')
  return response.data
}
