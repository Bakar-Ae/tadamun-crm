import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import toast from 'react-hot-toast'
import { Activity, Archive, Building2, Mail, Phone, Plus, UserRound, UsersRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import {
  EmptyState,
  GlassCard,
  PageActionButton,
  PageShell,
  SearchPanel,
  StatTile,
  StatusBadge,
  LoadingState,
  ErrorState,
  PaginationBar,

} from '../components/ui'
import {
  archiveContact,
  getContacts,
  type ContactResponse,
} from '../services/contactService'
import type { PageResponse } from '../services/userService'
import { formatStatus, getEmptyMessage, statusVariant } from '../lib/formatters'
import { getLoadErrorMessage } from '../lib/errors'
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

export function ContactsPage() {
  const [contacts, setContacts] = useState<PageResponse<ContactResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  const loadContacts = useCallback((search: string, pageNumber = page, size = pageSize) => {
    setLoading(true)
    setError('')

    getContacts(pageNumber, size, search)
      .then(setContacts)
      .catch(() => setError(getLoadErrorMessage('contacts')))
      .finally(() => setLoading(false))
  }, [page, pageSize])

  useEffect(() => {
    let ignore = false

    getContacts(0, 10, '')
      .then((data) => {
        if (!ignore) {
          setContacts(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError(getLoadErrorMessage('contacts'))
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
      loadContacts(keyword)
    }

    window.addEventListener('crm-data-changed', refreshAfterCreate)
    return () => window.removeEventListener('crm-data-changed', refreshAfterCreate)
  }, [keyword, loadContacts])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    loadContacts(keyword, 0)
  }

  async function handleArchive(contact: ContactResponse) {
    if (!window.confirm(`Archive ${contact.fullName}? Their history will remain attached to the customer.`)) {
      return
    }

    setActionLoadingId(contact.id)

    try {
      await archiveContact(contact.id)
      toast.success(`${contact.fullName} archived`)
      loadContacts(keyword)
    } catch {
      toast.error(getLoadErrorMessage('contacts'))
    } finally {
      setActionLoadingId(null)
    }
  }

  function goToPreviousPage() {
    const previousPage = Math.max(page - 1, 0)
    setPage(previousPage)
    loadContacts(keyword, previousPage)
  }

  function goToNextPage() {
    const nextPage = page + 1
    setPage(nextPage)
    loadContacts(keyword, nextPage)
  }

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize)
    setPage(0)
    loadContacts(keyword, 0, nextPageSize)
  }

  const visibleContacts = contacts?.content ?? []
  const activeContacts = visibleContacts.filter((contact) => contact.status === 'ACTIVE').length
  const linkedCustomers = new Set(visibleContacts.map((contact) => contact.customerId)).size
  const hasSearch = keyword.trim().length > 0

  return (
    <AppLayout>
      <PageShell
        title="Contacts"
        description="Keep track of the people connected to customer accounts."
        action={
          <PageActionButton icon={Plus} onClick={() => openQuickCreate('contact')}>
            Add contact
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
            <StatTile label="Contacts" value={visibleContacts.length} icon={UsersRound} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Active contacts" value={activeContacts} icon={Activity} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Linked accounts" value={linkedCustomers} icon={Building2} tone="amber" />
          </motion.div>
        </motion.section>

        <SearchPanel
          value={keyword}
          onChange={setKeyword}
          onSubmit={handleSearch}
          placeholder="Search contacts by name, email, phone, role, or customer"
        />

        {error && <ErrorState message={error} onRetry={() => loadContacts(keyword)} />}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Relationship contacts</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleContacts.length} of {contacts?.totalElements ?? 0} contacts
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[1000px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Contact</th>
                  <th className="px-5 py-3 font-semibold">Customer</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Phone</th>
                  <th className="px-5 py-3 font-semibold">Role</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 text-right font-semibold">Action</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td colSpan={7}>
                      <LoadingState message="Loading contacts..." />

                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleContacts.map((contact) => (
                    <tr key={contact.id} className="transition hover:bg-violet-500/5">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                            <UserRound size={18} />
                          </div>
                          <p className="font-semibold text-[var(--crm-text)]">{contact.fullName}</p>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Building2 size={16} />
                          {contact.customerName}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Mail size={16} />
                          {contact.email ?? 'No email'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Phone size={16} />
                          {contact.phone ?? 'No phone'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        {contact.position ?? 'No role set'}
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={statusVariant(contact.status)}>
                          {formatStatus(contact.status)}
                        </StatusBadge>
                      </td>

                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          onClick={() => handleArchive(contact)}
                          disabled={contact.status !== 'ACTIVE' || actionLoadingId === contact.id}
                          className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-amber-300 hover:bg-amber-400/10 hover:text-[var(--crm-warning-text)] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          <Archive size={14} />
                          {actionLoadingId === contact.id ? 'Saving...' : 'Archive'}
                        </button>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleContacts.length === 0 && (
                  <EmptyState
                    icon={UsersRound}
                    title={hasSearch ? 'No contacts found' : 'No contacts yet'}
                    message={getEmptyMessage(hasSearch, 'contacts', 'Add contact')}
                    colSpan={7}
                    action={
                      !hasSearch && (
                        <PageActionButton icon={Plus} onClick={() => openQuickCreate('contact')}>
                          Add contact
                        </PageActionButton>
                      )
                    }
                  />
                )}
              </tbody>
            </table>
          </div>
          {contacts && (
            <PaginationBar
              page={page}
              totalPages={contacts.totalPages}
              totalElements={contacts.totalElements}
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
