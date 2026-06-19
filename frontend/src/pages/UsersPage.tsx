import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { motion, type Variants } from 'framer-motion'
import toast from 'react-hot-toast'
import { Activity, Mail, ShieldCheck, UserPlus, UserRound, UserX, Users } from 'lucide-react'
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
  ErrorState
} from '../components/ui'
import {
  deactivateUser,
  getUsers,
  type PageResponse,
  type UserResponse,
} from '../services/userService'
import { formatRole, formatStatus, getEmptyMessage, statusVariant } from '../lib/formatters'
import { openQuickCreate } from '../lib/quickCreate'
import { getLoadErrorMessage, getSaveErrorMessage } from '../lib/errors'

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

export function UsersPage() {
  const [users, setUsers] = useState<PageResponse<UserResponse> | null>(null)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)

  const loadUsers = useCallback((search: string) => {
    setLoading(true)
    setError('')

    getUsers(0, 10, search)
      .then(setUsers)
      .catch(() => setError(getLoadErrorMessage('team members')))
      .finally(() => setLoading(false))
  }, [])

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
          setError(getLoadErrorMessage('team members'))
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
      loadUsers(keyword)
    }

    window.addEventListener('crm-data-changed', refreshAfterCreate)
    return () => window.removeEventListener('crm-data-changed', refreshAfterCreate)
  }, [keyword, loadUsers])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loadUsers(keyword)
  }

  async function handleDeactivate(user: UserResponse) {
    if (!window.confirm(`Deactivate ${user.fullName}? They will no longer be able to sign in.`)) {
      return
    }

    setActionLoadingId(user.id)

    try {
      await deactivateUser(user.id)
      toast.success(`${user.fullName} deactivated`)
      loadUsers(keyword)
    } catch {
      toast.error(getSaveErrorMessage('user'))
    } finally {
      setActionLoadingId(null)
    }
  }

  const visibleUsers = users?.content ?? []
  const activeUsers = visibleUsers.filter((user) => user.status === 'ACTIVE').length
  const adminUsers = visibleUsers.filter((user) => user.role.includes('ADMIN')).length
  const hasSearch = keyword.trim().length > 0

  return (
    <AppLayout>
      <PageShell
        title="Team & access"
        description="Manage the people who can use Tadamun and the access level each person has."
        action={
          <PageActionButton icon={UserPlus} onClick={() => openQuickCreate('user')}>
            Invite user
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
            <StatTile label="Team members" value={visibleUsers.length} icon={Users} tone="blue" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Active access" value={activeUsers} icon={Activity} tone="green" />
          </motion.div>

          <motion.div variants={cardAnimation}>
            <StatTile label="Administrators" value={adminUsers} icon={ShieldCheck} tone="red" />
          </motion.div>
        </motion.section>

        <SearchPanel
          value={keyword}
          onChange={setKeyword}
          onSubmit={handleSearch}
          placeholder="Search team members by name, email, or role"
        />

        {error && <ErrorState message={error} onRetry={() => loadUsers(keyword)} />}

        <GlassCard className="overflow-hidden p-0">
          <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
            <div>
              <h3 className="font-semibold text-[var(--crm-text)]">Team members</h3>
              <p className="text-sm text-[var(--crm-text-muted)]">
                Showing {visibleUsers.length} of {users?.totalElements ?? 0} accounts
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[860px] border-collapse text-left text-sm">
              <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                <tr>
                  <th className="px-5 py-3 font-semibold">Person</th>
                  <th className="px-5 py-3 font-semibold">Email</th>
                  <th className="px-5 py-3 font-semibold">Access</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 text-right font-semibold">Action</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--crm-border)]">
                {loading && (
                  <tr>
                    <td colSpan={7}>
                      <LoadingState message="Loading leads..." />
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
                          <p className="font-semibold text-[var(--crm-text)]">{user.fullName}</p>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[var(--crm-text-muted)]">
                        <div className="flex items-center gap-2">
                          <Mail size={16} />
                          {user.email}
                        </div>
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={statusVariant(user.role)}>{formatRole(user.role)}</StatusBadge>
                      </td>

                      <td className="px-5 py-4">
                        <StatusBadge variant={statusVariant(user.status)}>
                          {formatStatus(user.status)}
                        </StatusBadge>
                      </td>

                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          onClick={() => handleDeactivate(user)}
                          disabled={user.status !== 'ACTIVE' || actionLoadingId === user.id}
                          className="inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-red-300 hover:bg-red-400/10 hover:text-[var(--crm-danger-text)] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          <UserX size={14} />
                          {actionLoadingId === user.id ? 'Saving...' : 'Deactivate'}
                        </button>
                      </td>
                    </tr>
                  ))}

                {!loading && visibleUsers.length === 0 && (
                  <EmptyState
                    icon={Users}
                    title={hasSearch ? 'No team members found' : 'No team members yet'}
                    message={getEmptyMessage(hasSearch, 'team members', 'Invite user')}
                    colSpan={5}
                    action={
                      !hasSearch && (
                        <PageActionButton icon={UserPlus} onClick={() => openQuickCreate('user')}>
                          Invite user
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
