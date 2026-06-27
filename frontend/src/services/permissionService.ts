import { api } from './api'
import type { RoleName } from './userService'

export type PermissionName =
  | 'USER_VIEW'
  | 'USER_CREATE'
  | 'USER_UPDATE'
  | 'USER_DEACTIVATE'
  | 'USER_ROLE_CHANGE'
  | 'CUSTOMER_VIEW'
  | 'CUSTOMER_CREATE'
  | 'CUSTOMER_UPDATE'
  | 'CUSTOMER_ARCHIVE'
  | 'LEAD_VIEW'
  | 'LEAD_CREATE'
  | 'LEAD_UPDATE'
  | 'LEAD_CONVERT'
  | 'LEAD_ARCHIVE'
  | 'CONTACT_VIEW'
  | 'CONTACT_CREATE'
  | 'CONTACT_UPDATE'
  | 'CONTACT_ARCHIVE'
  | 'TASK_VIEW'
  | 'TASK_CREATE'
  | 'TASK_UPDATE'
  | 'TASK_ASSIGN'
  | 'TASK_COMPLETE'
  | 'NOTE_VIEW'
  | 'NOTE_CREATE'
  | 'NOTE_UPDATE'
  | 'DASHBOARD_VIEW'
  | 'REPORT_VIEW'
  | 'REPORT_EXPORT'
  | 'AUDIT_LOG_VIEW'
  | 'PERMISSION_MANAGE'

export type PermissionResponse = {
  name: PermissionName
  description: string
}

export type RolePermissionResponse = {
  name: RoleName
  description: string
  editable: boolean
  permissions: PermissionName[]
}

export type UpdateRolePermissionsRequest = {
  permissionNames: PermissionName[]
}

export async function getPermissions() {
  const response = await api.get<PermissionResponse[]>('/permissions')
  return response.data
}

export async function getRolePermissions() {
  const response = await api.get<RolePermissionResponse[]>('/roles')
  return response.data
}

export async function updateRolePermissions(
  roleName: RoleName,
  permissionNames: PermissionName[],
) {
  const request: UpdateRolePermissionsRequest = {
    permissionNames,
  }

  const response = await api.put<RolePermissionResponse>(
    `/roles/${roleName}/permissions`,
    request,
  )

  return response.data
}