import { useEffect, useState } from 'react'
import { BarChart3, CheckCircle2, ClipboardList, Target, UsersRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getReportSummary, type ReportSummary } from '../services/reportService'

export function ReportsPage() {
  const [report, setReport] = useState<ReportSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    getReportSummary()
      .then((data) => {
        if (!ignore) setReport(data)
      })
      .catch(() => {
        if (!ignore) setError('Could not load reports. Please try again.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [])

  const cards = report
    ? [
        { label: 'Total Customers', value: report.totalCustomers, icon: UsersRound, color: 'bg-emerald-50 text-emerald-700' },
        { label: 'Active Customers', value: report.activeCustomers, icon: UsersRound, color: 'bg-blue-50 text-blue-700' },
        { label: 'Total Leads', value: report.totalLeads, icon: Target, color: 'bg-violet-50 text-violet-700' },
        { label: 'Qualified Leads', value: report.qualifiedLeads, icon: Target, color: 'bg-indigo-50 text-indigo-700' },
        { label: 'Total Tasks', value: report.totalTasks, icon: ClipboardList, color: 'bg-amber-50 text-amber-700' },
        { label: 'Completed Tasks', value: report.completedTasks, icon: CheckCircle2, color: 'bg-teal-50 text-teal-700' },
      ]
    : []

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <p className="text-sm font-medium text-emerald-300">Business Intelligence</p>
            <h2 className="mt-2 text-3xl font-semibold">Reports</h2>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              View high-level CRM totals for customers, leads, and tasks.
            </p>
          </div>
        </section>

        {error && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {error}
          </div>
        )}

        {loading ? (
          <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500 shadow-sm">
            Loading reports...
          </div>
        ) : (
          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {cards.map((item) => {
              const Icon = item.icon

              return (
                <div key={item.label} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-slate-500">{item.label}</p>
                      <p className="mt-3 text-3xl font-semibold text-slate-950">{item.value}</p>
                    </div>

                    <div className={`rounded-xl p-3 ${item.color}`}>
                      <Icon size={23} />
                    </div>
                  </div>
                </div>
              )
            })}
          </section>
        )}

        {report && (
          <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5 flex items-center gap-3">
              <div className="rounded-lg bg-slate-950 p-3 text-white">
                <BarChart3 size={22} />
              </div>
              <div>
                <h3 className="font-semibold text-slate-950">Detailed Breakdown</h3>
                <p className="text-sm text-slate-500">Additional report counters</p>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-lg bg-slate-50 p-4">
                <p className="text-xs font-semibold uppercase text-slate-500">Archived Customers</p>
                <p className="mt-2 text-xl font-semibold">{report.archivedCustomers}</p>
              </div>
              <div className="rounded-lg bg-slate-50 p-4">
                <p className="text-xs font-semibold uppercase text-slate-500">New Leads</p>
                <p className="mt-2 text-xl font-semibold">{report.newLeads}</p>
              </div>
              <div className="rounded-lg bg-slate-50 p-4">
                <p className="text-xs font-semibold uppercase text-slate-500">Lost Leads</p>
                <p className="mt-2 text-xl font-semibold">{report.lostLeads}</p>
              </div>
              <div className="rounded-lg bg-slate-50 p-4">
                <p className="text-xs font-semibold uppercase text-slate-500">Cancelled Tasks</p>
                <p className="mt-2 text-xl font-semibold">{report.cancelledTasks}</p>
              </div>
            </div>
          </section>
        )}
      </div>
    </AppLayout>
  )
}