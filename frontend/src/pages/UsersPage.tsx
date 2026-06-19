import { useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import { Activity, Mail, ShieldCheck, UserRound, Users } from 'lucide-react'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, GlassCard, PageShell, SearchPanel, StatTile } from '../components/ui'
import { getUsers, type PageResponse, type UserResponse } from '../services/userService'

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

  return 'border-slate-400/20 bg-slate-400/10 text-[var(--crm-text-muted)]'
}

function roleBadgeClass(role: string) {
  if (role.includes('ADMIN')) {
    return 'border-red-400/30 bg-red-400/10 text-[var(--crm-danger-text)]'
  }

  if (role.includes('MANAGER')) {
    return 'border-violet-400/30 bg-violet-400/10 text-violet-400'
  }

  if (role.includes('SALES')) {
    return 'border-blue-400/30 bg-blue-500/10 text-[var(--crm-primary)]'
  }

  return 'border-amber-400/30 bg-amber-400/10 text-[var(--crm-warning-text)]'
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

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadUsers(keyword)
  }

  const visibleUsers = users?.content ?? []
  const activeUsers = visibleUsers.filter((user) => user.status === 'ACTIVE').length
  const adminUsers = visibleUsers.filter((user) => user.role.includes('ADMIN')).length

  return (
    <AppLayout>
      <PageShell
        title="Users"
        description="Review employee accounts, roles, and status."
      >
        <motion.section
          className="grid gap-4 sm:grid-cols-3"
          variants={containerAnimation}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={cardAnimation}>
            <StatTile label="Visible" value={visibleUsers.length} icon={Users} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Active" value={activeUsers} icon={Activity} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Admins" value={adminUsers} icon={ShieldCheck} tone="red" />
          </motion.div>
        </motion.section>

        <SearchPanel
          value={keyword}
          onChange={setKeyword}
          onSubmit={handleSearch}
          placeholder="Search users by name, email, or role"
        />

        {error && (
          <div className="rounded-2xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm font-medium text-[var(--crm-danger-text)]">
            {error}
          </div>
        )}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">User Directory</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleUsers.length} of {users?.totalElements ?? 0} users
              </p>
            </div>

            <div className="rounded-xl bg-[var(--crm-soft-gradient)] p-3 text-[var(--crm-primary)] ring-1 ring-violet-300/25">
              <Users size={22} />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">User</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Role</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td className="px-5 py-8 text-center text-[var(--crm-text-muted)]" colSpan={4}>
                      Loading users...
                    </td>
                  </tr>
                )}

                {!loading &&
                  visibleUsers.map((user) => (
                    <tr key={user.id} className="transition hover:bg-violet-500/5">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/25">
                            <UserRound size={18} />
                          </div>
                          <div>
                            <p className="font-semibold text-[var(--crm-text)]">{user.fullName}</p>
                            <p className="text-xs text-[var(--crm-text-muted)]">ID #{user.id}</p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Mail size={16} />
                          {user.email}
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold ring-1 ${roleBadgeClass(
                            user.role,
                          )}`}
                        >
                          <ShieldCheck size={13} />
                          {user.role}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold ring-1 ${statusBadgeClass(
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
                  <EmptyState
                    icon={Users}
                    title="No users found"
                    message="Try a different name, email, or role."
                    colSpan={4}
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
