import { useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import {
  BriefcaseBusiness,
  DollarSign,
  Mail,
  Target,
  UserRound,
  UsersRound,
} from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, GlassCard, PageShell, SearchPanel, StatTile } from '../components/ui'
import { getLeads, type LeadResponse } from '../services/leadService'
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

function statusBadgeClass(status: string) {
  if (status === 'NEW') {
    return 'border-blue-400/30 bg-blue-500/10 text-[var(--crm-primary)]'
  }

  if (status === 'QUALIFIED' || status === 'CONVERTED') {
    return 'border-emerald-400/30 bg-emerald-400/10 text-[var(--crm-success-text)]'
  }

  if (status === 'LOST' || status === 'ARCHIVED') {
    return 'border-slate-400/20 bg-slate-400/10 text-[var(--crm-text-muted)]'
  }

  return 'border-amber-400/30 bg-amber-400/10 text-[var(--crm-warning-text)]'
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

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadLeads(keyword)
  }

  const visibleLeads = leads?.content ?? []
  const qualifiedLeads = visibleLeads.filter((lead) => lead.status === 'QUALIFIED').length
  const assignedLeads = visibleLeads.filter((lead) => lead.assignedToUserName).length

  return (
    <AppLayout>
      <PageShell
        title="Leads"
        description="Track leads, owners, status, and estimated value."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Visible" value={visibleLeads.length} icon={Target} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Qualified" value={qualifiedLeads} icon={DollarSign} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Assigned" value={assignedLeads} icon={UsersRound} tone="amber" />
          </motion.div>
        </motion.section>

        <SearchPanel
          value={keyword}
          onChange={setKeyword}
          onSubmit={handleSearch}
          placeholder="Search leads by name, email, company, or source"
        />

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Lead Directory</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleLeads.length} of {leads?.totalElements ?? 0} leads
              </p>
            </div>

            <div className="rounded-xl bg-[var(--crm-soft-gradient)] p-3 text-[var(--crm-primary)] ring-1 ring-violet-300/25">
              <Target size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Lead</th>
                  <th className="px-5 py-3 font-semibold">Company</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Value</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Assigned To</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-[var(--crm-text-muted)]" colSpan={6}>
                      Loading leads...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleLeads.map((lead) => (
                    <tr key={lead.id} className="transition hover:bg-violet-500/5">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                            <UserRound size={18} />
                          </div>
                          <div>
                            <p className="font-semibold text-[var(--crm-text)]">{lead.fullName}</p>
                            <p className="text-xs text-[var(--crm-text-muted)]">ID #{lead.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <BriefcaseBusiness size={16} />
                          {lead.companyName ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Mail size={16} />
                          {lead.email ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <DollarSign size={16} />
                          {formatMoney(lead.estimatedValue)}
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
                            lead.status,
                          )}`}
                        >
                          {lead.status}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        {lead.assignedToUserName ?? '-'}
                      </td>
                    </tr>
                  ))}

                {!loading && visibleLeads.length === 0 && (
                  <EmptyState
                    icon={Target}
                    title="No leads found"
                    message="Try another name, email, company, or source."
                    colSpan={6}
                  />
                )}
              </tbody>
            </table>
          </div>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
