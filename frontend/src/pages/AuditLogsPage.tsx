import { useCallback, useEffect, useState } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Clock, Database, ShieldCheck, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, GlassCard, LoadingState, PageShell, PaginationBar, StatTile, StatusBadge } from '../components/ui'
import { getAuditLogs, type AuditLogResponse } from '../services/auditLogService'
import type { PageResponse } from '../services/userService'
import {
  formatAuditAction,
  formatAuditDetails,
  formatDateTime,
  formatEntityName,
} from '../lib/formatters'
import { getLoadErrorMessage } from '../lib/errors'

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

function auditVariant(action: string) {
  if (action.includes('ARCHIVED') || action.includes('DEACTIVATED')) {
    return 'warning' as const
  }

  if (action.includes('PASSWORD') || action.includes('LOGIN')) {
    return 'danger' as const
  }

  if (action.includes('CREATED')) {
    return 'success' as const
  }

  return 'info' as const
}

export function AuditLogsPage() {
  const [logs, setLogs] = useState<PageResponse<AuditLogResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [error, setError] = useState('')

  const loadAuditLogs = useCallback((pageNumber = page, size = pageSize) => {
    setLoading(true)
    setError('')

    getAuditLogs(pageNumber, size)
      .then(setLogs)
      .catch(() => setError(getLoadErrorMessage('audit history')))
      .finally(() => setLoading(false))
  }, [page, pageSize])

  useEffect(() => {
    let ignore = false

    getAuditLogs(0, 10)
      .then((data) => {
        if (!ignore) {
          setLogs(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError(getLoadErrorMessage('audit history'))
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false)
        }
      })

    return () => {
      ignore = true
    }
  }, [])

  function goToPreviousPage() {
   const previousPage = Math.max(page - 1, 0)
   setPage(previousPage)
   loadAuditLogs(previousPage)
}

  function goToNextPage() {
   const nextPage = page + 1
   setPage(nextPage)
   loadAuditLogs(nextPage)
}

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize)
    setPage(0)
    loadAuditLogs(0, nextPageSize)
  }

  const visibleLogs = logs?.content ?? []
  const actorCount = new Set(visibleLogs.map((log) => log.actorUserId).filter(Boolean)).size

  return (
    <AppLayout>
      <PageShell
        title="Audit history"
        description="Review important account, customer, and security events."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Audit Events" value={visibleLogs.length} icon={ShieldCheck} tone="red" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="People involved" value={actorCount} icon={UserRound} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Total events" value={logs?.totalElements ?? 0} icon={Database} tone="slate" />
          </motion.div>
        </motion.section>

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Security timeline</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleLogs.length} of {logs?.totalElements ?? 0} events
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Event</th>
                  <th className="px-5 py-3 font-semibold">Record</th>
                  <th className="px-5 py-3 font-semibold">Person</th>
                  <th className="px-5 py-3 font-semibold">Details</th>
                  <th className="px-5 py-3 font-semibold">Time</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td colSpan={5}>
                      <LoadingState message="Loading audit history..." />
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleLogs.map((log) => (
                    <tr key={log.id} className="transition hover:bg-violet-500/5">
                      <td className="px-5 py-4">
                        <StatusBadge variant={auditVariant(log.action)}>
                          {formatAuditAction(log.action)}
                        </StatusBadge>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Database size={16} />
                          {formatEntityName(log.entityType, log.entityId)}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <UserRound size={16} />
                          {log.actorUserName ?? 'System'}
                        </div>
                      </td>

                      <td className="max-w-md px-5 py-4 text-[var(--crm-text-muted)]">
                        <span className="line-clamp-2">{formatAuditDetails(log)}</span>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Clock size={16} />
                          {formatDateTime(log.createdAt)}
                        </div>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleLogs.length === 0 && (
                  <EmptyState
                    icon={ShieldCheck}
                    title="No audit events yet"
                    message="Important account, security, and record changes will appear here."
                    colSpan={5}
                  />
                )}
              </tbody>
            </table>
          </div>
          {logs && (
        <PaginationBar
          page={page}
          totalPages={logs.totalPages}
          totalElements={logs.totalElements}
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
