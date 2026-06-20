import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import toast from 'react-hot-toast'
import {
  Archive,
  BriefcaseBusiness,
  DollarSign,
  Mail,
  Plus,
  Target,
  UserRound,
  UsersRound,
} from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import {
  EmptyState,
  GlassCard,
  PageActionButton,
  PageShell,
  SearchPanel,
  StatTile,
  StatusBadge,
  ErrorState,
  PaginationBar,
} from '../components/ui'
import { archiveLead, getLeads, type LeadResponse } from '../services/leadService'
import type { PageResponse } from '../services/userService'
import {
  formatMoney,
  formatStatus,
  getEmptyMessage,
  statusVariant,
} from '../lib/formatters'
import { getLoadErrorMessage, getSaveErrorMessage } from '../lib/errors'
import { openQuickCreate } from '../lib/quickCreate'

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

export function LeadsPage() {
  const [leads, setLeads] = useState<PageResponse<LeadResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  const loadLeads = useCallback((search: string, pageNumber = page, size = pageSize) => {
    setLoading(true)
    setError('')

    getLeads(pageNumber, size, search)
      .then(setLeads)
      .catch(() => setError(getLoadErrorMessage('leads')))
      .finally(() => setLoading(false))
  }, [page, pageSize])
  

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
          setError(getLoadErrorMessage('leads'))
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

  useEffect(() => {
    function refreshAfterCreate() {
      loadLeads(keyword)
    }

    window.addEventListener('crm-data-changed', refreshAfterCreate)
    return () => window.removeEventListener('crm-data-changed', refreshAfterCreate)
  }, [keyword, loadLeads])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    loadLeads(keyword, 0)
  }

  async function handleArchive(lead: LeadResponse) {
    if (!window.confirm(`Archive ${lead.fullName}? This keeps the record but removes it from active pipeline work.`)) {
      return
    }

    setActionLoadingId(lead.id)

    try {
      await archiveLead(lead.id)
      toast.success(`${lead.fullName} archived`)
      loadLeads(keyword)
    } catch {
      toast.error(getSaveErrorMessage('lead'))
    } finally {
      setActionLoadingId(null)
    }
  }

  function goToPreviousPage() {
    const previousPage = Math.max(page - 1, 0)
    setPage(previousPage)
    loadLeads(keyword, previousPage)
  }

  function goToNextPage() {
    const nextPage = page + 1
    setPage(nextPage)
    loadLeads(keyword, nextPage)
  }

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize)
    setPage(0)
    loadLeads(keyword, 0, nextPageSize)
  }

  const visibleLeads = leads?.content ?? []
  const qualifiedLeads = visibleLeads.filter((lead) => lead.status === 'QUALIFIED').length
  const assignedLeads = visibleLeads.filter((lead) => lead.assignedToUserName).length
  const hasSearch = keyword.trim().length > 0

  return (
    <AppLayout>
      <PageShell
        title="Pipeline"
        description="Track potential customers from first contact through qualification."
        action={
          <PageActionButton icon={Plus} onClick={() => openQuickCreate('lead')}>
            Add lead
          </PageActionButton>
        }
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Leads" value={visibleLeads.length} icon={Target} tone="blue" />
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

        {error && <ErrorState message={error} onRetry={() => loadLeads(keyword)} />}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Active pipeline</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleLeads.length} of {leads?.totalElements ?? 0} leads
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[1040px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Lead</th>
                  <th className="px-5 py-3 font-semibold">Company</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Value</th>
                  <th className="px-5 py-3 font-semibold">Stage</th>
                  <th className="px-5 py-3 font-semibold">Owner</th>
                  <th className="px-5 py-3 text-right font-semibold">Action</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-[var(--crm-text-muted)]" colSpan={7}>
                      Loading pipeline...
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
                          <p className="font-semibold text-[var(--crm-text)]">{lead.fullName}</p>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <BriefcaseBusiness size={16} />
                          {lead.companyName ?? 'No company'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Mail size={16} />
                          {lead.email ?? 'No email'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <DollarSign size={16} />
                          {formatMoney(lead.estimatedValue)}
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={statusVariant(lead.status)}>
                          {formatStatus(lead.status)}
                        </StatusBadge>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        {lead.assignedToUserName ?? 'Unassigned'}
                      </td>

                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          onClick={() => handleArchive(lead)}
                          disabled={lead.status === 'ARCHIVED' || actionLoadingId === lead.id}
                          className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-amber-300 hover:bg-amber-400/10 hover:text-[var(--crm-warning-text)] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          <Archive size={14} />
                          {actionLoadingId === lead.id ? 'Saving...' : 'Archive'}
                        </button>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleLeads.length === 0 && (
                  <EmptyState
                    icon={Target}
                    title={hasSearch ? 'No leads found' : 'No leads yet'}
                    message={getEmptyMessage(hasSearch, 'leads', 'Add lead')}
                    colSpan={7}
                    action={
                      !hasSearch && (
                        <PageActionButton icon={Plus} onClick={() => openQuickCreate('lead')}>
                          Add lead
                        </PageActionButton>
                      )
                    }
                  />
                )}
              </tbody>
            </table>
          </div>
          {leads && (
            <PaginationBar
              page={page}
              totalPages={leads.totalPages}
              totalElements={leads.totalElements}
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
