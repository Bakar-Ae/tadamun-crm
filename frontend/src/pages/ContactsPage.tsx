import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import toast from 'react-hot-toast'
import { Activity, Archive, Building2, Filter, Mail, Phone, Plus, RotateCcw, UserRound, UsersRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import {
  DetailDrawer,
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
  updateContact,
  type ContactFilters,
  type ContactResponse,
  type ContactStatus,
} from '../services/contactService'
import type { PageResponse } from '../services/userService'
import {formatDateTime ,formatStatus, getEmptyMessage, statusVariant } from '../lib/formatters'
import { getLoadErrorMessage,getSaveErrorMessage} from '../lib/errors'
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
  const [statusFilter, setStatusFilter] = useState<ContactStatus | ''>('')
  const [customerIdFilter, setCustomerIdFilter] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedContact, setSelectedContact] = useState<ContactResponse | null>(null)
  const [editingContact, setEditingContact] = useState(false)
  const [editContactForm, setEditContactForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    position: '',
    status: 'ACTIVE' as ContactStatus,
  })
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  const loadContacts = useCallback((
    search: string,
    pageNumber = page,
    size = pageSize,
    status: ContactStatus | '' = statusFilter,
    customerIdText = customerIdFilter,
  ) => {
    setLoading(true)
    setError('')
  
  const filters: ContactFilters = {
      keyword: search,
      status,
      customerId: customerIdText ? Number(customerIdText) : null,
    }
  
    getContacts(pageNumber, size, filters)
      .then(setContacts)
      .catch(() => setError(getLoadErrorMessage('contacts')))
      .finally(() => setLoading(false))
  }, [customerIdFilter, page, pageSize, statusFilter])

  useEffect(() => {
    let ignore = false

    getContacts(0, 10, {})
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
      loadContacts(keyword, page, pageSize, statusFilter, customerIdFilter)
    }

    window.addEventListener('crm-data-changed', refreshAfterCreate)
    return () => window.removeEventListener('crm-data-changed', refreshAfterCreate)
  },[customerIdFilter, keyword, loadContacts, page, pageSize, statusFilter])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    loadContacts(keyword, 0, pageSize, statusFilter, customerIdFilter)
  }
  function applyContactFilters() {
   setPage(0)
   loadContacts(keyword, 0, pageSize, statusFilter, customerIdFilter)
 }

  function resetContactFilters() {
    setKeyword('')
    setStatusFilter('')
    setCustomerIdFilter('')
    setPage(0)
    loadContacts('', 0, pageSize, '', '')
  }

  async function handleArchive(contact: ContactResponse) {
    if (!window.confirm(`Archive ${contact.fullName}? Their history will remain attached to the customer.`)) {
      return
    }

    setActionLoadingId(contact.id)

    try {
      const archivedContact = await archiveContact(contact.id)
      toast.success(`${contact.fullName} archived`)
      if (selectedContact?.id === archivedContact.id) {
        setSelectedContact(archivedContact)
      }
      loadContacts(keyword, page, pageSize, statusFilter, customerIdFilter)
    } catch {
      toast.error(getSaveErrorMessage('contact'))
    } finally {
      setActionLoadingId(null)
    }
  }

  async function handleRestore(contact: ContactResponse) {
    if (!window.confirm(`Restore ${contact.fullName} to active contacts?`)) {
      return
    }

    setActionLoadingId(contact.id)

    try {
      const restoredContact = await updateContact(contact.id, {
        fullName: contact.fullName,
        email: contact.email,
        phone: contact.phone,
        position: contact.position,
        status: 'ACTIVE',
      })

      toast.success(`${restoredContact.fullName} restored`)
      if (selectedContact?.id === restoredContact.id) {
        setSelectedContact(restoredContact)
      }
      loadContacts(keyword, page, pageSize, statusFilter, customerIdFilter)
    } catch {
      toast.error(getSaveErrorMessage('contact'))
    } finally {
      setActionLoadingId(null)
    }
  }

  function startEditingContact(contact: ContactResponse) {
  setSelectedContact(contact)
  setEditContactForm({
    fullName: contact.fullName,
    email: contact.email ?? '',
    phone: contact.phone ?? '',
    position: contact.position ?? '',
    status: contact.status,
  })
  setEditingContact(true)
}

function cancelEditingContact() {
  setEditingContact(false)

  if (selectedContact) {
    setEditContactForm({
      fullName: selectedContact.fullName,
      email: selectedContact.email ?? '',
      phone: selectedContact.phone ?? '',
      position: selectedContact.position ?? '',
      status: selectedContact.status,
    })
  }
}

