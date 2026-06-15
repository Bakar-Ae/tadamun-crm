import { useEffect, useState } from 'react'
import {
  Archive,
  ArrowUpRight,
  CheckCircle2,
  ClipboardList,
  Target,
  Users,
  UserRound,
} from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getMe, type LoginResponse } from '../services/authService'
import { getDashboardSummary, type DashboardSummary } from '../services/dashboardService'

const cards = [
  {
    label: 'Total Users',
    key: 'totalUsers',
    icon: Users,
    color: 'from-blue-600 to-blue-500',
    bg: 'bg-blue-50',
    text: 'text-blue-700',
  },
  {
    label: 'Active Customers',
    key: 'activeCustomers',
    icon: UserRound,
    color: 'from-emerald-600 to-emerald-500',
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
  },
  {
    label: 'Active Leads',
    key: 'activeLeads',
    icon: Target,
    color: 'from-violet-600 to-violet-500',
    bg: 'bg-violet-50',
    text: 'text-violet-700',
  },
  {
    label: 'Open Tasks',
    key: 'openTasks',
    icon: ClipboardList,
    color: 'from-amber-500 to-orange-500',
    bg: 'bg-amber-50',
    text: 'text-amber-700',
  },
  {
    label: 'Completed Tasks',
    key: 'completedTasks',
    icon: CheckCircle2,
    color: 'from-teal-600 to-cyan-500',
    bg: 'bg-teal-50',
    text: 'text-teal-700',
  },
  {
    label: 'Archived Customers',
    key: 'archivedCustomers',
    icon: Archive,
    color: 'from-rose-600 to-pink-500',
    bg: 'bg-rose-50',
    text: 'text-rose-700',
  },
] as const

export function DashboardPage() {
  const [user, setUser] = useState<LoginResponse | null>(null)
  const [summary, setSummary] = useState<DashboardSummary | null>(null)

  useEffect(() => {
    Promise.all([getMe(), getDashboardSummary()])
      .then(([userData, summaryData]) => {
        setUser(userData)
        setSummary(summaryData)
      })
      .catch(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/'
      })
  }, [])

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-7 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-blue-300">Enterprise CRM Dashboard</p>
                <h2 className="mt-2 text-3xl font-semibold">
                  Welcome back, {user?.fullName ?? 'Loading...'}
                </h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  Track customer activity, lead movement, task execution, reports, and operational health from one workspace.
                </p>
              </div>

              <div className="rounded-xl border border-white/10 bg-white/10 px-5 py-4">
                <p className="text-xs font-semibold uppercase text-slate-400">Signed in as</p>
                <p className="mt-1 text-sm font-semibold text-white">{user?.email ?? 'Loading...'}</p>
                <p className="text-xs text-blue-200">{user?.role ?? ''}</p>
              </div>
            </div>
          </div>

          <div className="grid gap-0 border-t border-slate-200 md:grid-cols-3">
            <div className="border-b border-slate-200 p-5 md:border-b-0 md:border-r">
              <p className="text-sm text-slate-500">Customer base</p>
              <p className="mt-2 text-2xl font-semibold text-slate-950">
                {summary?.activeCustomers ?? '-'}
              </p>
            </div>
            <div className="border-b border-slate-200 p-5 md:border-b-0 md:border-r">
              <p className="text-sm text-slate-500">Lead pipeline</p>
              <p className="mt-2 text-2xl font-semibold text-slate-950">
                {summary?.activeLeads ?? '-'}
              </p>
            </div>
            <div className="p-5">
              <p className="text-sm text-slate-500">Open work</p>
              <p className="mt-2 text-2xl font-semibold text-slate-950">
                {summary?.openTasks ?? '-'}
              </p>
            </div>
          </div>
        </section>

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {cards.map((card) => {
            const Icon = card.icon
            const value = summary?.[card.key] ?? '-'

            return (
              <div
                key={card.key}
                className="group overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-lg"
              >
                <div className={`h-1.5 bg-gradient-to-r ${card.color}`} />
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-sm font-medium text-slate-500">{card.label}</p>
                      <p className="mt-3 text-3xl font-semibold text-slate-950">{value}</p>
                    </div>

                    <div className={`rounded-xl ${card.bg} p-3 ${card.text}`}>
                      <Icon size={23} />
                    </div>
                  </div>

                  <div className="mt-5 flex items-center gap-2 text-xs font-medium text-slate-400">
                    <ArrowUpRight size={14} />
                    Live CRM metric
                  </div>
                </div>
              </div>
            )
          })}
        </section>

        <section className="grid gap-4 lg:grid-cols-2">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-base font-semibold text-slate-950">Today&apos;s Focus</h3>
            <p className="mt-1 text-sm text-slate-500">
              Keep work moving by following active leads and open tasks.
            </p>

            <div className="mt-5 space-y-3">
              <div className="flex items-center justify-between rounded-lg border border-amber-100 bg-amber-50 px-4 py-3">
                <div>
                  <p className="text-sm font-semibold text-amber-900">Open tasks</p>
                  <p className="text-xs text-amber-700">Pending team execution</p>
                </div>
                <p className="text-xl font-semibold text-amber-950">{summary?.openTasks ?? '-'}</p>
              </div>

              <div className="flex items-center justify-between rounded-lg border border-violet-100 bg-violet-50 px-4 py-3">
                <div>
                  <p className="text-sm font-semibold text-violet-900">Active leads</p>
                  <p className="text-xs text-violet-700">Pipeline opportunities</p>
                </div>
                <p className="text-xl font-semibold text-violet-950">
                  {summary?.activeLeads ?? '-'}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-base font-semibold text-slate-950">Operational Health</h3>
            <p className="mt-1 text-sm text-slate-500">
              Simple health view for the current CRM workspace.
            </p>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="rounded-lg border border-emerald-100 bg-emerald-50 p-4">
                <p className="text-xs font-semibold uppercase text-emerald-700">Customers</p>
                <p className="mt-2 text-2xl font-semibold text-emerald-950">
                  {summary?.activeCustomers ?? '-'}
                </p>
              </div>

              <div className="rounded-lg border border-blue-100 bg-blue-50 p-4">
                <p className="text-xs font-semibold uppercase text-blue-700">Users</p>
                <p className="mt-2 text-2xl font-semibold text-blue-950">
                  {summary?.totalUsers ?? '-'}
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </AppLayout>
  )
}