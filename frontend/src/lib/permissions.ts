import type { PermissionName } from '../services/permissionService'

export type PermissionGroup = {
  id: string
  label: string
  description: string
  permissions: PermissionName[]
}

export const permissionGroups: PermissionGroup[] = [
  {
    id: 'users',
    label: 'Users and access',
    description: 'Manage user accounts, status, and assigned roles.',
    permissions: [
      'USER_VIEW',
      'USER_CREATE',
      'USER_UPDATE',
      'USER_DEACTIVATE',
      'USER_ROLE_CHANGE',
    ],
  },
  {
    id: 'customers',
    label: 'Customers',
    description: 'View and manage customer accounts.',
    permissions: [
      'CUSTOMER_VIEW',
      'CUSTOMER_CREATE',
      'CUSTOMER_UPDATE',
      'CUSTOMER_ARCHIVE',
    ],
  },
  {
    id: 'leads',
    label: 'Leads',
    description: 'Manage the sales pipeline and lead conversion.',
    permissions: [
      'LEAD_VIEW',
      'LEAD_CREATE',
      'LEAD_UPDATE',
      'LEAD_CONVERT',
      'LEAD_ARCHIVE',
    ],
  },
  {
    id: 'contacts',
    label: 'Contacts',
    description: 'Manage people connected to customer accounts.',
    permissions: [
      'CONTACT_VIEW',
      'CONTACT_CREATE',
      'CONTACT_UPDATE',
      'CONTACT_ARCHIVE',
    ],
  },
  {
    id: 'tasks',
    label: 'Tasks',
    description: 'Create, assign, update, and complete team tasks.',
    permissions: [
      'TASK_VIEW',
      'TASK_CREATE',
      'TASK_UPDATE',
      'TASK_ASSIGN',
      'TASK_COMPLETE',
    ],
  },
  {
    id: 'notes',
    label: 'Notes',
    description: 'View and maintain customer and lead notes.',
    permissions: ['NOTE_VIEW', 'NOTE_CREATE', 'NOTE_UPDATE'],
  },
  {
    id: 'insights',
    label: 'Dashboard and reports',
    description: 'Access business summaries, reports, and exports.',
    permissions: ['DASHBOARD_VIEW', 'REPORT_VIEW', 'REPORT_EXPORT'],
  },
  {
    id: 'administration',
    label: 'Administration',
    description: 'Access audit history and security configuration.',
    permissions: ['AUDIT_LOG_VIEW', 'PERMISSION_MANAGE'],
  },
]

export function findPermissionGroup(permissionName: PermissionName) {
  return permissionGroups.find((group) =>
    group.permissions.includes(permissionName),
  )
}