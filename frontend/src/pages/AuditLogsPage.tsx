import { useEffect, useState } from 'react'
import { Clock, Database, ShieldCheck, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getAuditLogs, type AuditLogResponse } from '../services/auditLogService'
import type { PageResponse } from '../services/userService'

export function AuditLogsPage() {
  const [logs, setLogs] = useState<PageResponse<AuditLogResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    getAuditLogs()
      .then((data) => {
        if (!ignore) setLogs(data)
      })
      .catch(() => {
        if (!ignore) setError('Could not load audit logs. Please try again.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [])

  const visibleLogs = logs?.content ?? []

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-rose-300">Security & Compliance</p>
                <h2 className="mt-2 text-3xl font-semibold">Audit Logs</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  Review important actions performed inside the CRM.
                </p>
              </div>

              <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                <p className="text-xs text-slate-400">Visible Events</p>
                <p className="mt-1 text-xl font-semibold">{visibleLogs.length}</p>
              </div>
            </div>
          </div>
        </section>

        {error && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {error}
          </div>
        )}

        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
            <div>
              <h3 className="font-semibold text-slate-950">System Activity</h3>
              <p className="text-sm text-slate-500">
                Showing {visibleLogs.length} of {logs?.totalElements ?? 0} events
              </p>
            </div>

            <div className="rounded-lg bg-rose-50 p-3 text-rose-700">
              <ShieldCheck size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-semibold">Action</th>
                  <th className="px-5 py-3 font-semibold">Entity</th>
                  <th className="px-5 py-3 font-semibold">Actor</th>
                  <th className="px-5 py-3 font-semibold">Details</th>
                  <th className="px-5 py-3 font-semibold">Time</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-slate-500" colSpan={5}>
                      Loading audit logs...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleLogs.map((log) => (
                    <tr key={log.id} className="transition hover:bg-slate-50">
                      <td className="px-5 py-4">
                        <span className="inline-flex rounded-full bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-700 ring-1 ring-rose-200">
                          {log.action}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Database size={16} className="text-slate-400" />
                          {log.entityType} #{log.entityId ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <UserRound size={16} className="text-slate-400" />
                          {log.actorUserName ?? '-'}
                        </div>
                      </td>

                      <td className="max-w-md px-5 py-4 text-slate-600">
                        <span className="line-clamp-2">{log.details ?? '-'}</span>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Clock size={16} className="text-slate-400" />
                          {new Date(log.createdAt).toLocaleString()}
                        </div>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleLogs.length === 0 && (
                  <tr>
                    <td className="px-5 py-10 text-center text-slate-500" colSpan={5}>
                      No audit logs found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </AppLayout>
  )
}