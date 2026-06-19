import { useEffect, useState } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Bell, CheckCircle2, Inbox, ShieldCheck } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, GlassCard, PageShell, StatTile } from '../components/ui'
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  type NotificationResponse,
} from '../services/notificationService'

const containerAnimation: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
    },
  },
}

const cardAnimation: Variants = {
  hidden: { opacity: 0, y: 16 },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.3,
      ease: 'easeOut',
    },
  },
}

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
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  async function refreshNotifications() {
    const data = await fetchNotificationData()
    setNotifications(data.notifications)
    setUnreadCount(data.unreadCount)
  }

  async function handleMarkAsRead(id: number) {
    setActionLoadingId(id)

    try {
      await markNotificationAsRead(id)
      await refreshNotifications()
    } finally {
      setActionLoadingId(null)
    }
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

  const readCount = notifications.filter((notification) => notification.readStatus).length

  return (
    <AppLayout>
      <PageShell
        title="Notifications"
        description="Review account and system updates."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Unread" value={unreadCount} icon={Bell} tone="amber" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Read" value={readCount} icon={CheckCircle2} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Total" value={notifications.length} icon={Inbox} tone="blue" />
          </motion.div>
        </motion.section>

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Notification Inbox</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                System messages and account events
              </p>
            </div>

            <div className="rounded-xl bg-[var(--crm-soft-gradient)] p-3 text-[var(--crm-primary)] ring-1 ring-violet-300/25">
              <ShieldCheck size={22} />
            </div>
          </div>

          {loading ? (
            <div className="p-6 text-sm text-[var(--crm-text-muted)]">Loading notifications...</div>
          ) : notifications.length === 0 ? (
            <EmptyState
              icon={Bell}
              title="No notifications yet"
              message="Account and system updates will appear here."
            />
          ) : (
            <motion.div
              className="divide-y divide-[var(--crm-border)]"
              variants={containerAnimation}
              initial="hidden"
              animate="show"
            >
              {notifications.map((notification) => (
                <motion.div
                  key={notification.id}
                  className={`flex flex-col gap-4 p-5 sm:flex-row sm:items-start sm:justify-between ${
                    notification.readStatus ? 'bg-transparent' : 'bg-violet-500/5'
                  }`}
                  variants={cardAnimation}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <Bell
                        size={18}
                        className={
                          notification.readStatus
                            ? 'text-[var(--crm-text-muted)]'
                            : 'text-[var(--crm-primary)]'
                        }
                      />
                      <h3 className="font-semibold text-[var(--crm-text)]">{notification.title}</h3>
                    </div>

                    <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
                      {notification.message}
                    </p>

                    <p className="mt-3 text-xs font-medium uppercase text-[var(--crm-text-muted)]">
                      {notification.type} - {new Date(notification.createdAt).toLocaleString()}
                    </p>
                  </div>

                  {!notification.readStatus && (
                    <button
                      onClick={() => handleMarkAsRead(notification.id)}
                      disabled={actionLoadingId === notification.id}
                      className="inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl border border-violet-300/30 bg-violet-500/10 px-3 text-sm font-semibold text-[var(--crm-primary)] transition hover:bg-violet-500/15 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <CheckCircle2 size={16} />
                      {actionLoadingId === notification.id ? 'Saving...' : 'Mark read'}
                    </button>
                  )}
                </motion.div>
              ))}
            </motion.div>
          )}
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
