import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import toast from 'react-hot-toast'
import {
  Activity,
  Archive,
  Building2,
  Mail,
  Phone,
  Plus,
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
  LoadingState,
  ErrorState,
  DetailDrawer,
  PaginationBar,
  
} from '../components/ui'
import {
  archiveCustomer,
  getCustomers,
  type CustomerResponse,
} from '../services/customerService'
import type { PageResponse } from '../services/userService'
import {formatDateTime,formatStatus, getEmptyMessage, statusVariant } from '../lib/formatters'
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

export function CustomersPage() {
  const [customers, setCustomers] = useState<PageResponse<CustomerResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerResponse | null>(null)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  const loadCustomers = useCallback((search: string, pageNumber = page, size = pageSize) => {
    setLoading(true)
    setError('')

    getCustomers(pageNumber, size, search)
    .then(setCustomers)
    .catch(() => setError(getLoadErrorMessage('customers')))
    .finally(() => setLoading(false))
  }, [page,pageSize])

  useEffect(() => {
    let ignore = false

    getCustomers(0, 10, '')
      .then((data) => {
        if (!ignore) {
          setCustomers(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError(getLoadErrorMessage('customers'))
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
      loadCustomers(keyword)
    }

    window.addEventListener('crm-data-changed', refreshAfterCreate)
    return () => window.removeEventListener('crm-data-changed', refreshAfterCreate)
  }, [keyword, loadCustomers])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    loadCustomers(keyword, 0)
  }

  async function handleArchive(customer: CustomerResponse) {
    if (!window.confirm(`Archive ${customer.name}? You can keep the history, but it will leave active work lists.`)) {
      return
    }
    setActionLoadingId(customer.id)

    try {
      await archiveCustomer(customer.id)
      toast.success(`${customer.name} archived`)
      loadCustomers(keyword)
    } catch {
      toast.error(getSaveErrorMessage('customer'))
    } finally {
      setActionLoadingId(null)
    }
  }
  function goToPreviousPage() {
    const previousPage = Math.max(page - 1, 0)
    setPage(previousPage)
    loadCustomers(keyword, previousPage)
  }

function goToNextPage() {
  const nextPage = page + 1
  setPage(nextPage)
  loadCustomers(keyword, nextPage)
}

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize)
    setPage(0)
    loadCustomers(keyword, 0, nextPageSize)
  }

  const visibleCustomers = customers?.content ?? []
  const activeCustomers = visibleCustomers.filter((customer) => customer.status === 'ACTIVE').length
  const companyCustomers = visibleCustomers.filter(
    (customer) => customer.customerType === 'COMPANY',
  ).length
  const hasSearch = keyword.trim().length > 0

  return (
    <AppLayout>
      <PageShell
        title="Customers"
        description="Manage accounts, contact details, and the companies your team is working with."
        action={
          <PageActionButton icon={Plus} onClick={() => openQuickCreate('customer')}>
            Add customer
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
            <StatTile label="Customers" value={visibleCustomers.length} icon={UsersRound} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Active accounts" value={activeCustomers} icon={Activity} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Companies" value={companyCustomers} icon={Building2} tone="amber" />
          </motion.div>
        </motion.section>

        <SearchPanel
          value={keyword}
          onChange={setKeyword}
          onSubmit={handleSearch}
          placeholder="Search customers by name, email, phone, or company"
        />

        {error && <ErrorState message={error} onRetry={() => loadCustomers(keyword)} />}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Account list</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleCustomers.length} of {customers?.totalElements ?? 0} customers
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Customer</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Phone</th>
                  <th className="px-5 py-3 font-semibold">Company</th>
                  <th className="px-5 py-3 font-semibold">Type</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="sticky right-0 bg-[var(--crm-card-subtle)] px-5 py-3 text-right font-semibold">Action</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td colSpan={7}>
                      <LoadingState message="Loading customers..." />
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleCustomers.map((customer) => (
                    <tr key={customer.id} className="transition hover:bg-violet-500/5">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                            {customer.customerType === 'COMPANY' ? (
                              <Building2 size={18} />
                            ) : (
                              <UserRound size={18} />
                            )}
                          </div>
                          <p className="font-semibold text-[var(--crm-text)]">{customer.name}</p>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Mail size={16} />
                          {customer.email ?? 'No email'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Phone size={16} />
                          {customer.phone ?? 'No phone'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        {customer.companyName ?? 'Independent'}
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={statusVariant(customer.customerType)}>
                          {formatStatus(customer.customerType)}
                        </StatusBadge>
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={statusVariant(customer.status)}>
                          {formatStatus(customer.status)}
                        </StatusBadge>
                      </td>

                  <td className="sticky right-0 bg-[var(--crm-card)] px-5 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => setSelectedCustomer(customer)}
                        className="inline-flex h-9 items-center justify-center rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-violet-300 hover:bg-violet-500/10 hover:text-[var(--crm-primary)]"
                      >
                        View
                      </button>
                  
                      <button
                        type="button"
                        onClick={() => handleArchive(customer)}
                        disabled={customer.status !== 'ACTIVE' || actionLoadingId === customer.id}
                        className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-amber-300 hover:bg-amber-400/10 hover:text-[var(--crm-warning-text)] disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <Archive size={14} />
                        {actionLoadingId === customer.id ? 'Saving...' : 'Archive'}
                      </button>
                    </div>
                     </td>
                    </tr>
                  ))}

                {!loading && visibleCustomers.length === 0 && (
                  <EmptyState
                    icon={UsersRound}
                    title={hasSearch ? 'No customers found' : 'No customers yet'}
                    message={getEmptyMessage(hasSearch, 'customers', 'Add customer')}
                    colSpan={7}
                    action={
                      !hasSearch && (
                        <PageActionButton icon={Plus} onClick={() => openQuickCreate('customer')}>
                          Add customer
                        </PageActionButton>
                      )
                    }
                  />
                )}
              </tbody>
            </table>
          </div>
        {customers && (
          <PaginationBar
            page={page}
            totalPages={customers.totalPages}
            totalElements={customers.totalElements}
            pageSize={pageSize}
            onPrevious={goToPreviousPage}
            onNext={goToNextPage}
            onPageSizeChange={handlePageSizeChange}
            disabled={loading}
          />
       )}
        </GlassCard>
        <DetailDrawer
           open={selectedCustomer !== null}
           title={selectedCustomer?.name ?? 'Customer details'}
           description={selectedCustomer?.companyName ?? 'Customer account'}
           onClose={() => setSelectedCustomer(null)}
          >
           {selectedCustomer && (
             <div className="space-y-5">
               <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                 <h3 className="font-semibold text-[var(--crm-text)]">Account information</h3>
          
                 <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                   <div>
                     <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Name</dt>
                     <dd className="mt-1 font-medium text-[var(--crm-text)]">{selectedCustomer.name}</dd>
                   </div>
          
                   <div>
                     <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Company</dt>
                     <dd className="mt-1 font-medium text-[var(--crm-text)]">
                       {selectedCustomer.companyName ?? 'Independent'}
                     </dd>
                   </div>
          
                   <div>
                     <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Email</dt>
                     <dd className="mt-1 font-medium text-[var(--crm-text)]">
                       {selectedCustomer.email ?? 'No email'}
                     </dd>
                   </div>
          
                   <div>
                     <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Phone</dt>
                     <dd className="mt-1 font-medium text-[var(--crm-text)]">
                       {selectedCustomer.phone ?? 'No phone'}
                     </dd>
                   </div>
          
                   <div>
                     <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Type</dt>
                     <dd className="mt-1">
                       <StatusBadge variant={statusVariant(selectedCustomer.customerType)}>
                         {formatStatus(selectedCustomer.customerType)}
                       </StatusBadge>
                     </dd>
                   </div>
          
                   <div>
                     <dt className="text-xs uppercase text-[var(--crm-text-muted)]">Status</dt>
                     <dd className="mt-1">
                       <StatusBadge variant={statusVariant(selectedCustomer.status)}>
                         {formatStatus(selectedCustomer.status)}
                       </StatusBadge>
                     </dd>
                   </div>
                 </dl>
               </section>
          
               <section className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] p-4">
                 <h3 className="font-semibold text-[var(--crm-text)]">Record activity</h3>
                 <p className="mt-2 text-sm text-[var(--crm-text-muted)]">
                   Created {formatDateTime(selectedCustomer.createdAt)}. Last updated{' '}
                   {formatDateTime(selectedCustomer.updatedAt)}.
                 </p>
               </section>
             </div>
           )}
         </DetailDrawer>
      </PageShell>
    </AppLayout>
  )
}
