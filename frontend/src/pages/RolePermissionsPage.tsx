import { useEffect, useMemo, useState } from 'react'
import { Lock, RotateCcw, Save, ShieldCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { AppLayout } from '../layouts/AppLayout'
import {
  ErrorState,
  GlassCard,
  LoadingState,
  PageActionButton,
  PageShell,
} from '../components/ui'
import { permissionGroups } from '../lib/permissions'
import { formatRole, humanizeEnum } from '../lib/formatters'
import { getLoadErrorMessage, getSaveErrorMessage } from '../lib/errors'
import {
  getPermissions,
  getRolePermissions,
  updateRolePermissions,
  type PermissionName,
  type PermissionResponse,
  type RolePermissionResponse,
} from '../services/permissionService'
import type { RoleName } from '../services/userService'

export function RolePermissionsPage() {
  const [permissions, setPermissions] = useState<PermissionResponse[]>([])
  const [roles, setRoles] = useState<RolePermissionResponse[]>([])
  const [selectedRoleName, setSelectedRoleName] = useState<RoleName | null>(null)
  const [draftPermissions, setDraftPermissions] = useState<Set<PermissionName>>(new Set())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [loadKey, setLoadKey] = useState(0)

  useEffect(() => {
    let cancelled = false

    Promise.all([getPermissions(), getRolePermissions()])
      .then(([permissionData, roleData]) => {
        if (cancelled) return

        const initialRole =
          roleData.find((role) => role.name === 'MANAGER') ?? roleData[0]

        setPermissions(permissionData)
        setRoles(roleData)
        setSelectedRoleName(initialRole?.name ?? null)
        setDraftPermissions(new Set(initialRole?.permissions ?? []))
      })
      .catch(() => {
        if (!cancelled) {
          setError(getLoadErrorMessage('roles and permissions'))
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [loadKey])

  const selectedRole = useMemo(
    () => roles.find((role) => role.name === selectedRoleName) ?? null,
    [roles, selectedRoleName],
  )

  const permissionDetails = useMemo(
    () => new Map(permissions.map((permission) => [permission.name, permission])),
    [permissions],
  )

  const hasChanges = useMemo(() => {
    if (!selectedRole) return false

    const saved = new Set(selectedRole.permissions)

    return (
      saved.size !== draftPermissions.size ||
      [...draftPermissions].some((permission) => !saved.has(permission))
    )
  }, [draftPermissions, selectedRole])

  function selectRole(role: RolePermissionResponse) {
    if (hasChanges) return

    setSelectedRoleName(role.name)
    setDraftPermissions(new Set(role.permissions))
  }

  function togglePermission(permission: PermissionName) {
    if (!selectedRole?.editable) return

    setDraftPermissions((current) => {
      const next = new Set(current)

      if (next.has(permission)) next.delete(permission)
      else next.add(permission)

      return next
    })
  }

  function resetChanges() {
    if (!selectedRole) return
    setDraftPermissions(new Set(selectedRole.permissions))
  }

  async function saveChanges() {
    if (!selectedRole || !selectedRole.editable || !hasChanges) return

    const confirmed = window.confirm(
      `Save permission changes for ${formatRole(selectedRole.name)}?`,
    )

    if (!confirmed) return

    setSaving(true)

    try {
      const updatedRole = await updateRolePermissions(
        selectedRole.name,
        [...draftPermissions],
      )

      setRoles((current) =>
        current.map((role) =>
          role.name === updatedRole.name ? updatedRole : role,
        ),
      )
      setDraftPermissions(new Set(updatedRole.permissions))
      toast.success('Permissions updated')
    } catch {
      toast.error(getSaveErrorMessage('role permissions'))
    } finally {
      setSaving(false)
    }
  }

  function retryLoading() {
    setLoading(true)
    setError('')
    setLoadKey((current) => current + 1)
  }

  return (
    <AppLayout>
      <PageShell
        title="Roles & permissions"
        description="Control what each team role can view and change."
        action={
          selectedRole?.editable ? (
            <PageActionButton
              icon={Save}
              disabled={!hasChanges || saving}
              onClick={saveChanges}
            >
              {saving ? 'Saving...' : 'Save changes'}
            </PageActionButton>
          ) : undefined
        }
      >
        {loading ? (
          <GlassCard>
            <LoadingState message="Loading roles and permissions..." />
          </GlassCard>
        ) : error ? (
          <ErrorState message={error} onRetry={retryLoading} />
        ) : (
          <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
            <GlassCard className="h-fit">
              <h2 className="text-sm font-semibold">Team roles</h2>

              <div className="mt-4 space-y-2">
                {roles.map((role) => {
                  const active = role.name === selectedRoleName

                  return (
                    <button
                      key={role.name}
                      type="button"
                      disabled={hasChanges && !active}
                      onClick={() => selectRole(role)}
                      className={`flex w-full items-center justify-between rounded-xl border px-3 py-3 text-left transition ${
                        active
                          ? 'border-violet-400 bg-violet-500/10'
                          : 'border-[var(--crm-border)] hover:bg-violet-500/5'
                      } disabled:cursor-not-allowed disabled:opacity-50`}
                    >
                      <span>
                        <span className="block text-sm font-semibold">
                          {formatRole(role.name)}
                        </span>
                        <span className="text-xs text-[var(--crm-text-muted)]">
                          {role.permissions.length} permissions
                        </span>
                      </span>

                      {role.editable ? (
                        <ShieldCheck size={17} />
                      ) : (
                        <Lock size={17} />
                      )}
                    </button>
                  )
                })}
              </div>

              {hasChanges && (
                <p className="mt-4 text-xs text-amber-600">
                  Save or discard changes before selecting another role.
                </p>
              )}
            </GlassCard>

            <GlassCard>
              <div className="flex items-start justify-between gap-4 border-b border-[var(--crm-border)] pb-5">
                <div>
                  <h2 className="text-lg font-semibold">
                    {formatRole(selectedRole?.name)}
                  </h2>
                  <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                    {selectedRole?.description}
                  </p>
                </div>

                {hasChanges && (
                  <button
                    type="button"
                    onClick={resetChanges}
                    className="inline-flex items-center gap-2 text-sm font-semibold text-[var(--crm-primary)]"
                  >
                    <RotateCcw size={15} />
                    Discard
                  </button>
                )}
              </div>

              <div className="divide-y divide-[var(--crm-border)]">
                {permissionGroups.map((group) => (
                  <section key={group.id} className="py-5">
                    <h3 className="text-sm font-semibold">{group.label}</h3>
                    <p className="mt-1 text-xs text-[var(--crm-text-muted)]">
                      {group.description}
                    </p>

                    <div className="mt-4 grid gap-3 md:grid-cols-2">
                      {group.permissions.map((permissionName) => (
                        <label
                          key={permissionName}
                          className="flex items-start gap-3 rounded-xl border border-[var(--crm-border)] p-3"
                        >
                          <input
                            type="checkbox"
                            checked={draftPermissions.has(permissionName)}
                            disabled={!selectedRole?.editable}
                            onChange={() => togglePermission(permissionName)}
                            className="mt-1 h-4 w-4 accent-violet-600"
                          />

                          <span>
                            <span className="block text-sm font-medium">
                              {humanizeEnum(permissionName)}
                            </span>
                            <span className="text-xs text-[var(--crm-text-muted)]">
                              {permissionDetails.get(permissionName)?.description}
                            </span>
                          </span>
                        </label>
                      ))}
                    </div>
                  </section>
                ))}
              </div>
            </GlassCard>
          </div>
        )}
      </PageShell>
    </AppLayout>
  )
}