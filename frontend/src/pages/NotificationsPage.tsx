import { useEffect, useState } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Bell, CheckCircle2, Inbox, ShieldCheck } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, GlassCard, PageShell, PaginationBar, StatTile, StatusBadge } from '../components/ui'
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  type PageResponse,
  type NotificationResponse,
} from '../services/notificationService'
import { formatDateTime, humanizeEnum } from '../lib/formatters'


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

async function fetchNotificationData(pageNumber = 0, pageSize = 10) {
  const [notificationPage, countResponse] = await Promise.all([
    getNotifications(pageNumber, pageSize),
    getUnreadNotificationCount(),
  ])

  return {
    notificationPage,
    notifications: notificationPage.content,
    unreadCount: countResponse.unreadCount,
  }
}

export function NotificationsPage() {
  const [notificationPage, setNotificationPage] = useState<PageResponse<NotificationResponse> | null>(null)
  const [notifications, setNotifications] = useState<NotificationResponse[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [loading, setLoading] = useState(true)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  async function refreshNotifications(pageNumber = page, size = pageSize) {
    const data = await fetchNotificationData(pageNumber, size)
    setNotificationPage(data.notificationPage)
    setNotifications(data.notifications)
    setUnreadCount(data.unreadCount)
  }

  async function handleMarkAsRead(id: number) {
    setActionLoadingId(id)

    try {
      await markNotificationAsRead(id)
      await refreshNotifications(page, pageSize)
    } finally {
      setActionLoadingId(null)
    }
  }

  function loadNotifications(pageNumber = page, size = pageSize) {
    setLoading(true)

    refreshNotifications(pageNumber, size)
      .catch(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.location.assign('/')
      })
      .finally(() => setLoading(false))
  }

  function goToPreviousPage() {
    const previousPage = Math.max(page - 1, 0)
    setPage(previousPage)
    loadNotifications(previousPage)
  }

  function goToNextPage() {
    const nextPage = page + 1
    setPage(nextPage)
    loadNotifications(nextPage)
  }

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize)
    setPage(0)
    loadNotifications(0, nextPageSize)
  }

  useEffect(() => {
    let active = true

    fetchNotificationData(0, 10)
      .then((data) => {
        if (!active) {
          return
        }

        setNotificationPage(data.notificationPage)
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
        description="Review messages about account security and CRM activity."
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
            <StatTile label="Messages" value={notifications.length} icon={Inbox} tone="blue" />
          </motion.div>
        </motion.section>

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Inbox</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Security alerts and record updates for this workspace
              </p>
            </div>
          </div>

          {loading ? (
            <div className="p-6 text-sm text-[var(--crm-text-muted)]">Loading notifications...</div>
          ) : notifications.length === 0 ? (
            <EmptyState
              icon={Bell}
              title="No notifications yet"
              message="Important account and workspace messages will appear here."
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
                      {!notification.readStatus && <StatusBadge variant="info">Unread</StatusBadge>}
                    </div>

                    <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
                      {notification.message}
                    </p>

                    <p className="mt-3 text-xs font-medium text-[var(--crm-text-muted)]">
                      {humanizeEnum(notification.type)} · {formatDateTime(notification.createdAt)}
                    </p>
                  </div>

                  {!notification.readStatus && (
                    <button
                      type="button"
                      onClick={() => handleMarkAsRead(notification.id)}
                      disabled={actionLoadingId === notification.id}
                      className="inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl border border-violet-300/30 bg-violet-500/10 px-3 text-sm font-semibold text-[var(--crm-primary)] transition hover:bg-violet-500/15 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <ShieldCheck size={16} />
                      {actionLoadingId === notification.id ? 'Saving...' : 'Mark read'}
                    </button>
                  )}
                </motion.div>
              ))}
            </motion.div>
          )}
          {notificationPage && (
            <PaginationBar
              page={page}
              totalPages={notificationPage.totalPages}
              totalElements={notificationPage.totalElements}
              pageSize={pageSize}
              onPrevious={goToPreviousPage}
              onNext={goToNextPage}
              onPageSizeChange={handlePageSizeChange}
              disabled={loading}
            />
          )}
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
