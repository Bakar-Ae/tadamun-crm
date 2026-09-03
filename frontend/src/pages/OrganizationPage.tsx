import { useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  Ban,
  Building2,
  Clock3,
  History,
  Mail,
  Pencil,
  Send,
  Settings2,
  ShieldCheck,
  UserMinus,
  UserPlus,
  UserRound,
  Users,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { NavLink, useSearchParams } from 'react-router'
import { AppLayout } from '../layouts/AppLayout'
import {
  EmptyState,
  ErrorState,
  GlassCard,
  LoadingState,
  Modal,
  PageActionButton,
  PageShell,
  PaginationBar,
  SelectField,
  StatusBadge,
  TextField,
} from '../components/ui'
import {
  createOrganizationInvitation,
  getOrganizationInvitations,
  revokeOrganizationInvitation,
  type OrganizationInvitationResponse,
} from '../services/organizationInvitationService'
import {
  deactivateOrganizationMember,
  getCurrentOrganization,
  getOrganizationMembers,
  updateCurrentOrganization,
  updateOrganizationMemberRole,
  type OrganizationMembershipResponse,
  type OrganizationResponse,
} from '../services/organizationService'
import { getAuditLogs, type AuditLogResponse } from '../services/auditLogService'
import type { PageResponse, RoleName } from '../services/userService'
import {
  formatAuditAction,
  formatAuditDetails,
  formatDateTime,
  formatRole,
  formatStatus,
  statusVariant,
} from '../lib/formatters'
import { useWorkspace } from '../workspace/useWorkspace'

type OrganizationTab = 'settings' | 'members' | 'invitations' | 'activity'

const timeZones = [
  'Africa/Mogadishu',
  'Africa/Nairobi',
  'UTC',
  'Asia/Dubai',
  'Asia/Riyadh',
  'Asia/Kolkata',
  'Europe/Istanbul',
  'Europe/London',
  'America/New_York',
]

const memberRoles: Array<Exclude<RoleName, 'OWNER'>> = [
  'ADMIN',
  'MANAGER',
  'SALES_REP',
  'SUPPORT_STAFF',
]

function getApiMessage(error: unknown, fallback: string) {
  const apiError = error as {
    response?: { data?: { message?: string } }
    message?: string
  }

  return apiError.response?.data?.message ?? apiError.message ?? fallback
}

export function OrganizationPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { activeWorkspace, reloadWorkspaces } = useWorkspace()
  const permissions = useMemo(
    () => new Set(activeWorkspace?.permissions ?? []),
    [activeWorkspace],
  )
  const canUpdateOrganization = permissions.has('ORGANIZATION_UPDATE')
  const canViewMembers = permissions.has('MEMBERSHIP_VIEW')
  const canInviteMembers = permissions.has('MEMBERSHIP_INVITE')
  const canUpdateMembers = permissions.has('MEMBERSHIP_UPDATE')
  const canDeactivateMembers = permissions.has('MEMBERSHIP_DEACTIVATE')
  const canViewAudit = permissions.has('AUDIT_LOG_VIEW')

  const [organization, setOrganization] = useState<OrganizationResponse | null>(null)
  const [organizationName, setOrganizationName] = useState('')
  const [organizationTimeZone, setOrganizationTimeZone] = useState('Africa/Mogadishu')
  const [organizationLoading, setOrganizationLoading] = useState(true)
  const [organizationSaving, setOrganizationSaving] = useState(false)
  const [organizationError, setOrganizationError] = useState('')
  const [organizationLoadKey, setOrganizationLoadKey] = useState(0)

  const [members, setMembers] = useState<PageResponse<OrganizationMembershipResponse> | null>(null)
  const [memberPage, setMemberPage] = useState(0)
  const [memberPageSize, setMemberPageSize] = useState(10)
  const [membersLoading, setMembersLoading] = useState(canViewMembers)
  const [membersError, setMembersError] = useState('')
  const [membersLoadKey, setMembersLoadKey] = useState(0)
  const [editingMember, setEditingMember] = useState<OrganizationMembershipResponse | null>(null)
  const [memberRole, setMemberRole] = useState<Exclude<RoleName, 'OWNER'>>('SALES_REP')
  const [memberSaving, setMemberSaving] = useState(false)
  const [memberActionId, setMemberActionId] = useState<number | null>(null)

  const [invitations, setInvitations] = useState<PageResponse<OrganizationInvitationResponse> | null>(null)
  const [invitationPage, setInvitationPage] = useState(0)
  const [invitationPageSize, setInvitationPageSize] = useState(10)
  const [invitationsLoading, setInvitationsLoading] = useState(canViewMembers)
  const [invitationsError, setInvitationsError] = useState('')
  const [invitationsLoadKey, setInvitationsLoadKey] = useState(0)
  const [inviteOpen, setInviteOpen] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<Exclude<RoleName, 'OWNER'>>('SALES_REP')
  const [inviteError, setInviteError] = useState('')
  const [inviteSaving, setInviteSaving] = useState(false)
  const [invitationActionId, setInvitationActionId] = useState<number | null>(null)

  const [activity, setActivity] = useState<AuditLogResponse[]>([])
  const [activityLoading, setActivityLoading] = useState(canViewAudit)
  const [activityError, setActivityError] = useState('')
  const [activityLoadKey, setActivityLoadKey] = useState(0)

  const visibleTabs = useMemo(
    () => [
      { id: 'settings' as const, label: 'Settings', icon: Settings2, visible: true },
      { id: 'members' as const, label: 'Members', icon: Users, visible: canViewMembers },
      { id: 'invitations' as const, label: 'Invitations', icon: Mail, visible: canViewMembers },
      { id: 'activity' as const, label: 'Activity', icon: History, visible: canViewAudit },
    ].filter((tab) => tab.visible),
    [canViewAudit, canViewMembers],
  )
  const requestedTab = searchParams.get('tab') as OrganizationTab | null
  const activeTab = visibleTabs.some((tab) => tab.id === requestedTab)
    ? requestedTab as OrganizationTab
    : visibleTabs[0].id

  const assignableRoles = useMemo(
    () => activeWorkspace?.role === 'OWNER'
      ? memberRoles
      : memberRoles.filter((role) => role !== 'ADMIN'),
    [activeWorkspace?.role],
  )

  useEffect(() => {
    let cancelled = false

    getCurrentOrganization()
      .then((data) => {
        if (cancelled) return
        setOrganization(data)
        setOrganizationName(data.name)
        setOrganizationTimeZone(data.timeZone)
        setOrganizationError('')
      })
      .catch((error) => {
        if (!cancelled) {
          setOrganizationError(getApiMessage(error, 'Could not load organization settings.'))
        }
      })
      .finally(() => {
        if (!cancelled) setOrganizationLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [organizationLoadKey])

  useEffect(() => {
    if (!canViewMembers) return
    let cancelled = false

    getOrganizationMembers(memberPage, memberPageSize)
      .then((data) => {
        if (!cancelled) {
          setMembers(data)
          setMembersError('')
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setMembersError(getApiMessage(error, 'Could not load workspace members.'))
        }
      })
      .finally(() => {
        if (!cancelled) setMembersLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [canViewMembers, memberPage, memberPageSize, membersLoadKey])

  useEffect(() => {
    if (!canViewMembers) return
    let cancelled = false

    getOrganizationInvitations(invitationPage, invitationPageSize)
      .then((data) => {
        if (!cancelled) {
          setInvitations(data)
          setInvitationsError('')
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setInvitationsError(getApiMessage(error, 'Could not load invitations.'))
        }
      })
      .finally(() => {
        if (!cancelled) setInvitationsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [canViewMembers, invitationPage, invitationPageSize, invitationsLoadKey])

  useEffect(() => {
    if (!canViewAudit) return
    let cancelled = false

    getAuditLogs(0, 20, { keyword: 'ORGANIZATION_' })
      .then((data) => {
        if (!cancelled) {
          setActivity(data.content)
          setActivityError('')
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setActivityError(getApiMessage(error, 'Could not load organization activity.'))
        }
      })
      .finally(() => {
        if (!cancelled) setActivityLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [activityLoadKey, canViewAudit])

  function selectTab(tab: OrganizationTab) {
    setSearchParams({ tab })
  }

  function retryOrganization() {
    setOrganizationLoading(true)
    setOrganizationError('')
    setOrganizationLoadKey((key) => key + 1)
  }

  function retryMembers() {
    setMembersLoading(true)
    setMembersError('')
    setMembersLoadKey((key) => key + 1)
  }

  function retryInvitations() {
    setInvitationsLoading(true)
    setInvitationsError('')
    setInvitationsLoadKey((key) => key + 1)
  }

  function retryActivity() {
    setActivityLoading(true)
    setActivityError('')
    setActivityLoadKey((key) => key + 1)
  }

  async function saveOrganization(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!organization || !canUpdateOrganization) return

    const name = organizationName.trim()
    const timeZone = organizationTimeZone.trim()

    if (!name || !timeZone) {
      setOrganizationError('Organization name and time zone are required.')
      return
    }

    setOrganizationSaving(true)
    setOrganizationError('')

    try {
      const updated = await updateCurrentOrganization({
        name,
        timeZone,
        version: organization.version,
      })
      setOrganization(updated)
      setOrganizationName(updated.name)
      setOrganizationTimeZone(updated.timeZone)
      await reloadWorkspaces()
      toast.success('Organization settings saved')
      retryActivity()
    } catch (error) {
      setOrganizationError(getApiMessage(error, 'Could not save organization settings.'))
    } finally {
      setOrganizationSaving(false)
    }
  }

  function openRoleEditor(member: OrganizationMembershipResponse) {
    if (member.role === 'OWNER') return
    setEditingMember(member)
    setMemberRole(member.role)
  }

  async function saveMemberRole(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!editingMember) return

    setMemberSaving(true)

    try {
      const updated = await updateOrganizationMemberRole(
        editingMember.id,
        memberRole,
        editingMember.version,
      )
      setMembers((current) => current
        ? {
            ...current,
            content: current.content.map((member) => member.id === updated.id ? updated : member),
          }
        : current)
      setEditingMember(null)
      toast.success('Member role updated')
      retryActivity()
    } catch (error) {
      toast.error(getApiMessage(error, 'Could not update member role.'))
    } finally {
      setMemberSaving(false)
    }
  }

  async function deactivateMember(member: OrganizationMembershipResponse) {
    const confirmed = window.confirm(
      `Remove ${member.userFullName}'s access to ${organization?.name ?? 'this workspace'}?`,
    )
    if (!confirmed) return

    setMemberActionId(member.id)

    try {
      const updated = await deactivateOrganizationMember(member.id, member.version)
      setMembers((current) => current
        ? {
            ...current,
            content: current.content.map((item) => item.id === updated.id ? updated : item),
          }
        : current)
      toast.success('Workspace access removed')
      retryActivity()
    } catch (error) {
      toast.error(getApiMessage(error, 'Could not remove workspace access.'))
    } finally {
      setMemberActionId(null)
    }
  }

  function openInvitation() {
    setInviteEmail('')
    setInviteRole(assignableRoles[0] ?? 'SALES_REP')
    setInviteError('')
    setInviteOpen(true)
  }

  async function sendInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const email = inviteEmail.trim()

    if (!email) {
      setInviteError('Email is required.')
      return
    }

    setInviteSaving(true)
    setInviteError('')

    try {
      await createOrganizationInvitation({ email, role: inviteRole })
      setInviteOpen(false)
      setInvitationPage(0)
      retryInvitations()
      retryActivity()
      toast.success('Invitation sent')
    } catch (error) {
      setInviteError(getApiMessage(error, 'Could not send invitation.'))
    } finally {
      setInviteSaving(false)
    }
  }

  async function revokeInvitation(invitation: OrganizationInvitationResponse) {
    if (!window.confirm(`Revoke the invitation for ${invitation.email}?`)) return
    setInvitationActionId(invitation.id)

    try {
      const updated = await revokeOrganizationInvitation(invitation.id)
      setInvitations((current) => current
        ? {
            ...current,
            content: current.content.map((item) => item.id === updated.id ? updated : item),
          }
        : current)
      toast.success('Invitation revoked')
      retryActivity()
    } catch (error) {
      toast.error(getApiMessage(error, 'Could not revoke invitation.'))
    } finally {
      setInvitationActionId(null)
    }
  }

  function canManageMember(member: OrganizationMembershipResponse) {
    if (member.id === activeWorkspace?.membershipId || member.role === 'OWNER') return false
    if (member.role === 'ADMIN' && activeWorkspace?.role !== 'OWNER') return false
    return member.status === 'ACTIVE'
  }

  const organizationDirty = Boolean(
    organization && (
      organizationName.trim() !== organization.name ||
      organizationTimeZone !== organization.timeZone
    ),
  )
  const availableTimeZones = timeZones.includes(organizationTimeZone)
    ? timeZones
    : [organizationTimeZone, ...timeZones]

  return (
    <AppLayout>
      <PageShell
        title="Organization"
        description="Manage workspace details, members, and access."
        action={canInviteMembers ? (
          <PageActionButton icon={UserPlus} onClick={openInvitation}>
            Invite member
          </PageActionButton>
        ) : undefined}
      >
        <div
          className="flex gap-1 overflow-x-auto rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface)] p-1.5 shadow-sm"
          role="tablist"
          aria-label="Organization administration"
        >
          {visibleTabs.map((tab) => {
            const Icon = tab.icon
            const selected = activeTab === tab.id

            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => selectTab(tab.id)}
                className={`inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold transition ${
                  selected
                    ? 'bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)] ring-1 ring-violet-300/30'
                    : 'text-[var(--crm-text-muted)] hover:bg-violet-500/10 hover:text-[var(--crm-text)]'
                }`}
              >
                <Icon size={16} />
                {tab.label}
              </button>
            )
          })}
        </div>

        {activeTab === 'settings' && (
          <GlassCard className="p-0">
            <div className="border-b border-[var(--crm-border)] px-5 py-4">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="font-semibold text-[var(--crm-text)]">Workspace details</h2>
                  <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                    These details are shared with everyone in this workspace.
                  </p>
                </div>
                {organization && (
                  <StatusBadge variant={statusVariant(organization.status)}>
                    {formatStatus(organization.status)}
                  </StatusBadge>
                )}
              </div>
            </div>

            {organizationLoading && <LoadingState message="Loading organization settings..." />}
            {!organizationLoading && organizationError && !organization && (
              <div className="p-5">
                <ErrorState message={organizationError} onRetry={retryOrganization} />
              </div>
            )}
            {!organizationLoading && organization && (
              <form onSubmit={saveOrganization} className="space-y-5 p-5">
                {organizationError && <ErrorState message={organizationError} />}
                <div className="grid gap-4 md:grid-cols-2">
                  <TextField
                    label="Organization name"
                    value={organizationName}
                    onChange={(event) => setOrganizationName(event.target.value)}
                    required
                    disabled={!canUpdateOrganization || organizationSaving}
                  />
                  <TextField
                    label="Workspace address"
                    value={organization.slug}
                    helperText="The workspace address cannot be changed here."
                    disabled
                  />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <SelectField
                    label="Time zone"
                    value={organizationTimeZone}
                    onChange={(event) => setOrganizationTimeZone(event.target.value)}
                    required
                    disabled={!canUpdateOrganization || organizationSaving}
                  >
                    {availableTimeZones.map((timeZone) => (
                      <option key={timeZone} value={timeZone}>{timeZone}</option>
                    ))}
                  </SelectField>
                  <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-card-subtle)] px-4 py-3">
                    <p className="text-xs font-semibold uppercase tracking-wide text-[var(--crm-text-muted)]">Created by</p>
                    <p className="mt-2 text-sm font-semibold text-[var(--crm-text)]">
                      {organization.createdByName ?? 'Account owner'}
                    </p>
                    <p className="mt-1 text-xs text-[var(--crm-text-muted)]">
                      {formatDateTime(organization.createdAt)}
                    </p>
                  </div>
                </div>
                {canUpdateOrganization && (
                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={!organizationDirty || organizationSaving}
                      className="crm-primary-action inline-flex h-11 items-center justify-center gap-2 rounded-2xl px-4 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Building2 size={16} />
                      {organizationSaving ? 'Saving...' : 'Save changes'}
                    </button>
                  </div>
                )}
              </form>
            )}
          </GlassCard>
        )}

        {activeTab === 'members' && (
          <GlassCard className="overflow-hidden p-0">
            <div className="border-b border-[var(--crm-border)] px-5 py-4">
              <h2 className="font-semibold text-[var(--crm-text)]">Workspace members</h2>
              <p className="mt-1 text-sm text-[var(--crm-text-muted)]">
                {members?.totalElements ?? 0} people have access to this workspace.
              </p>
            </div>
            {membersError && (
              <div className="p-5"><ErrorState message={membersError} onRetry={retryMembers} /></div>
            )}
            {!membersError && (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[860px] border-collapse text-left text-sm">
                  <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                    <tr>
                      <th className="px-5 py-3 font-semibold">Person</th>
                      <th className="px-5 py-3 font-semibold">Role</th>
                      <th className="px-5 py-3 font-semibold">Access</th>
                      <th className="px-5 py-3 font-semibold">Joined</th>
                      <th className="px-5 py-3 text-right font-semibold">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[var(--crm-border)]">
                    {membersLoading && (
                      <tr><td colSpan={5}><LoadingState message="Loading members..." /></td></tr>
                    )}
                    {!membersLoading && (members?.content ?? []).map((member) => {
                      const manageable = canManageMember(member)
                      return (
                        <tr key={member.id} className="transition hover:bg-violet-500/5">
                          <td className="px-5 py-4">
                            <div className="flex items-center gap-3">
                              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)]">
                                <UserRound size={18} />
                              </span>
                              <div>
                                <p className="font-semibold text-[var(--crm-text)]">{member.userFullName}</p>
                                <p className="mt-0.5 text-xs text-[var(--crm-text-muted)]">{member.userEmail}</p>
                              </div>
                            </div>
                          </td>
                          <td className="px-5 py-4"><StatusBadge variant="info">{formatRole(member.role)}</StatusBadge></td>
                          <td className="px-5 py-4"><StatusBadge variant={statusVariant(member.status)}>{formatStatus(member.status)}</StatusBadge></td>
                          <td className="px-5 py-4 text-[var(--crm-text-muted)]">{formatDateTime(member.joinedAt)}</td>
                          <td className="px-5 py-4">
                            <div className="flex justify-end gap-2">
                              {canUpdateMembers && (
                                <button
                                  type="button"
                                  onClick={() => openRoleEditor(member)}
                                  disabled={!manageable}
                                  className="inline-flex h-9 items-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text)] transition hover:bg-violet-500/10 disabled:cursor-not-allowed disabled:opacity-40"
                                >
                                  <Pencil size={14} /> Change role
                                </button>
                              )}
                              {canDeactivateMembers && (
                                <button
                                  type="button"
                                  onClick={() => deactivateMember(member)}
                                  disabled={!manageable || memberActionId === member.id}
                                  className="inline-flex h-9 items-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-red-300 hover:bg-red-500/10 hover:text-[var(--crm-danger-text)] disabled:cursor-not-allowed disabled:opacity-40"
                                >
                                  <UserMinus size={14} />
                                  {memberActionId === member.id ? 'Removing...' : 'Remove'}
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                    {!membersLoading && (members?.content.length ?? 0) === 0 && (
                      <EmptyState
                        icon={Users}
                        title="No members found"
                        message="Invite a colleague to give them access to this workspace."
                        colSpan={5}
                        action={canInviteMembers ? (
                          <PageActionButton icon={UserPlus} onClick={openInvitation}>Invite member</PageActionButton>
                        ) : undefined}
                      />
                    )}
                  </tbody>
                </table>
              </div>
            )}
            {members && (
              <PaginationBar
                page={memberPage}
                totalPages={members.totalPages}
                totalElements={members.totalElements}
                pageSize={memberPageSize}
                onPrevious={() => {
                  setMembersLoading(true)
                  setMemberPage((page) => Math.max(page - 1, 0))
                }}
                onNext={() => {
                  setMembersLoading(true)
                  setMemberPage((page) => page + 1)
                }}
                onPageSizeChange={(size) => {
                  setMembersLoading(true)
                  setMemberPageSize(size)
                  setMemberPage(0)
                }}
                disabled={membersLoading}
              />
            )}
          </GlassCard>
        )}

        {activeTab === 'invitations' && (
          <GlassCard className="overflow-hidden p-0">
            <div className="flex flex-col gap-3 border-b border-[var(--crm-border)] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="font-semibold text-[var(--crm-text)]">Invitations</h2>
                <p className="mt-1 text-sm text-[var(--crm-text-muted)]">Track pending and completed invitations.</p>
              </div>
              {canInviteMembers && (
                <button type="button" onClick={openInvitation} className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-sm font-semibold text-[var(--crm-text)] transition hover:bg-violet-500/10">
                  <Send size={15} /> Send invitation
                </button>
              )}
            </div>
            {invitationsError && (
              <div className="p-5"><ErrorState message={invitationsError} onRetry={retryInvitations} /></div>
            )}
            {!invitationsError && (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[780px] border-collapse text-left text-sm">
                  <thead className="bg-[var(--crm-card-subtle)] text-xs uppercase text-[var(--crm-text-muted)]">
                    <tr>
                      <th className="px-5 py-3 font-semibold">Email</th>
                      <th className="px-5 py-3 font-semibold">Role</th>
                      <th className="px-5 py-3 font-semibold">Status</th>
                      <th className="px-5 py-3 font-semibold">Expires</th>
                      <th className="px-5 py-3 text-right font-semibold">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[var(--crm-border)]">
                    {invitationsLoading && (
                      <tr><td colSpan={5}><LoadingState message="Loading invitations..." /></td></tr>
                    )}
                    {!invitationsLoading && (invitations?.content ?? []).map((invitation) => (
                      <tr key={invitation.id} className="transition hover:bg-violet-500/5">
                        <td className="px-5 py-4">
                          <p className="font-semibold text-[var(--crm-text)]">{invitation.email}</p>
                          <p className="mt-0.5 text-xs text-[var(--crm-text-muted)]">Sent by {invitation.invitedByUserName}</p>
                        </td>
                        <td className="px-5 py-4">{formatRole(invitation.role)}</td>
                        <td className="px-5 py-4"><StatusBadge variant={statusVariant(invitation.status)}>{formatStatus(invitation.status)}</StatusBadge></td>
                        <td className="px-5 py-4 text-[var(--crm-text-muted)]">{formatDateTime(invitation.expiresAt)}</td>
                        <td className="px-5 py-4 text-right">
                          {canUpdateMembers && invitation.status === 'PENDING' ? (
                            <button
                              type="button"
                              onClick={() => revokeInvitation(invitation)}
                              disabled={invitationActionId === invitation.id}
                              className="inline-flex h-9 items-center gap-2 rounded-xl border border-[var(--crm-border)] px-3 text-xs font-semibold text-[var(--crm-text-muted)] transition hover:border-red-300 hover:bg-red-500/10 hover:text-[var(--crm-danger-text)] disabled:opacity-50"
                            >
                              <Ban size={14} /> {invitationActionId === invitation.id ? 'Revoking...' : 'Revoke'}
                            </button>
                          ) : <span className="text-xs text-[var(--crm-text-muted)]">No action</span>}
                        </td>
                      </tr>
                    ))}
                    {!invitationsLoading && (invitations?.content.length ?? 0) === 0 && (
                      <EmptyState
                        icon={Mail}
                        title="No invitations yet"
                        message="Invitations sent from this workspace will appear here."
                        colSpan={5}
                      />
                    )}
                  </tbody>
                </table>
              </div>
            )}
            {invitations && (
              <PaginationBar
                page={invitationPage}
                totalPages={invitations.totalPages}
                totalElements={invitations.totalElements}
                pageSize={invitationPageSize}
                onPrevious={() => {
                  setInvitationsLoading(true)
                  setInvitationPage((page) => Math.max(page - 1, 0))
                }}
                onNext={() => {
                  setInvitationsLoading(true)
                  setInvitationPage((page) => page + 1)
                }}
                onPageSizeChange={(size) => {
                  setInvitationsLoading(true)
                  setInvitationPageSize(size)
                  setInvitationPage(0)
                }}
                disabled={invitationsLoading}
              />
            )}
          </GlassCard>
        )}

        {activeTab === 'activity' && (
          <GlassCard className="overflow-hidden p-0">
            <div className="flex items-center justify-between border-b border-[var(--crm-border)] px-5 py-4">
              <div>
                <h2 className="font-semibold text-[var(--crm-text)]">Organization activity</h2>
                <p className="mt-1 text-sm text-[var(--crm-text-muted)]">Recent settings, membership, and invitation changes.</p>
              </div>
              <NavLink to="/audit-logs" className="text-sm font-semibold text-[var(--crm-primary)] hover:underline">View all</NavLink>
            </div>
            {activityLoading && <LoadingState message="Loading organization activity..." />}
            {!activityLoading && activityError && (
              <div className="p-5"><ErrorState message={activityError} onRetry={retryActivity} /></div>
            )}
            {!activityLoading && !activityError && activity.length === 0 && (
              <EmptyState icon={History} title="No organization activity yet" message="Settings and access changes will appear here." />
            )}
            {!activityLoading && !activityError && activity.length > 0 && (
              <div className="divide-y divide-[var(--crm-border)]">
                {activity.map((item) => (
                  <div key={item.id} className="flex gap-4 px-5 py-4">
                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-[var(--crm-soft-gradient)] text-[var(--crm-primary)]">
                      <ShieldCheck size={17} />
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                        <p className="font-semibold text-[var(--crm-text)]">{formatAuditAction(item.action)}</p>
                        <span className="inline-flex items-center gap-1 text-xs text-[var(--crm-text-muted)]"><Clock3 size={13} /> {formatDateTime(item.createdAt)}</span>
                      </div>
                      <p className="mt-1 text-sm text-[var(--crm-text-muted)]">{formatAuditDetails(item)}</p>
                      <p className="mt-1 text-xs text-[var(--crm-text-muted)]">by {item.actorUserName ?? 'System'}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </GlassCard>
        )}
      </PageShell>

      <Modal
        open={inviteOpen}
        title="Invite member"
        description="Send secure access to this workspace."
        onClose={() => {
          if (!inviteSaving) setInviteOpen(false)
        }}
      >
        <form onSubmit={sendInvitation} className="space-y-4">
          {inviteError && <ErrorState message={inviteError} />}
          <TextField
            label="Email"
            type="email"
            value={inviteEmail}
            onChange={(event) => setInviteEmail(event.target.value)}
            required
            autoFocus
            helperText="The invitation expires after 72 hours."
          />
          <SelectField
            label="Role"
            value={inviteRole}
            onChange={(event) => setInviteRole(event.target.value as Exclude<RoleName, 'OWNER'>)}
            required
            helperText="Choose the access this person needs."
          >
            {assignableRoles.map((role) => <option key={role} value={role}>{formatRole(role)}</option>)}
          </SelectField>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setInviteOpen(false)} disabled={inviteSaving} className="h-11 rounded-2xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text)] transition hover:bg-violet-500/10 disabled:opacity-50">Cancel</button>
            <button type="submit" disabled={inviteSaving} className="crm-primary-action inline-flex h-11 items-center gap-2 rounded-2xl px-4 text-sm font-semibold disabled:opacity-50"><Send size={16} /> {inviteSaving ? 'Sending...' : 'Send invitation'}</button>
          </div>
        </form>
      </Modal>

      <Modal
        open={editingMember !== null}
        title="Change member role"
        description={editingMember ? `Update access for ${editingMember.userFullName}.` : undefined}
        onClose={() => {
          if (!memberSaving) setEditingMember(null)
        }}
      >
        <form onSubmit={saveMemberRole} className="space-y-4">
          <SelectField
            label="Role"
            value={memberRole}
            onChange={(event) => setMemberRole(event.target.value as Exclude<RoleName, 'OWNER'>)}
            required
          >
            {assignableRoles.map((role) => <option key={role} value={role}>{formatRole(role)}</option>)}
          </SelectField>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setEditingMember(null)} disabled={memberSaving} className="h-11 rounded-2xl border border-[var(--crm-border)] px-4 text-sm font-semibold text-[var(--crm-text)] transition hover:bg-violet-500/10 disabled:opacity-50">Cancel</button>
            <button type="submit" disabled={memberSaving || editingMember?.role === memberRole} className="crm-primary-action inline-flex h-11 items-center gap-2 rounded-2xl px-4 text-sm font-semibold disabled:opacity-50"><ShieldCheck size={16} /> {memberSaving ? 'Saving...' : 'Save role'}</button>
          </div>
        </form>
      </Modal>
    </AppLayout>
  )
}
