import { api } from './api'

export type NotificationType =
  | 'SYSTEM'
  | 'TASK_ASSIGNED'
  | 'CUSTOMER_UPDATED'
  | 'LEAD_UPDATED'
  | 'PASSWORD_CHANGED'
  | 'REPORT_READY'

export type NotificationResponse = {
  id: number
  title: string
  message: string
  type: NotificationType
  readStatus: boolean
  readAt: string | null
  createdAt: string
}

export type PageResponse<T> = {
  content: T[]
  empty: boolean
  first: boolean
  last: boolean
  number: number
  numberOfElements: number
  size: number
  totalElements: number
  totalPages: number
}

export type UnreadNotificationCountResponse = {
  unreadCount: number
}
export type NotificationPreferences = {
  emailEnabled: boolean
  inAppEnabled: boolean
  taskNotificationsEnabled: boolean
  customerNotificationsEnabled: boolean
  leadNotificationsEnabled: boolean
  reportNotificationsEnabled: boolean
}

export async function getNotifications(page = 0, size = 10) {
  const response = await api.get<PageResponse<NotificationResponse>>('/notifications', {
    params: {
      page,
      size,
      sort: 'id,desc',
    },
  })

  return response.data
}

export async function getUnreadNotificationCount() {
  const response = await api.get<UnreadNotificationCountResponse>('/notifications/unread-count')
  return response.data
}

export async function markNotificationAsRead(id: number) {
  await api.patch(`/notifications/${id}/read`)
}
export async function getNotificationPreferences() {
  const response = await api.get<NotificationPreferences>(
    '/notifications/preferences',
  )

  return response.data
}

export async function updateNotificationPreferences(
  preferences: NotificationPreferences,
) {
  const response = await api.put<NotificationPreferences>(
    '/notifications/preferences',
    preferences,
  )

  return response.data
}