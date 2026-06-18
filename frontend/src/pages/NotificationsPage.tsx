import { useEffect, useState } from 'react'
import { Bell, CheckCircle2 } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  type NotificationResponse,
} from '../services/notificationService'

async function fetchNotificationData() {
  const [notificationPage, countResponse] = await Promise.all([
    getNotifications(),
    getUnreadNotificationCount(),
  ])

  return {
    notifications: notificationPage.content,
    unreadCount: countResponse.unreadCount,
  }
}

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)

  async function refreshNotifications() {
    const data = await fetchNotificationData()
    setNotifications(data.notifications)
    setUnreadCount(data.unreadCount)
  }

  async function handleMarkAsRead(id: number) {
    await markNotificationAsRead(id)
    await refreshNotifications()
  }

  useEffect(() => {
    let active = true

    fetchNotificationData()
      .then((data) => {
        if (!active) {
          return
        }

        setNotifications(data.notifications)
        setUnreadCount(data.unreadCount)
      })
      .catch(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.location.assign('/')
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <AppLayout>
      <div>
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h2 className="text-2xl font-semibold text-slate-950">Notifications</h2>
            <p className="mt-1 text-sm text-slate-500">
              Security and system updates for your account.
            </p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 shadow-sm">
            Unread: {unreadCount}
          </div>
        </div>

        <section className="mt-6 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          {loading ? (
            <div className="p-6 text-sm text-slate-500">Loading notifications...</div>
          ) : notifications.length === 0 ? (
            <div className="flex flex-col items-center justify-center px-6 py-16 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-blue-700">
                <Bell size={24} />
              </div>
              <h3 className="mt-4 text-lg font-semibold text-slate-950">No notifications yet</h3>
              <p className="mt-2 max-w-md text-sm text-slate-500">
                Important security and system updates will appear here.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`flex flex-col gap-4 p-5 sm:flex-row sm:items-start sm:justify-between ${
                    notification.readStatus ? 'bg-white' : 'bg-blue-50/60'
                  }`}
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <Bell
                        size={18}
                        className={notification.readStatus ? 'text-slate-400' : 'text-blue-700'}
                      />
                      <h3 className="font-semibold text-slate-950">{notification.title}</h3>
                    </div>

                    <p className="mt-2 text-sm text-slate-600">{notification.message}</p>

                    <p className="mt-3 text-xs font-medium uppercase text-slate-400">
                      {notification.type} • {new Date(notification.createdAt).toLocaleString()}
                    </p>
                  </div>

                  {!notification.readStatus && (
                    <button
                      onClick={() => handleMarkAsRead(notification.id)}
                      className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-blue-200 bg-white px-3 text-sm font-semibold text-blue-700 transition hover:bg-blue-50"
                    >
                      <CheckCircle2 size={16} />
                      Mark read
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </AppLayout>
  )
}