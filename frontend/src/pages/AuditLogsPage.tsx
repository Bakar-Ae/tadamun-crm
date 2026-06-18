import { useEffect, useState } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Clock, Database, ShieldCheck, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { GlassCard, PageShell, StatTile } from '../components/ui'
import { getAuditLogs, type AuditLogResponse } from '../services/auditLogService'
import type { PageResponse } from '../services/userService'

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

export function AuditLogsPage() {
  const [logs, setLogs] = useState<PageResponse<AuditLogResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    getAuditLogs()
      .then((data) => {
        if (!ignore) {
          setLogs(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError('Could not load audit logs. Please try again.')
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

  const visibleLogs = logs?.content ?? []
  const actorCount = new Set(visibleLogs.map((log) => log.actorUserId).filter(Boolean)).size

  return (
    <AppLayout>
      <PageShell
        title="Audit Logs"
        description="Review important actions performed inside Tadamun for security and compliance."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Visible Events" value={visibleLogs.length} icon={ShieldCheck} tone="red" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Actors" value={actorCount} icon={UserRound} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Total Events" value={logs?.totalElements ?? 0} icon={Database} tone="slate" />
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
              <h3 className="font-semibold text-[var(--crm-text)]">System Activity</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleLogs.length} of {logs?.totalElements ?? 0} events
              </p>
            </div>

            <div className="rounded-xl bg-red-400/10 p-3 text-[var(--crm-danger-text)] ring-1 ring-red-300/20">
              <ShieldCheck size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Action</th>
                  <th className="px-5 py-3 font-semibold">Entity</th>
                  <th className="px-5 py-3 font-semibold">Actor</th>
                  <th className="px-5 py-3 font-semibold">Details</th>
                  <th className="px-5 py-3 font-semibold">Time</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-[var(--crm-text-muted)]" colSpan={5}>
                      Loading audit logs...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleLogs.map((log) => (
                    <tr key={log.id} className="transition hover:bg-cyan-400/5">
                      <td className="px-5 py-4">
                        <span className="inline-flex rounded-full border border-red-400/30 bg-red-400/10 px-2.5 py-1 text-xs font-semibold text-[var(--crm-danger-text)] ring-1 ring-red-300/20">
                          {log.action}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Database size={16} />
                          {log.entityType} #{log.entityId ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <UserRound size={16} />
                          {log.actorUserName ?? '-'}
                        </div>
                      </td>

                      <td className="max-w-md px-5 py-4 text-[var(--crm-text-muted)]">
                        <span className="line-clamp-2">{log.details ?? '-'}</span>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Clock size={16} />
                          {new Date(log.createdAt).toLocaleString()}
                        </div>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleLogs.length === 0 && (
                  <tr>
                    <td className="px-5 py-10 text-center text-[var(--crm-text-muted)]" colSpan={5}>
                      No audit logs found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
