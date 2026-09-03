import { Building2, LogOut, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { clearStoredWorkspaceId } from '../lib/workspaceStorage'
import { logout } from '../services/authService'
import { useWorkspace } from '../workspace/useWorkspace'

export function WorkspaceAccessGate() {
  const [signingOut, setSigningOut] = useState(false)
  const navigate = useNavigate()
  const {
    workspaces,
    status,
    selectWorkspace,
    reloadWorkspaces,
  } = useWorkspace()

  const loadFailed = status === 'error'

  async function handleSignOut() {
    setSigningOut(true)
    const refreshToken = localStorage.getItem('refreshToken')

    if (refreshToken) {
      await logout(refreshToken).catch(() => undefined)
    }

    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    clearStoredWorkspaceId()
    navigate('/', { replace: true })
  }

  return (
    <main className="grid min-h-screen place-items-center bg-[var(--crm-bg)] px-4 text-[var(--crm-text)]">
      <section className="w-full max-w-lg rounded-3xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-6 shadow-[var(--crm-shadow-soft)] sm:p-8">
        <span className="grid h-12 w-12 place-items-center rounded-2xl bg-violet-500/10 text-[var(--crm-primary)]">
          <Building2 size={22} />
        </span>

        <h1 className="mt-5 text-2xl font-semibold">
          {loadFailed
            ? 'Workspaces could not be loaded'
            : workspaces.length > 0
              ? 'Choose a workspace'
              : 'No workspace available'}
        </h1>
        <p className="mt-2 text-sm leading-6 text-[var(--crm-text-muted)]">
          {loadFailed
            ? 'Check your connection and try again.'
            : workspaces.length > 0
              ? 'Select the organization you want to work in.'
              : 'Ask an organization owner to invite this account.'}
        </p>

        {workspaces.length > 0 && !loadFailed && (
          <div className="mt-6 space-y-2">
            {workspaces.map((workspace) => (
              <button
                key={workspace.organizationId}
                type="button"
                onClick={() => selectWorkspace(workspace.organizationId)}
                className="flex min-h-14 w-full items-center gap-3 rounded-2xl border border-[var(--crm-border)] px-4 text-left transition hover:border-violet-300 hover:bg-violet-500/10"
              >
                <Building2
                  size={18}
                  className="shrink-0 text-[var(--crm-primary)]"
                />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold">
                    {workspace.name}
                  </span>
                  <span className="block text-xs capitalize text-[var(--crm-text-muted)]">
                    {workspace.role.replaceAll('_', ' ').toLowerCase()}
                  </span>
                </span>
              </button>
            ))}
          </div>
        )}

        {(loadFailed || workspaces.length === 0) && (
          <button
            type="button"
            onClick={() => void reloadWorkspaces()}
            className="crm-primary-action mt-6 inline-flex h-11 items-center gap-2 rounded-2xl px-4 text-sm font-semibold"
          >
            <RefreshCw size={16} />
            Try again
          </button>
        )}

        <button
          type="button"
          onClick={() => void handleSignOut()}
          disabled={signingOut}
          className="mt-4 inline-flex h-11 items-center gap-2 rounded-2xl px-3 text-sm font-semibold text-[var(--crm-text-muted)] transition hover:bg-violet-500/10 hover:text-[var(--crm-text)] disabled:cursor-not-allowed disabled:opacity-60"
        >
          <LogOut size={16} />
          {signingOut ? 'Signing out...' : 'Sign out'}
        </button>
      </section>
    </main>
  )
}
