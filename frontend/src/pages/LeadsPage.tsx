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
  ActivityTimeline,
  DetailDrawer,
  EmptyState,
  GlassCard,
  PageActionButton,
  PageShell,
  SearchPanel,
  StatTile,
  StatusBadge,
  ErrorState,
  PaginationBar,
  type ActivityTimelineItem,
} from '../components/ui'
import { archiveLead, getLeads, updateLead, type LeadResponse, type LeadStatus,} from '../services/leadService'
import { getLeadNotes } from '../services/noteService'
import type { PageResponse } from '../services/userService'
import {
  formatDateTime,
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
  const [editingLead, setEditingLead] = useState(false)
  const [editLeadForm, setEditLeadForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    companyName: '',
    source: '',
    status: 'NEW' as LeadStatus,
    estimatedValue: '',
  })
  const [selectedLead, setSelectedLead] = useState<LeadResponse | null>(null)
  const [leadActivity, setLeadActivity] = useState<ActivityTimelineItem[]>([])
  const [activityLoading, setActivityLoading] = useState(false)
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
  function startEditingLead(lead: LeadResponse) {
  setSelectedLead(lead)
  setEditLeadForm({
    fullName: lead.fullName,
    email: lead.email ?? '',
    phone: lead.phone ?? '',
    companyName: lead.companyName ?? '',
    source: lead.source ?? '',
    status: lead.status,
    estimatedValue: lead.estimatedValue ? String(lead.estimatedValue) : '',
  })
  setEditingLead(true)
}

function cancelEditingLead() {
  setEditingLead(false)

  if (selectedLead) {
    setEditLeadForm({
      fullName: selectedLead.fullName,
      email: selectedLead.email ?? '',
      phone: selectedLead.phone ?? '',
      companyName: selectedLead.companyName ?? '',
      source: selectedLead.source ?? '',
      status: selectedLead.status,
      estimatedValue: selectedLead.estimatedValue ? String(selectedLead.estimatedValue) : '',
    })
  }
}

async function saveLeadEdit() {
  if (!selectedLead) {
    return
  }

  setActionLoadingId(selectedLead.id)

  try {
    const updatedLead = await updateLead(selectedLead.id, {
      fullName: editLeadForm.fullName.trim(),
      email: editLeadForm.email.trim() || null,
      phone: editLeadForm.phone.trim() || null,
      companyName: editLeadForm.companyName.trim() || null,
      source: editLeadForm.source.trim() || null,
      status: editLeadForm.status,
      estimatedValue: editLeadForm.estimatedValue
        ? Number(editLeadForm.estimatedValue)
        : null,
      assignedToUserId: selectedLead.assignedToUserId,
    })

    setSelectedLead(updatedLead)
    setEditingLead(false)
    toast.success(`${updatedLead.fullName} updated`)
    loadLeads(keyword)
  } catch {
    toast.error(getSaveErrorMessage('lead'))
  } finally {
    setActionLoadingId(null)
  }
}

