import { useEffect, useState } from 'react'
import { Activity, Mail, Search, ShieldCheck, UserRound, Users } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { getUsers, type PageResponse, type UserResponse } from '../services/userService'

function statusBadgeClass(status: string) {
  if (status === 'ACTIVE') {
    return 'bg-emerald-50 text-emerald-700 ring-emerald-200'
  }

  return 'bg-slate-100 text-slate-600 ring-slate-200'
}

function roleBadgeClass(role: string) {
  if (role.includes('ADMIN')) {
    return 'bg-rose-50 text-rose-700 ring-rose-200'
  }

  if (role.includes('MANAGER')) {
    return 'bg-violet-50 text-violet-700 ring-violet-200'
  }

  if (role.includes('SALES')) {
    return 'bg-blue-50 text-blue-700 ring-blue-200'
  }

  return 'bg-amber-50 text-amber-700 ring-amber-200'
}

export function UsersPage() {
  const [users, setUsers] = useState<PageResponse<UserResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function loadUsers(search: string) {
    setLoading(true)
    setError('')

    getUsers(0, 10, search)
      .then(setUsers)
      .catch(() => setError('Could not load users. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let ignore = false

    getUsers(0, 10, '')
      .then((data) => {
        if (!ignore) {
          setUsers(data)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError('Could not load users. Please try again.')
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
    loadUsers(keyword)
  }

  const visibleUsers = users?.content ?? []
  const activeUsers = visibleUsers.filter((user) => user.status === 'ACTIVE').length
  const adminUsers = visibleUsers.filter((user) => user.role.includes('ADMIN')).length

  return (
    <AppLayout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-slate-950 px-6 py-6 text-white">
            <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
              <div>
                <p className="text-sm font-medium text-blue-300">Administration</p>
                <h2 className="mt-2 text-3xl font-semibold">Users</h2>
                <p className="mt-2 max-w-2xl text-sm text-slate-300">
                  Manage employee access, roles, account status, and CRM user visibility.
                </p>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Visible</p>
                  <p className="mt-1 text-xl font-semibold">{visibleUsers.length}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Active</p>
                  <p className="mt-1 text-xl font-semibold">{activeUsers}</p>
                </div>
                <div className="rounded-lg border border-white/10 bg-white/10 px-4 py-3">
                  <p className="text-xs text-slate-400">Admins</p>
                  <p className="mt-1 text-xl font-semibold">{adminUsers}</p>
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
                  placeholder="Search by name, email, or role"
                  className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />
              </div>

              <button className="h-11 rounded-lg bg-blue-600 px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700">
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
              <h3 className="font-semibold text-slate-950">User Directory</h3>
              <p className="text-sm text-slate-500">
                Showing {visibleUsers.length} of {users?.totalElements ?? 0} users
              </p>
            </div>

            <div className="rounded-lg bg-blue-50 p-3 text-blue-700">
              <Users size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-semibold">User</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Role</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-slate-500" colSpan={4}>
                      Loading users...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleUsers.map((user) => (
                    <tr key={user.id} className="transition hover:bg-slate-50">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-950 text-white">
                            <UserRound size={18} />
                          </div>
                          <div>
                            <p className="font-semibold text-slate-950">{user.fullName}</p>
                            <p className="text-xs text-slate-500">ID #{user.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-slate-600">
                        <div className="flex items-center gap-2">
                          <Mail size={16} className="text-slate-400" />
                          {user.email}
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${roleBadgeClass(
                            user.role,
                          )}`}
                        >
                          <ShieldCheck size={13} />
                          {user.role}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
                            user.status,
                          )}`}
                        >
                          <Activity size={13} />
                          {user.status}
                        </span>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleUsers.length === 0 && (
                  <tr>
                    <td className="px-5 py-10 text-center text-slate-500" colSpan={4}>
                      No users found
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
