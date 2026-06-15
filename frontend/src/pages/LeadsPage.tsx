import { useEffect, useState } from 'react'
import { BriefcaseBusiness, DollarSign, Mail, Search, Target, UserRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getLeads, type LeadResponse } from '../services/leadService'
import type { PageResponse } from '../services/userService'

function statusBadgeClass(status: string) {
  if (status === 'NEW') {
    return 'bg-blue-50 text-blue-700 ring-blue-200'
  }

  if (status === 'QUALIFIED' || status === 'CONVERTED') {
    return 'bg-emerald-50 text-emerald-700 ring-emerald-200'
  }

  if (status === 'LOST' || status === 'ARCHIVED') {
    return 'bg-slate-100 text-slate-600 ring-slate-200'
  }

  return 'bg-amber-50 text-amber-700 ring-amber-200'
}

function formatMoney(value: number | null) {
  if (value === null) {
    return '-'
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value)
}

export function LeadsPage() {
  const [leads, setLeads] = useState<PageResponse<LeadResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function loadLeads(search: string) {
    setLoading(true)
    setError('')

    getLeads(0, 10, search)
      .then(setLeads)
      .catch(() => setError('Could not load leads. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let ignore = false

    getLeads(0, 10, '')
      .then((data) => {
        if (!ignore) {
          setLeads(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError('Could not load leads. Please try again.')
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

  function handleSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadLeads(keyword)
  }

  const visibleLeads = leads?.content ?? []
  const qualifiedLeads = visibleLeads.filter((lead) => lead.status === 'QUALIFIED').length
  const assignedLeads = visibleLeads.filter((lead) => lead.assignedToUserName).length

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-violet-300">Lead Pipeline</p>
                <h2 className="mt-2 text-3xl font-semibold">Leads</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  Track potential customers, lead status, estimated value, and ownership.
                </p>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Visible</p>
                  <p className="mt-1 text-xl font-semibold">{visibleLeads.length}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Qualified</p>
                  <p className="mt-1 text-xl font-semibold">{qualifiedLeads}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Assigned</p>
                  <p className="mt-1 text-xl font-semibold">{assignedLeads}</p>
                </div>
              </div>
            </div>
          </div>

          <form onSubmit={handleSearch} className="border-t border-slate-200 p-5">
            <div className="flex flex-col gap-3 md:flex-row">
              <div className="relative flex-1">
                <Search
                  size={18}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                />
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Search by name, email, company, or source"
                  className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-violet-600 focus:ring-4 focus:ring-violet-100"
                />
              </div>

              <button className="h-11 rounded-lg bg-violet-600 px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-violet-700">
                Search
              </button>
            </div>
          </form>
        </section>

        {error && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {error}
          </div>
        )}

        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
            <div>
              <h3 className="font-semibold text-slate-950">Lead Directory</h3>
              <p className="text-sm text-slate-500">
                Showing {visibleLeads.length} of {leads?.totalElements ?? 0} leads
              </p>
            </div>

            <div className="rounded-lg bg-violet-50 p-3 text-violet-700">
              <Target size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-semibold">Lead</th>
                  <th className="px-5 py-3 font-semibold">Company</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Value</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Assigned To</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-slate-500" colSpan={6}>
                      Loading leads...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleLeads.map((lead) => (
                    <tr key={lead.id} className="transition hover:bg-slate-50">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-violet-600 text-white">
                            <UserRound size={18} />
                          </div>
                          <div>
                            <p className="font-semibold text-slate-950">{lead.fullName}</p>
                            <p className="text-xs text-slate-500">ID #{lead.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <BriefcaseBusiness size={16} className="text-slate-400" />
                          {lead.companyName ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Mail size={16} className="text-slate-400" />
                          {lead.email ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <DollarSign size={16} className="text-slate-400" />
                          {formatMoney(lead.estimatedValue)}
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
                            lead.status,
                          )}`}
                        >
                          {lead.status}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-slate-600">{lead.assignedToUserName ?? '-'}</td>
                    </tr>
                  ))}

                {!loading && visibleLeads.length === 0 && (
                  <tr>
                    <td className="px-5 py-10 text-center text-slate-500" colSpan={6}>
                      No leads found
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
