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
} from '../components/ui'
import {
  archiveCustomer,
  getCustomers,
  type CustomerResponse,
} from '../services/customerService'
import type { PageResponse } from '../services/userService'
import { formatStatus, getEmptyMessage, statusVariant } from '../lib/formatters'
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
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  const loadCustomers = useCallback((search: string) => {
    setLoading(true)
    setError('')

     getCustomers(0, 10, search)
    .then(setCustomers)
    .catch(() => setError(getLoadErrorMessage('customers')))
    .finally(() => setLoading(false))
  }, [])

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
    loadCustomers(keyword)
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

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

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
                  <th className="px-5 py-3 text-right font-semibold">Action</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-[var(--crm-text-muted)]" colSpan={7}>
                      Loading customers...
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

                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          onClick={() => handleArchive(customer)}
                          disabled={customer.status !== 'ACTIVE' || actionLoadingId === customer.id}
                          className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-amber-300 hover:bg-amber-400/10 hover:text-[var(--crm-warning-text)] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          <Archive size={14} />
                          {actionLoadingId === customer.id ? 'Saving...' : 'Archive'}
                        </button>
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
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
