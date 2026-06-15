import { useEffect, useState } from 'react'
import { Building2, Mail, Phone, Search, UserRound, UsersRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getCustomers, type CustomerResponse } from '../services/customerService'
import type { PageResponse } from '../services/userService'

function statusBadgeClass(status: string) {
  if (status === 'ACTIVE') {
    return 'bg-emerald-50 text-emerald-700 ring-emerald-200'
  }

  return 'bg-slate-100 text-slate-600 ring-slate-200'
}

function typeBadgeClass(type: string) {
  if (type === 'COMPANY') {
    return 'bg-blue-50 text-blue-700 ring-blue-200'
  }

  return 'bg-violet-50 text-violet-700 ring-violet-200'
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

  function handleSearch(event: React.FormEvent<HTMLFormElement>) {
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
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-emerald-300">Customer Management</p>
                <h2 className="mt-2 text-3xl font-semibold">Customers</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  View customer records, company accounts, contact details, and customer status.
                </p>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Visible</p>
                  <p className="mt-1 text-xl font-semibold">{visibleCustomers.length}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Active</p>
                  <p className="mt-1 text-xl font-semibold">{activeCustomers}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Companies</p>
                  <p className="mt-1 text-xl font-semibold">{companyCustomers}</p>
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
                  placeholder="Search by name, email, phone, or company"
                  className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-emerald-600 focus:ring-4 focus:ring-emerald-100"
                />
              </div>

              <button className="h-11 rounded-lg bg-emerald-600 px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700">
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
              <h3 className="font-semibold text-slate-950">Customer Directory</h3>
              <p className="text-sm text-slate-500">
                Showing {visibleCustomers.length} of {customers?.totalElements ?? 0} customers
              </p>
            </div>

            <div className="rounded-lg bg-emerald-50 p-3 text-emerald-700">
              <UsersRound size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] border-collapse text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-semibold">Customer</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Phone</th>
                  <th className="px-5 py-3 font-semibold">Company</th>
                  <th className="px-5 py-3 font-semibold">Type</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-slate-500" colSpan={6}>
                      Loading customers...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleCustomers.map((customer) => (
                    <tr key={customer.id} className="transition hover:bg-slate-50">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-600 text-white">
                            {customer.customerType === 'COMPANY' ? (
                              <Building2 size={18} />
                            ) : (
                              <UserRound size={18} />
                            )}
                          </div>
                          <div>
                            <p className="font-semibold text-slate-950">{customer.name}</p>
                            <p className="text-xs text-slate-500">ID #{customer.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Mail size={16} className="text-slate-400" />
                          {customer.email ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Phone size={16} className="text-slate-400" />
                          {customer.phone ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">{customer.companyName ?? '-'}</td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${typeBadgeClass(
                            customer.customerType,
                          )}`}
                        >
                          {customer.customerType}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
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
                    <td className="px-5 py-10 text-center text-slate-500" colSpan={6}>
                      No customers found
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
