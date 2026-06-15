import { useEffect, useState } from 'react'
import { Building2, Mail, Phone, Search, UserRound, UsersRound } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getContacts, type ContactResponse } from '../services/contactService'
import type { PageResponse } from '../services/userService'

function statusBadgeClass(status: string) {
  if (status === 'ACTIVE') {
    return 'bg-emerald-50 text-emerald-700 ring-emerald-200'
  }

  return 'bg-slate-100 text-slate-600 ring-slate-200'
}

export function ContactsPage() {
  const [contacts, setContacts] = useState<PageResponse<ContactResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function loadContacts(search: string) {
    setLoading(true)
    setError('')

    getContacts(0, 10, search)
      .then(setContacts)
      .catch(() => setError('Could not load contacts. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let ignore = false

    getContacts(0, 10, '')
      .then((data) => {
        if (!ignore) setContacts(data)
      })
      .catch(() => {
        if (!ignore) setError('Could not load contacts. Please try again.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [])

  function handleSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadContacts(keyword)
  }

  const visibleContacts = contacts?.content ?? []
  const activeContacts = visibleContacts.filter((contact) => contact.status === 'ACTIVE').length

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-cyan-300">Contact Management</p>
                <h2 className="mt-2 text-3xl font-semibold">Contacts</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  Manage people connected to customer accounts, roles, emails, and phone numbers.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Visible</p>
                  <p className="mt-1 text-xl font-semibold">{visibleContacts.length}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Active</p>
                  <p className="mt-1 text-xl font-semibold">{activeContacts}</p>
                </div>
              </div>
            </div>
          </div>

          <form onSubmit={handleSearch} className="border-t border-slate-200 p-5">
            <div className="flex flex-col gap-3 md:flex-row">
              <div className="relative flex-1">
                <Search size={18} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Search by name, email, phone, position, or customer"
                  className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-cyan-600 focus:ring-4 focus:ring-cyan-100"
                />
              </div>

              <button className="h-11 rounded-lg bg-cyan-600 px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-cyan-700">
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
              <h3 className="font-semibold text-slate-950">Contact Directory</h3>
              <p className="text-sm text-slate-500">
                Showing {visibleContacts.length} of {contacts?.totalElements ?? 0} contacts
              </p>
            </div>

            <div className="rounded-lg bg-cyan-50 p-3 text-cyan-700">
              <UsersRound size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-semibold">Contact</th>
                  <th className="px-5 py-3 font-semibold">Customer</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Phone</th>
                  <th className="px-5 py-3 font-semibold">Position</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-slate-500" colSpan={6}>
                      Loading contacts...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleContacts.map((contact) => (
                    <tr key={contact.id} className="transition hover:bg-slate-50">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-cyan-600 text-white">
                            <UserRound size={18} />
                          </div>
                          <div>
                            <p className="font-semibold text-slate-950">{contact.fullName}</p>
                            <p className="text-xs text-slate-500">ID #{contact.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Building2 size={16} className="text-slate-400" />
                          {contact.customerName}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Mail size={16} className="text-slate-400" />
                          {contact.email ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Phone size={16} className="text-slate-400" />
                          {contact.phone ?? '-'}
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">{contact.position ?? '-'}</td>

                      <td className="px-5 py-4">
                        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(contact.status)}`}>
                          {contact.status}
                        </span>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleContacts.length === 0 && (
                  <tr>
                    <td className="px-5 py-10 text-center text-slate-500" colSpan={6}>
                      No contacts found
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