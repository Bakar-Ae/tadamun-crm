import { useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Activity, Building2, Mail, Phone, Search, UserRound, UsersRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { GlassCard, PageShell, StatTile } from '../components/ui'
import { getCustomers, type CustomerResponse } from '../services/customerService'
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
  if (status === 'ACTIVE') {
    return 'border-emerald-400/30 bg-emerald-400/10 text-[var(--crm-success-text)]'
  }

  return 'border-amber-400/30 bg-amber-400/10 text-[var(--crm-warning-text)]'
}

function typeBadgeClass(type: string) {
  if (type === 'COMPANY') {
    return 'border-cyan-400/30 bg-cyan-400/10 text-[var(--crm-accent-text)]'
  }

  return 'border-violet-400/30 bg-violet-400/10 text-violet-400'
}

export function CustomersPage() {
  const [customers, setCustomers] = useState<PageResponse<CustomerResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function loadCustomers(search: string) {
    setLoading(true)
    setError('')

    getCustomers(0, 10, search)
      .then(setCustomers)
      .catch(() => setError('Could not load customers. Please try again.'))
      .finally(() => setLoading(false))
  }

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
          setError('Could not load customers. Please try again.')
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
    loadCustomers(keyword)
  }

  const visibleCustomers = customers?.content ?? []
  const activeCustomers = visibleCustomers.filter((customer) => customer.status === 'ACTIVE').length
  const companyCustomers = visibleCustomers.filter(
    (customer) => customer.customerType === 'COMPANY',
  ).length

  return (
    <AppLayout>
      <PageShell
        title="Customers"
        description="View customer records, company accounts, contact details, and customer status."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Visible" value={visibleCustomers.length} icon={UsersRound} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Active" value={activeCustomers} icon={Activity} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Companies" value={companyCustomers} icon={Building2} tone="amber" />
          </motion.div>
        </motion.section>

        <GlassCard>
          <form onSubmit={handleSearch}>
            <div className="flex flex-col gap-3 md:flex-row">
              <div className="relative flex-1">
                <Search
                  size={18}
                  className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--crm-text-muted)]"
                />
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Search by name, email, phone, or company"
                  className="crm-focus h-11 w-full rounded-xl border border-[var(--crm-border)] bg-[var(--crm-surface)] pl-10 pr-3 text-sm text-[var(--crm-text)] transition placeholder:text-[var(--crm-text-muted)] focus:border-cyan-400"
                />
              </div>

              <button className="h-11 rounded-xl bg-cyan-600 px-5 text-sm font-semibold text-white shadow-sm shadow-cyan-900/20 transition hover:-translate-y-0.5 hover:bg-cyan-700">
                Search
              </button>
            </div>
          </form>
        </GlassCard>

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Customer Directory</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleCustomers.length} of {customers?.totalElements ?? 0} customers
              </p>
            </div>

            <div className="rounded-xl bg-cyan-400/10 p-3 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
              <UsersRound size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Customer</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Phone</th>
                  <th className="px-5 py-3 font-semibold">Company</th>
                  <th className="px-5 py-3 font-semibold">Type</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-[var(--crm-text-muted)]" colSpan={6}>
                      Loading customers...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleCustomers.map((customer) => (
                    <tr key={customer.id} className="transition hover:bg-cyan-400/5">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-400/10 text-[var(--crm-accent-text)] ring-1 ring-cyan-300/20">
                            {customer.customerType === 'COMPANY' ? (
                              <Building2 size={18} />
                            ) : (
                              <UserRound size={18} />
                            )}
                          </div>
                          <div>
                            <p className="font-semibold text-[var(--crm-text)]">{customer.name}</p>
                            <p className="text-xs text-[var(--crm-text-muted)]">ID #{customer.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Mail size={16} />
                          {customer.email ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Phone size={16} />
                          {customer.phone ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        {customer.companyName ?? '-'}
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ring-1 ${typeBadgeClass(
                            customer.customerType,
                          )}`}
                        >
                          {customer.customerType}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
                            customer.status,
                          )}`}
                        >
                          {customer.status}
                        </span>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleCustomers.length === 0 && (
                  <tr>
                    <td className="px-5 py-10 text-center text-[var(--crm-text-muted)]" colSpan={6}>
                      No customers found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </GlassCard>
      </PageShell>
    </AppLayout>
  )
}
