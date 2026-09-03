import { useState } from 'react'
import { Building2, Check, ChevronsUpDown } from 'lucide-react'
import { useWorkspace } from '../workspace/useWorkspace'

function formatRole(role: string) {
  return role.replaceAll('_', ' ').toLowerCase()
}

export function WorkspaceSelector() {
  const [open, setOpen] = useState(false)
  const { activeWorkspace, selectWorkspace, workspaces } = useWorkspace()

  if (!activeWorkspace) return null

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => {
          if (workspaces.length > 1) setOpen((value) => !value)
        }}
        className="flex h-11 w-full items-center gap-2 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 text-left shadow-sm transition hover:border-violet-300"
        aria-label="Switch workspace"
        aria-expanded={workspaces.length > 1 ? open : undefined}
        aria-haspopup={workspaces.length > 1 ? 'menu' : undefined}
      >
        <Building2
          size={17}
          className="shrink-0 text-[var(--crm-primary)]"
        />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-semibold text-[var(--crm-text)]">
            {activeWorkspace.name}
          </span>
        </span>
        {workspaces.length > 1 && (
          <ChevronsUpDown
            size={15}
            className="shrink-0 text-[var(--crm-text-muted)]"
          />
        )}
      </button>

      {open && workspaces.length > 1 && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 cursor-default"
            aria-label="Close workspace menu"
            onClick={() => setOpen(false)}
          />
          <div
            role="menu"
            className="absolute left-0 top-12 z-50 w-72 rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-2 shadow-[var(--crm-shadow-soft)]"
          >
            <p className="px-3 pb-2 pt-1 text-xs font-semibold uppercase tracking-[0.16em] text-[var(--crm-text-muted)]">
              Workspaces
            </p>
            {workspaces.map((workspace) => {
              const selected =
                workspace.organizationId === activeWorkspace.organizationId

              return (
                <button
                  key={workspace.organizationId}
                  type="button"
                  role="menuitemradio"
                  aria-checked={selected}
                  onClick={() => selectWorkspace(workspace.organizationId)}
                  className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition hover:bg-violet-500/10"
                >
                  <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-violet-500/10 text-[var(--crm-primary)]">
                    <Building2 size={17} />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-semibold text-[var(--crm-text)]">
                      {workspace.name}
                    </span>
                    <span className="block text-xs capitalize text-[var(--crm-text-muted)]">
                      {formatRole(workspace.role)}
                    </span>
                  </span>
                  {selected && (
                    <Check size={16} className="text-emerald-500" />
                  )}
                </button>
              )
            })}
          </div>
        </>
      )}
    </div>
  )
}