function loadLeadActivity(leadId: number) {
  setActivityLoading(true)

  getLeadNotes(leadId, 0, 5)
    .then((notes) => {
      setLeadActivity(
        notes.content.map((note) => ({
          id: note.id,
          type: 'note',
          title: 'Note added',
          description: note.content,
          actor: note.createdByUserName,
          createdAt: note.createdAt,
        })),
      )
    })
    .catch(() => {
      setLeadActivity([])
    })
    .finally(() => setActivityLoading(false))
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
                  <th className="sticky right-0 bg-[var(--crm-card-subtle)] px-5 py-3 text-right font-semibold">Action</th>
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
                     <td className="sticky right-0 bg-[var(--crm-card)] px-5 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => {
                           setSelectedLead(lead)
                           setEditingLead(false)
                           loadLeadActivity(lead.id)
                         }}
                          className="inline-flex h-9 items-center justify-center rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-violet-300 hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                        >
                          View
                        </button>
                    
                        <button
                          type="button"
                          onClick={() => handleArchive(lead)}
                          disabled={lead.status === 'ARCHIVED' || actionLoadingId === lead.id}
                          className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-amber-300 hover:bg-amber-400/10 hover:text-[var(--crm-warning-text)] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          <Archive size={14} />
                          {actionLoadingId === lead.id ? 'Saving...' : 'Archive'}
                        </button>
                      </div>
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
        <DetailDrawer
          open={selectedLead !== null}
          title={selectedLead?.fullName ?? 'Lead details'}
          description={selectedLead?.companyName ?? 'Pipeline record'}
          onClose={() => {
            setSelectedLead(null)
            setEditingLead(false)
            setLeadActivity([])
          }}
          footer={
            selectedLead && (
              <div className="flex justify-end gap-2">
                {editingLead ? (
                  <>
                    <button
                      type="button"
                      onClick={cancelEditingLead}
                      className="inline-flex h-10 items-center justify-center rounded-xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
                    >
                      Cancel
                    </button>

                    <button
                      type="button"
                      onClick={saveLeadEdit}
                      disabled={actionLoadingId === selectedLead.id}
                      className="inline-flex h-10 items-center justify-center rounded-xl bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {actionLoadingId === selectedLead.id ? 'Saving...' : 'Save changes'}
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    onClick={() => startEditingLead(selectedLead)}
                    className="inline-flex h-10 items-center justify-center rounded-xl bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-110"
                  >
                    Edit lead
                  </button>
                )}
              </div>
            )
          }
        >
          {selectedLead && (
            <div className="space-y-5">
              <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                <h3 className="font-semibold text-[var(--crm-text)]">Lead information</h3>
        
                {editingLead ? (
                  <div className="mt-4 grid gap-4 sm:grid-cols-2">
                    <label className="block">
                      <span className="text-xs uppercase text-[var(--crm-text-muted)]">Name</span>
                      <input
                        value={editLeadForm.fullName}
                        onChange={(event) =>
                          setEditLeadForm((current) => ({ ...current, fullName: event.target.value }))
                        }
                        className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                      />
                    </label>

                    <label className="block">
                      <span className="text-xs uppercase text-[var(--crm-text-muted)]">Company</span>
                      <input
                        value={editLeadForm.companyName}
                        onChange={(event) =>
                          setEditLeadForm((current) => ({ ...current, companyName: event.target.value }))
                        }
                        className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                      />
                    </label>

                    <label className="block">
                      <span className="text-xs uppercase text-[var(--crm-text-muted)]">Email</span>
                      <input
                        value={editLeadForm.email}
                        onChange={(event) =>
                          setEditLeadForm((current) => ({ ...current, email: event.target.value }))
                        }
                        className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                      />
                    </label>

                    <label className="block">
                      <span className="text-xs uppercase text-[var(--crm-text-muted)]">Phone</span>
                      <input
                        value={editLeadForm.phone}
                        onChange={(event) =>
                          setEditLeadForm((current) => ({ ...current, phone: event.target.value }))
                        }
                        className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                      />
                    </label>

                    <label className="block">
                      <span className="text-xs uppercase text-[var(--crm-text-muted)]">Estimated value</span>
                      <input
                        type="number"
                        min="0"
                        value={editLeadForm.estimatedValue}
                        onChange={(event) =>
                          setEditLeadForm((current) => ({ ...current, estimatedValue: event.target.value }))
                        }
                        className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                      />
                    </label>

                    <label className="block">
                      <span className="text-xs uppercase text-[var(--crm-text-muted)]">Source</span>
                      <input
                        value={editLeadForm.source}
                        onChange={(event) =>
                          setEditLeadForm((current) => ({ ...current, source: event.target.value }))
                        }
                        className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                      />
                    </label>

                    <label className="block">
                      <span className="text-xs uppercase text-[var(--crm-text-muted)]">Stage</span>
                      <select
                        value={editLeadForm.status}
                        onChange={(event) =>
                          setEditLeadForm((current) => ({ ...current, status: event.target.value as LeadStatus }))
                        }
                        className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                      >
                        <option value="NEW">New</option>
                        <option value="CONTACTED">Contacted</option>
                        <option value="QUALIFIED">Qualified</option>
                        <option value="LOST">Lost</option>
                        <option value="CONVERTED">Converted</option>
                        <option value="ARCHIVED">Archived</option>
                      </select>
                    </label>

                    <div>
                      <p className="text-xs uppercase text-[var(--crm-text-muted)]">Owner</p>
                      <p className="mt-2 font-medium text-[var(--crm-text)]">
                        {selectedLead.assignedToUserName ?? 'Unassigned'}
                      </p>
                    </div>
                  </div>
                ) : (
                  <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Name</dt>
                      <dd className="mt-1 font-medium text-[var(--crm-text)]">{selectedLead.fullName}</dd>
                    </div>
          
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Company</dt>
                      <dd className="mt-1 font-medium text-[var(--crm-text)]">
                        {selectedLead.companyName ?? 'No company'}
                      </dd>
                    </div>
          
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Email</dt>
                      <dd className="mt-1 font-medium text-[var(--crm-text)]">
                        {selectedLead.email ?? 'No email'}
                      </dd>
                    </div>
          
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Phone</dt>
                      <dd className="mt-1 font-medium text-[var(--crm-text)]">
                        {selectedLead.phone ?? 'No phone'}
                      </dd>
                    </div>
          
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Estimated value</dt>
                      <dd className="mt-1 font-medium text-[var(--crm-text)]">
                        {formatMoney(selectedLead.estimatedValue)}
                      </dd>
                    </div>
          
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Source</dt>
                      <dd className="mt-1 font-medium text-[var(--crm-text)]">
                        {selectedLead.source ?? 'Not set'}
                      </dd>
                    </div>
          
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Stage</dt>
                      <dd className="mt-1">
                        <StatusBadge variant={statusVariant(selectedLead.status)}>
                          {formatStatus(selectedLead.status)}
                        </StatusBadge>
                      </dd>
                    </div>
          
                    <div>
                      <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Owner</dt>
                      <dd className="mt-1 font-medium text-[var(--crm-text)]">
                        {selectedLead.assignedToUserName ?? 'Unassigned'}
                      </dd>
                    </div>
                  </dl>
                )}
              </section>
        
              <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                <h3 className="font-semibold text-[var(--crm-text)]">Record activity</h3>
                <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
                  Created {formatDateTime(selectedLead.createdAt)}. Last updated{' '}
                  {formatDateTime(selectedLead.updatedAt)}.
                </p>
              </section>

              <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                <h3 className="font-semibold text-[var(--crm-text)]">Recent activity</h3>
                <div className="mt-4">
                  <ActivityTimeline
                    items={leadActivity}
                    loading={activityLoading}
                    emptyTitle="No lead activity yet"
                    emptyMessage="Notes for this lead will appear here."
                  />
                </div>
              </section>
            </div>
          )}
        </DetailDrawer>
      </PageShell>
    </AppLayout>
  )
}
