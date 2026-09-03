import type { AuditLogResponse } from '../services/auditLogService'

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral'

const roleLabels: Record<string, string> = {
  OWNER: 'Owner',
  ROLE_OWNER: 'Owner',
  ADMIN: 'Administrator',
  ROLE_ADMIN: 'Administrator',
  MANAGER: 'Manager',
  ROLE_MANAGER: 'Manager',
  SALES_REP: 'Sales representative',
  ROLE_SALES_REP: 'Sales representative',
  SUPPORT_STAFF: 'Support staff',
  ROLE_SUPPORT_STAFF: 'Support staff',
}

const statusLabels: Record<string, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  ARCHIVED: 'Archived',
  NEW: 'New',
  CONTACTED: 'Contacted',
  QUALIFIED: 'Qualified',
  LOST: 'Lost',
  CONVERTED: 'Converted',
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent',
  COMPANY: 'Company',
  INDIVIDUAL: 'Individual',
  SUSPENDED: 'Suspended',
  PENDING: 'Pending',
  ACCEPTED: 'Accepted',
  REVOKED: 'Revoked',
  EXPIRED: 'Expired',
}

const auditActionLabels: Record<string, string> = {
  CUSTOMER_CREATED: 'Customer created',
  CUSTOMER_UPDATED: 'Customer updated',
  CUSTOMER_ARCHIVED: 'Customer archived',
  CUSTOMER_RESTORED: 'Customer restored',
  USER_CREATED: 'User invited',
  USER_UPDATED: 'User updated',
  USER_DEACTIVATED: 'User deactivated',
  LEAD_CREATED: 'Lead created',
  LEAD_UPDATED: 'Lead updated',
  LEAD_ARCHIVED: 'Lead archived',
  LEAD_CONVERTED: 'Lead converted',
  CONTACT_CREATED: 'Contact added',
  CONTACT_UPDATED: 'Contact updated',
  CONTACT_ARCHIVED: 'Contact archived',
  CONTACT_RESTORED: 'Contact restored',
  TASK_CREATED: 'Task created',
  TASK_UPDATED: 'Task updated',
  TASK_COMPLETED: 'Task completed',
  NOTE_CREATED: 'Note added',
  NOTE_UPDATED: 'Note updated',
  PASSWORD_CHANGED: 'Password changed',
  PASSWORD_RESET_REQUESTED: 'Password reset requested',
  PASSWORD_RESET_COMPLETED: 'Password reset completed',
  ORGANIZATION_CREATED: 'Organization created',
  ORGANIZATION_UPDATED: 'Organization settings updated',
  ORGANIZATION_MEMBERSHIP_CREATED: 'Member added',
  ORGANIZATION_MEMBERSHIP_ROLE_UPDATED: 'Member role changed',
  ORGANIZATION_MEMBERSHIP_DEACTIVATED: 'Member access removed',
  ORGANIZATION_INVITATION_CREATED: 'Invitation sent',
  ORGANIZATION_INVITATION_REVOKED: 'Invitation revoked',
  ORGANIZATION_INVITATION_ACCEPTED: 'Invitation accepted',
}

export function humanizeEnum(value: string | null | undefined) {
  if (!value) {
    return 'Not set'
  }

  const known = statusLabels[value] ?? roleLabels[value] ?? auditActionLabels[value]

  if (known) {
    return known
  }

  return value
    .replace(/^ROLE_/, '')
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ')
}

export function formatRole(value: string | null | undefined) {
  return value ? roleLabels[value] ?? humanizeEnum(value) : 'No role'
}

export function formatStatus(value: string | null | undefined) {
  return humanizeEnum(value)
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'Not scheduled'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Invalid date'
  }

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date)
}

export function formatMoney(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return 'Not estimated'
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value)
}

export function statusVariant(value: string | null | undefined): BadgeVariant {
  if (!value) {
    return 'neutral'
  }

  if (['ACTIVE', 'ACCEPTED', 'QUALIFIED', 'CONVERTED', 'COMPLETED'].includes(value)) {
    return 'success'
  }

  if (['NEW', 'PENDING', 'CONTACTED', 'OPEN', 'IN_PROGRESS', 'COMPANY', 'MEDIUM'].includes(value)) {
    return 'info'
  }

  if (['URGENT', 'LOST', 'CANCELLED', 'SUSPENDED'].includes(value)) {
    return 'danger'
  }

  if (['ARCHIVED', 'INACTIVE', 'REVOKED', 'EXPIRED', 'LOW', 'INDIVIDUAL'].includes(value)) {
    return 'neutral'
  }

  return 'warning'
}

export function priorityVariant(value: string | null | undefined): BadgeVariant {
  if (value === 'URGENT' || value === 'HIGH') {
    return 'danger'
  }

  if (value === 'MEDIUM') {
    return 'info'
  }

  return 'neutral'
}

export function getEmptyMessage(hasSearch: boolean, itemName: string, actionLabel: string) {
  return hasSearch
    ? `No ${itemName} match this search. Clear the search or try a different keyword.`
    : `No ${itemName} yet. Use ${actionLabel} to create the first one.`
}

function parseAuditDetails(details: string | null) {
  if (!details) {
    return null
  }

  try {
    const parsed = JSON.parse(details) as Record<string, unknown>
    return parsed
  } catch {
    return null
  }
}



export function formatAuditDetails(log: AuditLogResponse) {
  const parsed = parseAuditDetails(log.details)

  if (!parsed) {
    return log.details ?? 'No additional details'
  }

  const usefulValue =
    parsed.name ??
    parsed.fullName ??
    parsed.title ??
    parsed.email ??
    parsed.customerName ??
    parsed.leadName

  if (typeof usefulValue === 'string' && usefulValue.trim()) {
    return usefulValue
  }

  return Object.entries(parsed)
    .filter(([, value]) => value !== null && value !== undefined && value !== '')
    .slice(0, 2)
    .map(([key, value]) => `${humanizeEnum(key)}: ${String(value)}`)
    .join(', ') || 'No additional details'
}

export function formatEntityName(entityType: string | null | undefined, entityId?: number | null) {
  const label = humanizeEnum(entityType)
  return entityId ? `${label} ${entityId}` : label
}

export function formatAuditAction(action: string) {
  return auditActionLabels[action] ?? humanizeEnum(action)
}