async function saveContactEdit() {
  if (!selectedContact) return

  setActionLoadingId(selectedContact.id)

  try {
    const updatedContact = await updateContact(selectedContact.id, {
      fullName: editContactForm.fullName.trim(),
      email: editContactForm.email.trim() || null,
      phone: editContactForm.phone.trim() || null,
      position: editContactForm.position.trim() || null,
      status: editContactForm.status,
    })

    setSelectedContact(updatedContact)
    setEditingContact(false)
    toast.success(`${updatedContact.fullName} updated`)
    loadContacts(keyword, page, pageSize, statusFilter, customerIdFilter)
  } catch {
    toast.error(getSaveErrorMessage('contact'))
  } finally {
    setActionLoadingId(null)
  }
}

  function goToPreviousPage() {
    const previousPage = Math.max(page - 1, 0)
    setPage(previousPage)
    loadContacts(keyword, previousPage, pageSize, statusFilter, customerIdFilter)
  }

  function goToNextPage() {
    const nextPage = page + 1
    setPage(nextPage)
    loadContacts(keyword, nextPage, pageSize, statusFilter, customerIdFilter)
  }

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize)
    setPage(0)
    loadContacts(keyword, 0, nextPageSize, statusFilter, customerIdFilter)
  }

  const visibleContacts = contacts?.content ?? []
  const activeContacts = visibleContacts.filter((contact) => contact.status === 'ACTIVE').length
  const linkedCustomers = new Set(visibleContacts.map((contact) => contact.customerId)).size
  const hasSearch =
  keyword.trim().length > 0 ||
  statusFilter.length > 0 ||
  customerIdFilter.trim().length > 0

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
        <GlassCard>
        <div className="mb-4 flex items-center gap-2">
          <Filter size={18} className="text-violet-500" />
          <h3 className="font-semibold text-[var(--crm-text)]">Contact filters</h3>
        </div>
      
        <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto_auto]">
          <label>
            <span className="sr-only">Filter contacts by status</span>
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as ContactStatus | '')}
              className="crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] outline-none"
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </label>
      
          <label>
            <span className="sr-only">Filter contacts by customer ID</span>
            <input
              type="number"
              min="1"
              value={customerIdFilter}
              onChange={(event) => setCustomerIdFilter(event.target.value)}
              placeholder="Customer ID"
              className="crm-focus h-11 w-full rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)] outline-none"
            />
          </label>
      
          <button
            type="button"
            onClick={applyContactFilters}
            className="crm-focus inline-flex h-11 items-center justify-center rounded-2xl bg-gradient-to-r from-violet-600 to-blue-500 px-5 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:-translate-y-0.5"
          >
            Apply
          </button>
      
          <button
            type="button"
            onClick={resetContactFilters}
            className="crm-focus inline-flex h-11 items-center justify-center gap-2 rounded-2xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
          >
            <RotateCcw size={16} />
            Reset
          </button>
        </div>
      </GlassCard>

        {error && <ErrorState message={error} onRetry={() => loadContacts(keyword, page, pageSize, statusFilter, customerIdFilter)} />}

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
                  <th className="sticky right-0 bg-[var(--crm-card-subtle)] px-5 py-3 text-right font-semibold">Action</th>
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
                     <td className="sticky right-0 bg-[var(--crm-card)] px-5 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedContact(contact)
                            setEditingContact(false)
                          }}
                          className="inline-flex h-9 items-center justify-center rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-violet-300 hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                        >
                          View
                        </button>
                    
                        {contact.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            onClick={() => handleArchive(contact)}
                            disabled={actionLoadingId === contact.id}
                            className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-amber-300 hover:bg-amber-400/10 hover:text-[var(--crm-warning-text)] disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <Archive size={14} />
                            {actionLoadingId === contact.id ? 'Saving...' : 'Archive'}
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleRestore(contact)}
                            disabled={actionLoadingId === contact.id}
                            className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-emerald-300 hover:bg-emerald-400/10 hover:text-[var(--crm-success-text)] disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <RotateCcw size={14} />
                            {actionLoadingId === contact.id ? 'Saving...' : 'Restore'}
                          </button>
                        )}
                      </div>
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
       <DetailDrawer
        open={selectedContact !== null}
        title={selectedContact?.fullName ?? 'Contact details'}
        description={selectedContact?.customerName ?? 'Customer contact'}
        onClose={() => {
          setSelectedContact(null)
          setEditingContact(false)
        }}
        footer={
          selectedContact && (
            <div className="flex justify-end gap-2">
              {editingContact ? (
                <>
                  <button
                    type="button"
                    onClick={cancelEditingContact}
                    className="inline-flex h-10 items-center justify-center rounded-xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)]"
                  >
                    Cancel
                  </button>
      
                  <button
                    type="button"
                    onClick={saveContactEdit}
                    disabled={actionLoadingId === selectedContact.id}
                    className="inline-flex h-10 items-center justify-center rounded-xl bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {actionLoadingId === selectedContact.id ? 'Saving...' : 'Save changes'}
                  </button>
                </>
              ) : (
                <>
                  {selectedContact.status === 'ACTIVE' ? (
                    <button
                      type="button"
                      onClick={() => handleArchive(selectedContact)}
                      disabled={actionLoadingId === selectedContact.id}
                      className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:border-amber-300 hover:bg-amber-400/10 hover:text-[var(--crm-warning-text)] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Archive size={15} />
                      Archive
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleRestore(selectedContact)}
                      disabled={actionLoadingId === selectedContact.id}
                      className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:border-emerald-300 hover:bg-emerald-400/10 hover:text-[var(--crm-success-text)] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <RotateCcw size={15} />
                      Restore
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => startEditingContact(selectedContact)}
                    className="inline-flex h-10 items-center justify-center rounded-xl bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-110"
                  >
                    Edit contact
                  </button>
                </>
              )}
            </div>
          )
        }
      >
        {selectedContact && (
          <div className="space-y-5">
            <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
              <h3 className="font-semibold text-[var(--crm-text)]">Contact information</h3>
      
              {editingContact ? (
                <div className="mt-4 grid gap-4 sm:grid-cols-2">
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Name</span>
                    <input
                      value={editContactForm.fullName}
                      onChange={(event) =>
                        setEditContactForm((current) => ({ ...current, fullName: event.target.value }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    />
                  </label>
      
                  <div>
                    <p className="text-xs uppercase text-[var(--crm-text-muted)]">Customer</p>
                    <p className="mt-2 font-medium text-[var(--crm-text)]">{selectedContact.customerName}</p>
                  </div>
      
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Email</span>
                    <input
                      value={editContactForm.email}
                      onChange={(event) =>
                        setEditContactForm((current) => ({ ...current, email: event.target.value }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    />
                  </label>
      
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Phone</span>
                    <input
                      value={editContactForm.phone}
                      onChange={(event) =>
                        setEditContactForm((current) => ({ ...current, phone: event.target.value }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    />
                  </label>
      
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Role</span>
                    <input
                      value={editContactForm.position}
                      onChange={(event) =>
                        setEditContactForm((current) => ({ ...current, position: event.target.value }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    />
                  </label>
      
                  <label className="block">
                    <span className="text-xs uppercase text-[var(--crm-text-muted)]">Status</span>
                    <select
                      value={editContactForm.status}
                      onChange={(event) =>
                        setEditContactForm((current) => ({
                          ...current,
                          status: event.target.value as ContactStatus,
                        }))
                      }
                      className="crm-focus mt-1 h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-sm text-[var(--crm-text)]"
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="ARCHIVED">Archived</option>
                    </select>
                  </label>
                </div>
              ) : (
                <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Name</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">{selectedContact.fullName}</dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Customer</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">{selectedContact.customerName}</dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Email</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">
                      {selectedContact.email ?? 'No email'}
                    </dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Phone</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">
                      {selectedContact.phone ?? 'No phone'}
                    </dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Role</dt>
                    <dd className="mt-1 font-medium text-[var(--crm-text)]">
                      {selectedContact.position ?? 'No role set'}
                    </dd>
                  </div>
      
                  <div>
                    <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Status</dt>
                    <dd className="mt-1">
                      <StatusBadge variant={statusVariant(selectedContact.status)}>
                        {formatStatus(selectedContact.status)}
                      </StatusBadge>
                    </dd>
                  </div>
                </dl>
              )}
            </section>
      
            <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
              <h3 className="font-semibold text-[var(--crm-text)]">Record activity</h3>
              <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
                Created {formatDateTime(selectedContact.createdAt)}. Last updated{' '}
                {formatDateTime(selectedContact.updatedAt)}.
              </p>
            </section>
          </div>
        )}
      </DetailDrawer>
      </PageShell>
    </AppLayout>
  )
}
