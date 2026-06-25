import { useCallback, useEffect, useState } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Clock, Database, Eye, Filter, RotateCcw, Search, ShieldCheck, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import {
  DetailDrawer,
  EmptyState,
  GlassCard,
  LoadingState,
  PageShell,
  PaginationBar,
  StatTile,
  StatusBadge,
} from '../components/ui'
import { getAuditLogs, type AuditLogFilters, type AuditLogResponse } from '../services/auditLogService'
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

const actionOptions = [
  { label: 'All actions', value: '' },
  { label: 'Customer created', value: 'CUSTOMER_CREATED' },
  { label: 'Customer archived', value: 'CUSTOMER_ARCHIVED' },
  { label: 'Customer restored', value: 'CUSTOMER_RESTORED' },
  { label: 'Lead created', value: 'LEAD_CREATED' },
  { label: 'Lead converted', value: 'LEAD_CONVERTED' },
  { label: 'Contact added', value: 'CONTACT_CREATED' },
  { label: 'Task created', value: 'TASK_CREATED' },
  { label: 'Task completed', value: 'TASK_COMPLETED' },
  { label: 'Password changed', value: 'PASSWORD_CHANGED' },
]

const entityOptions = [
  { label: 'All records', value: '' },
  { label: 'Customers', value: 'CUSTOMER' },
  { label: 'Leads', value: 'LEAD' },
  { label: 'Contacts', value: 'CONTACT' },
  { label: 'Tasks', value: 'TASK' },
  { label: 'Notes', value: 'NOTE' },
  { label: 'Users', value: 'USER' },
]

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
  const [filters, setFilters] = useState<AuditLogFilters>({})
  const [draftFilters, setDraftFilters] = useState<AuditLogFilters>({})
  const [selectedLog, setSelectedLog] = useState<AuditLogResponse | null>(null)

  const loadAuditLogs = useCallback((
    pageNumber = page,
    size = pageSize,
    nextFilters = filters,
  ) => {
    setLoading(true)
    setError('')

    getAuditLogs(pageNumber, size, nextFilters)
      .then(setLogs)
      .catch(() => setError(getLoadErrorMessage('audit history')))
      .finally(() => setLoading(false))
  }, [filters, page, pageSize])

  useEffect(() => {
    let ignore = false

    getAuditLogs(0, 10, {})
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

  function applyFilters() {
    setPage(0)
    setFilters(draftFilters)
    loadAuditLogs(0, pageSize, draftFilters)
  }

  function clearFilters() {
    const emptyFilters: AuditLogFilters = {}
    setDraftFilters(emptyFilters)
    setFilters(emptyFilters)
    setPage(0)
    loadAuditLogs(0, pageSize, emptyFilters)
  }

  function handleActorFilter(value: string) {
    setDraftFilters((current) => ({
      ...current,
      actorUserId: value ? Number(value) : null,
    }))
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

        <GlassCard>
          <div className="mb-4 flex items-center gap-2">
            <Filter size={18} className="text-violet-500" />
            <h3 className="font-semibold text-[var(--crm-text)]">Filter audit history</h3>
          </div>

          <div className="grid gap-3 lg:grid-cols-[1.4fr_1fr_1fr_0.8fr_auto_auto]">
            <label className="relative">
              <Search
                size={17}
                className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
              />
              <input
                value={draftFilters.keyword ?? ''}
                onChange={(event) =>
                  setDraftFilters((current) => ({ ...current, keyword: event.target.value }))
                }
                placeholder="Search event, person, or details"
                className="crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-3 text-sm text-[var(--crm-text)] outline-none"
              />
            </label>

            <label htmlFor="audit-action-filter">
              <span className="sr-only">Filter audit logs by action</span>
              <select
                id="audit-action-filter"
                name="auditAction"
                value={draftFilters.action ?? ''}
                onChange={(event) =>
                  setDraftFilters((current) => ({ ...current, action: event.target.value }))
                }
                className="crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] outline-none"
              >
                {actionOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label htmlFor="audit-entity-filter">
              <span className="sr-only">Filter audit logs by record type</span>
              <select
                id="audit-entity-filter"
                name="auditEntityType"
                value={draftFilters.entityType ?? ''}
                onChange={(event) =>
                  setDraftFilters((current) => ({ ...current, entityType: event.target.value }))
                }
                className="crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] outline-none"
              >
                {entityOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <input
              type="number"
              min="1"
              value={draftFilters.actorUserId ?? ''}
              onChange={(event) => handleActorFilter(event.target.value)}
              placeholder="User ID"
              className="crm-focus h-11 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] outline-none"
            />

            <button
              type="button"
              onClick={applyFilters}
              className="crm-focus inline-flex h-11 items-center justify-center rounded-2xl bg-gradient-to-r from-violet-600 to-blue-500 px-5 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:-translate-y-0.5"
            >
              Apply
            </button>

            <button
              type="button"
              onClick={clearFilters}
              className="crm-focus inline-flex h-11 items-center justify-center gap-2 rounded-2xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
            >
              <RotateCcw size={16} />
              Reset
            </button>
          </div>
        </GlassCard>

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
                  <th className="px-5 py-3 font-semibold">Action</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td colSpan={6}>
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

                      <td className="px-5 py-4">
                        <button
                          type="button"
                          onClick={() => setSelectedLog(log)}
                          className="crm-focus inline-flex items-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 py-2 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
                        >
                          <Eye size={15} />
                          View
                        </button>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleLogs.length === 0 && (
                  <EmptyState
                    icon={ShieldCheck}
                    title="No audit events yet"
                    message="Important account, security, and record changes will appear here."
                    colSpan={6}
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

        <DetailDrawer
          open={Boolean(selectedLog)}
          title={selectedLog ? formatAuditAction(selectedLog.action) : 'Audit event'}
          description={selectedLog ? formatDateTime(selectedLog.createdAt) : undefined}
          onClose={() => setSelectedLog(null)}
        >
          {selectedLog ? (
            <div className="space-y-5">
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
                    Person
                  </p>
                  <p className="mt-2 font-semibold text-[var(--crm-text)]">
                    {selectedLog.actorUserName ?? 'System'}
                  </p>
                </div>

                <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
                    Record
                  </p>
                  <p className="mt-2 font-semibold text-[var(--crm-text)]">
                    {formatEntityName(selectedLog.entityType, selectedLog.entityId)}
                  </p>
                </div>
              </div>

              <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
                  Summary
                </p>
                <p className="mt-2 text-sm leading-6 text-[var(--crm-text)]">
                  {formatAuditDetails(selectedLog)}
                </p>
              </div>

              <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
                  Raw details
                </p>
                <pre className="mt-3 max-h-64 overflow-auto whitespace-pre-wrap rounded-xl bg-black/5 p-3 text-xs text-[var(--crm-text-muted)]">
                  {selectedLog.details ?? 'No stored details'}
                </pre>
              </div>
            </div>
          ) : null}
        </DetailDrawer>
      </PageShell>
    </AppLayout>
  )
}
