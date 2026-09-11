import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  BadgeCheck,
  CircleOff,
  Clock3,
  History,
  Mail,
  MessageCircle,
  Pencil,
  PlugZap,
  Plus,
  RefreshCw,
  RotateCcw,
  Send,
  Trash2,
  XCircle,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { AppLayout } from '../layouts/AppLayout'
import {
  EmptyState,
  ErrorState,
  LoadingState,
  Modal,
  PageShell,
  PaginationBar,
  TextAreaField,
  TextField,
} from '../components/ui'
import { useWorkspace } from '../workspace/useWorkspace'
import {
  cancelIntegrationDelivery,
  createIntegrationConnection,
  disableIntegrationConnection,
  getIntegrationConnections,
  getIntegrationDeliveries,
  getIntegrationDeliveryAttempts,
  queueIntegrationDelivery,
  retryIntegrationDelivery,
  revokeIntegrationConnection,
  updateIntegrationConnection,
  verifyIntegrationConnection,
  type IntegrationConnection,
  type IntegrationDelivery,
  type IntegrationDeliveryAttempt,
  type IntegrationProvider,
} from '../services/integrationService'

type ConnectionForm = {
  provider: IntegrationProvider
  name: string
  phoneNumberId: string
  apiVersion: string
  accessToken: string
  host: string
  port: string
  fromAddress: string
  fromName: string
  replyTo: string
  smtpAuth: boolean
  startTls: boolean
  username: string
  password: string
}

const emptyForm: ConnectionForm = {
  provider: 'WHATSAPP_CLOUD',
  name: '',
  phoneNumberId: '',
  apiVersion: 'v23.0',
  accessToken: '',
  host: 'mailpit',
  port: '1025',
  fromAddress: 'no-reply@crm.local',
  fromName: 'Tadamun',
  replyTo: '',
  smtpAuth: false,
  startTls: false,
  username: '',
  password: '',
}

function apiMessage(error: unknown, fallback: string) {
  const value = error as {
    response?: { data?: { message?: string } }
    message?: string
  }
  return value.response?.data?.message ?? value.message ?? fallback
}

function label(value: string) {
  return value.toLowerCase().replaceAll('_', ' ')
}

function dateTime(value: string | null) {
  if (!value) return 'Not yet'
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function statusClass(status: string) {
  if (status === 'ACTIVE' || status === 'SUCCEEDED') {
    return 'bg-emerald-500/10 text-emerald-700 ring-emerald-500/20'
  }
  if (['ERROR', 'REVOKED', 'TERMINAL_FAILURE', 'DEAD'].includes(status)) {
    return 'bg-rose-500/10 text-rose-700 ring-rose-500/20'
  }
  if (['PROCESSING', 'RETRY_SCHEDULED'].includes(status)) {
    return 'bg-amber-500/10 text-amber-700 ring-amber-500/20'
  }
  return 'bg-slate-500/10 text-[var(--crm-text-muted)] ring-slate-500/20'
}

function configText(connection: IntegrationConnection, key: string) {
  const value = connection.configuration[key]
  return typeof value === 'string' ? value : ''
}

function configBoolean(connection: IntegrationConnection, key: string) {
  return connection.configuration[key] === true
}

function formFromConnection(connection: IntegrationConnection): ConnectionForm {
  return {
    ...emptyForm,
    provider: connection.provider,
    name: connection.name,
    phoneNumberId: configText(connection, 'phoneNumberId'),
    apiVersion: configText(connection, 'apiVersion') || 'v23.0',
    host: configText(connection, 'host'),
    port: String(connection.configuration.port ?? '587'),
    fromAddress: configText(connection, 'fromAddress'),
    fromName: configText(connection, 'fromName'),
    replyTo: configText(connection, 'replyTo'),
    smtpAuth: configBoolean(connection, 'smtpAuth'),
    startTls: configBoolean(connection, 'startTls'),
  }
}

export function IntegrationsPage() {
  const { activeWorkspace } = useWorkspace()
  const canManage = activeWorkspace?.permissions.includes('INTEGRATION_MANAGE') ?? false
  const [connections, setConnections] = useState<IntegrationConnection[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [busy, setBusy] = useState<string | number | null>(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [editing, setEditing] = useState<IntegrationConnection | null>(null)
  const [form, setForm] = useState<ConnectionForm>(emptyForm)
  const [formError, setFormError] = useState('')
  const [sendOpen, setSendOpen] = useState(false)
  const [destination, setDestination] = useState('')
  const [subject, setSubject] = useState('Tadamun integration test')
  const [messageBody, setMessageBody] = useState('This is a test message from Tadamun CRM.')
  const [deliveries, setDeliveries] = useState<IntegrationDelivery[]>([])
  const [deliveryPage, setDeliveryPage] = useState(0)
  const [deliveryTotalPages, setDeliveryTotalPages] = useState(0)
  const [deliveryTotal, setDeliveryTotal] = useState(0)
  const [deliveriesLoading, setDeliveriesLoading] = useState(false)
  const [deliveryError, setDeliveryError] = useState('')
  const [detailDelivery, setDetailDelivery] = useState<IntegrationDelivery | null>(null)
  const [attempts, setAttempts] = useState<IntegrationDeliveryAttempt[]>([])
  const [attemptsLoading, setAttemptsLoading] = useState(false)

  const selectedConnection = useMemo(
    () => connections.find((connection) => connection.id === selectedId) ?? null,
    [connections, selectedId],
  )

  const loadConnections = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      const page = await getIntegrationConnections()
      setConnections(page.content)
      if (page.content.length === 0) setDeliveries([])
      setSelectedId((current) =>
        page.content.some((item) => item.id === current)
          ? current
          : page.content[0]?.id ?? null,
      )
    } catch (error) {
      setLoadError(apiMessage(error, 'Could not load integrations.'))
    } finally {
      setLoading(false)
    }
  }, [])

  const loadDeliveries = useCallback(async (connectionId: number, page: number) => {
    setDeliveriesLoading(true)
    setDeliveryError('')
    try {
      const result = await getIntegrationDeliveries(connectionId, page, 10)
      setDeliveries(result.content)
      setDeliveryTotalPages(result.totalPages)
      setDeliveryTotal(result.totalElements)
    } catch (error) {
      setDeliveryError(apiMessage(error, 'Could not load delivery history.'))
    } finally {
      setDeliveriesLoading(false)
    }
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => void loadConnections(), 0)
    return () => window.clearTimeout(timeoutId)
  }, [activeWorkspace?.organizationId, loadConnections])

  useEffect(() => {
    if (selectedId === null) return
    const timeoutId = window.setTimeout(
      () => void loadDeliveries(selectedId, deliveryPage),
      0,
    )
    return () => window.clearTimeout(timeoutId)
  }, [deliveryPage, loadDeliveries, selectedId])

  function openCreate() {
    setEditing(null)
    setForm({ ...emptyForm })
    setFormError('')
    setEditorOpen(true)
  }

  function openEdit(connection: IntegrationConnection) {
    setEditing(connection)
    setForm(formFromConnection(connection))
    setFormError('')
    setEditorOpen(true)
  }

  function updateForm<K extends keyof ConnectionForm>(key: K, value: ConnectionForm[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function saveConnection(event: FormEvent) {
    event.preventDefault()
    const configuration = form.provider === 'WHATSAPP_CLOUD'
      ? {
          phoneNumberId: form.phoneNumberId.trim(),
          apiVersion: form.apiVersion.trim(),
        }
      : {
          host: form.host.trim(),
          port: Number(form.port),
          fromAddress: form.fromAddress.trim(),
          fromName: form.fromName.trim(),
          replyTo: form.replyTo.trim(),
          smtpAuth: form.smtpAuth,
          startTls: form.startTls,
        }
    const enteredCredentials: Record<string, string> | undefined =
      form.provider === 'WHATSAPP_CLOUD'
      ? (form.accessToken.trim() ? { accessToken: form.accessToken.trim() } : undefined)
      : (form.username.trim() || form.password
          ? { username: form.username.trim(), password: form.password }
          : undefined)

    if (!editing && !enteredCredentials && form.provider === 'WHATSAPP_CLOUD') {
      setFormError('WhatsApp access token is required.')
      return
    }
    if (!editing && form.smtpAuth && !enteredCredentials) {
      setFormError('SMTP username and password are required when authentication is enabled.')
      return
    }

    setBusy('save')
    setFormError('')
    try {
      const saved = editing
        ? await updateIntegrationConnection(editing.id, {
            name: form.name.trim(),
            configuration,
            ...(enteredCredentials ? { credentials: enteredCredentials } : {}),
          })
        : await createIntegrationConnection({
            name: form.name.trim(),
            provider: form.provider,
            configuration,
            credentials: enteredCredentials ?? {},
          })
      setConnections((current) => editing
        ? current.map((item) => (item.id === saved.id ? saved : item))
        : [saved, ...current])
      setSelectedId(saved.id)
      setEditorOpen(false)
      toast.success(editing ? 'Integration updated' : 'Integration created')
    } catch (error) {
      setFormError(apiMessage(error, 'Could not save integration.'))
    } finally {
      setBusy(null)
    }
  }

  async function connectionAction(
    connection: IntegrationConnection,
    action: 'verify' | 'disable' | 'revoke',
  ) {
    if (action === 'revoke' && !window.confirm(
      `Revoke ${connection.name}? Stored credentials will no longer be usable.`,
    )) return
    setBusy(`${action}-${connection.id}`)
    try {
      const updated = action === 'verify'
        ? await verifyIntegrationConnection(connection.id)
        : action === 'disable'
          ? await disableIntegrationConnection(connection.id)
          : await revokeIntegrationConnection(connection.id)
      setConnections((current) => current.map((item) =>
        item.id === updated.id ? updated : item,
      ))
      toast.success(action === 'verify'
        ? updated.status === 'ACTIVE' ? 'Connection verified' : 'Verification failed'
        : `Integration ${action}d`)
    } catch (error) {
      toast.error(apiMessage(error, `Could not ${action} integration.`))
    } finally {
      setBusy(null)
    }
  }

  function openSend(connection: IntegrationConnection) {
    setSelectedId(connection.id)
    setDestination('')
    setSubject('Tadamun integration test')
    setMessageBody('This is a test message from Tadamun CRM.')
    setSendOpen(true)
  }

  async function sendTest(event: FormEvent) {
    event.preventDefault()
    if (!selectedConnection) return
    setBusy('send')
    try {
      const queued = await queueIntegrationDelivery(selectedConnection.id, {
        type: selectedConnection.provider === 'SMTP' ? 'EMAIL' : 'WHATSAPP_TEXT',
        destination: destination.trim(),
        subject: selectedConnection.provider === 'SMTP' ? subject.trim() : null,
        body: messageBody,
      })
      setDeliveries((current) => [queued, ...current].slice(0, 10))
      setDeliveryTotal((current) => current + 1)
      setSendOpen(false)
      toast.success('Test delivery queued')
    } catch (error) {
      toast.error(apiMessage(error, 'Could not queue test delivery.'))
    } finally {
      setBusy(null)
    }
  }

  async function openDeliveryDetails(delivery: IntegrationDelivery) {
    setDetailDelivery(delivery)
    setAttempts([])
    setAttemptsLoading(true)
    try {
      setAttempts(await getIntegrationDeliveryAttempts(delivery.publicDeliveryId))
    } catch (error) {
      toast.error(apiMessage(error, 'Could not load delivery attempts.'))
    } finally {
      setAttemptsLoading(false)
    }
  }

  async function deliveryAction(delivery: IntegrationDelivery, action: 'retry' | 'cancel') {
    setBusy(`${action}-${delivery.publicDeliveryId}`)
    try {
      const updated = action === 'retry'
        ? await retryIntegrationDelivery(delivery.publicDeliveryId)
        : await cancelIntegrationDelivery(delivery.publicDeliveryId)
      setDeliveries((current) => current.map((item) =>
        item.publicDeliveryId === updated.publicDeliveryId ? updated : item,
      ))
      toast.success(action === 'retry' ? 'Delivery queued again' : 'Delivery cancelled')
    } catch (error) {
      toast.error(apiMessage(error, `Could not ${action} delivery.`))
    } finally {
      setBusy(null)
    }
  }

  return (
    <AppLayout>
      <PageShell
        title="Integrations"
        description="Connect external messaging and email providers, verify credentials, and monitor delivery results."
        action={canManage ? (
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-sm transition hover:opacity-90"
          >
            <Plus size={17} /> New connection
          </button>
        ) : undefined}
      >
        <div className="flex justify-end">
          <button
            type="button"
            onClick={() => void loadConnections()}
            className="grid h-9 w-9 place-items-center rounded-lg border border-[var(--crm-border)] text-[var(--crm-text-muted)] transition hover:text-[var(--crm-primary)]"
            aria-label="Refresh integrations"
            title="Refresh integrations"
          >
            <RefreshCw size={16} />
          </button>
        </div>

        {loading ? (
          <LoadingState message="Loading integrations" />
        ) : loadError ? (
          <ErrorState message={loadError} onRetry={() => void loadConnections()} />
        ) : connections.length === 0 ? (
          <EmptyState
            icon={PlugZap}
            title="No integrations configured"
            message="Add a WhatsApp Cloud or SMTP connection to begin sending from CRM workflows."
          />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {connections.map((connection) => {
              const ProviderIcon = connection.provider === 'SMTP' ? Mail : MessageCircle
              const selected = connection.id === selectedId
              return (
                <article
                  key={connection.publicConnectionId}
                  className={`rounded-lg border bg-[var(--crm-surface)] p-5 transition ${selected
                    ? 'border-[var(--crm-primary)] ring-2 ring-violet-500/10'
                    : 'border-[var(--crm-border)]'}`}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedId(connection.id)
                      setDeliveryPage(0)
                    }}
                    className="flex w-full items-start gap-3 text-left"
                    aria-label={`Show delivery history for ${connection.name}`}
                  >
                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-violet-500/10 text-[var(--crm-primary)]">
                      <ProviderIcon size={19} />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-semibold text-[var(--crm-text)]">
                        {connection.name}
                      </span>
                      <span className="mt-1 block text-xs text-[var(--crm-text-muted)]">
                        {connection.provider === 'SMTP'
                          ? `${configText(connection, 'host')}:${String(connection.configuration.port ?? '')}`
                          : `Phone ID ${configText(connection, 'phoneNumberId')}`}
                      </span>
                    </span>
                    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold capitalize ring-1 ${statusClass(connection.status)}`}>
                      {label(connection.status)}
                    </span>
                  </button>

                  <div className="mt-4 grid grid-cols-2 gap-3 border-t border-[var(--crm-border)] pt-4 text-xs text-[var(--crm-text-muted)]">
                    <span>Verified<br /><strong className="font-medium text-[var(--crm-text)]">{dateTime(connection.lastVerifiedAt)}</strong></span>
                    <span>Credentials<br /><strong className="font-medium text-[var(--crm-text)]">{connection.credentialsConfigured ? 'Configured' : 'Missing'}</strong></span>
                  </div>

                  {connection.lastErrorMessage && (
                    <p className="mt-3 rounded-lg bg-rose-500/10 px-3 py-2 text-xs text-rose-700">
                      {connection.lastErrorMessage}
                    </p>
                  )}

                  {canManage && connection.status !== 'REVOKED' && (
                    <div className="mt-4 flex flex-wrap justify-end gap-1">
                      <button type="button" onClick={() => openEdit(connection)} className="grid h-9 w-9 place-items-center rounded-lg text-[var(--crm-text-muted)] hover:bg-violet-500/10 hover:text-[var(--crm-primary)]" aria-label={`Edit ${connection.name}`} title="Edit connection"><Pencil size={16} /></button>
                      <button type="button" onClick={() => void connectionAction(connection, 'verify')} disabled={busy === `verify-${connection.id}`} className="grid h-9 w-9 place-items-center rounded-lg text-emerald-700 hover:bg-emerald-500/10 disabled:opacity-50" aria-label={`Verify ${connection.name}`} title="Verify connection"><BadgeCheck size={17} /></button>
                      {connection.status === 'ACTIVE' && <button type="button" onClick={() => openSend(connection)} className="grid h-9 w-9 place-items-center rounded-lg text-[var(--crm-primary)] hover:bg-violet-500/10" aria-label={`Send test through ${connection.name}`} title="Send test"><Send size={16} /></button>}
                      {connection.status !== 'DISABLED' && <button type="button" onClick={() => void connectionAction(connection, 'disable')} disabled={busy === `disable-${connection.id}`} className="grid h-9 w-9 place-items-center rounded-lg text-amber-700 hover:bg-amber-500/10 disabled:opacity-50" aria-label={`Disable ${connection.name}`} title="Disable connection"><CircleOff size={17} /></button>}
                      <button type="button" onClick={() => void connectionAction(connection, 'revoke')} disabled={busy === `revoke-${connection.id}`} className="grid h-9 w-9 place-items-center rounded-lg text-rose-700 hover:bg-rose-500/10 disabled:opacity-50" aria-label={`Revoke ${connection.name}`} title="Revoke connection"><Trash2 size={16} /></button>
                    </div>
                  )}
                </article>
              )
            })}
          </div>
        )}

        {selectedConnection && (
          <section className="overflow-hidden rounded-lg border border-[var(--crm-border)] bg-[var(--crm-surface)]">
            <div className="flex flex-col gap-3 border-b border-[var(--crm-border)] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="font-semibold text-[var(--crm-text)]">Delivery history</h2>
                <p className="mt-1 text-xs text-[var(--crm-text-muted)]">{selectedConnection.name}</p>
              </div>
              <button type="button" onClick={() => void loadDeliveries(selectedConnection.id, deliveryPage)} className="grid h-9 w-9 place-items-center rounded-lg border border-[var(--crm-border)] text-[var(--crm-text-muted)] hover:text-[var(--crm-primary)]" aria-label="Refresh delivery history" title="Refresh delivery history"><RefreshCw size={16} /></button>
            </div>

            {deliveriesLoading ? (
              <LoadingState message="Loading delivery history" />
            ) : deliveryError ? (
              <ErrorState message={deliveryError} onRetry={() => void loadDeliveries(selectedConnection.id, deliveryPage)} />
            ) : deliveries.length === 0 ? (
              <EmptyState icon={History} title="No deliveries yet" message="Verified connections can send test messages from this page." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[820px] text-left text-sm">
                  <thead className="bg-[var(--crm-surface-soft)] text-xs uppercase text-[var(--crm-text-muted)]">
                    <tr><th className="px-4 py-3">Destination</th><th className="px-4 py-3">Type</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Attempts</th><th className="px-4 py-3">Created</th><th className="px-4 py-3 text-right">Controls</th></tr>
                  </thead>
                  <tbody className="divide-y divide-[var(--crm-border)]">
                    {deliveries.map((delivery) => (
                      <tr key={delivery.publicDeliveryId}>
                        <td className="px-4 py-4"><p className="font-medium text-[var(--crm-text)]">{delivery.destination}</p><p className="mt-1 max-w-xs truncate text-xs text-[var(--crm-text-muted)]">{delivery.subject || delivery.publicDeliveryId}</p></td>
                        <td className="px-4 py-4 capitalize text-[var(--crm-text-muted)]">{label(delivery.type)}</td>
                        <td className="px-4 py-4"><span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold capitalize ring-1 ${statusClass(delivery.status)}`}>{label(delivery.status)}</span></td>
                        <td className="px-4 py-4 text-[var(--crm-text-muted)]">{delivery.attemptCount} / {delivery.maximumAttempts}</td>
                        <td className="px-4 py-4 text-[var(--crm-text-muted)]">{dateTime(delivery.createdAt)}</td>
                        <td className="px-4 py-4"><div className="flex justify-end gap-1"><button type="button" onClick={() => void openDeliveryDetails(delivery)} className="grid h-9 w-9 place-items-center rounded-lg text-[var(--crm-text-muted)] hover:bg-violet-500/10 hover:text-[var(--crm-primary)]" aria-label="View delivery attempts" title="View attempts"><Clock3 size={16} /></button>{canManage && ['DEAD', 'TERMINAL_FAILURE'].includes(delivery.status) && <button type="button" onClick={() => void deliveryAction(delivery, 'retry')} disabled={busy === `retry-${delivery.publicDeliveryId}`} className="grid h-9 w-9 place-items-center rounded-lg text-amber-700 hover:bg-amber-500/10 disabled:opacity-50" aria-label="Retry delivery" title="Retry delivery"><RotateCcw size={16} /></button>}{canManage && ['PENDING', 'RETRY_SCHEDULED'].includes(delivery.status) && <button type="button" onClick={() => void deliveryAction(delivery, 'cancel')} disabled={busy === `cancel-${delivery.publicDeliveryId}`} className="grid h-9 w-9 place-items-center rounded-lg text-rose-700 hover:bg-rose-500/10 disabled:opacity-50" aria-label="Cancel delivery" title="Cancel delivery"><XCircle size={16} /></button>}</div></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <PaginationBar page={deliveryPage} totalPages={deliveryTotalPages} totalElements={deliveryTotal} pageSize={10} onPrevious={() => setDeliveryPage((page) => Math.max(0, page - 1))} onNext={() => setDeliveryPage((page) => page + 1)} disabled={deliveriesLoading} />
          </section>
        )}

        <Modal open={editorOpen} onClose={() => setEditorOpen(false)} title={editing ? 'Edit integration' : 'New integration'} description={editing ? 'Leave credential fields blank to keep the encrypted values already stored.' : 'Credentials are encrypted before they are written to the database.'}>
          <form onSubmit={(event) => void saveConnection(event)} className="space-y-4">
            {!editing && <div className="grid grid-cols-2 gap-2" role="group" aria-label="Integration provider">{(['WHATSAPP_CLOUD', 'SMTP'] as IntegrationProvider[]).map((provider) => <button key={provider} type="button" onClick={() => updateForm('provider', provider)} className={`h-11 rounded-lg border px-3 text-sm font-semibold transition ${form.provider === provider ? 'border-[var(--crm-primary)] bg-violet-500/10 text-[var(--crm-primary)]' : 'border-[var(--crm-border)] text-[var(--crm-text-muted)]'}`}>{provider === 'SMTP' ? 'Email (SMTP)' : 'WhatsApp Cloud'}</button>)}</div>}
            <TextField label="Connection name" value={form.name} onChange={(event) => updateForm('name', event.target.value)} maxLength={100} required />
            {form.provider === 'WHATSAPP_CLOUD' ? <>
              <div className="grid gap-4 sm:grid-cols-2"><TextField label="Phone number ID" value={form.phoneNumberId} onChange={(event) => updateForm('phoneNumberId', event.target.value)} inputMode="numeric" required /><TextField label="Graph API version" value={form.apiVersion} onChange={(event) => updateForm('apiVersion', event.target.value)} placeholder="v23.0" required /></div>
              <TextField label={editing ? 'New access token' : 'Access token'} type="password" value={form.accessToken} onChange={(event) => updateForm('accessToken', event.target.value)} autoComplete="new-password" required={!editing} />
            </> : <>
              <div className="grid gap-4 sm:grid-cols-[1fr_8rem]"><TextField label="SMTP host" value={form.host} onChange={(event) => updateForm('host', event.target.value)} required /><TextField label="Port" type="number" min={1} max={65535} value={form.port} onChange={(event) => updateForm('port', event.target.value)} required /></div>
              <div className="grid gap-4 sm:grid-cols-2"><TextField label="From address" type="email" value={form.fromAddress} onChange={(event) => updateForm('fromAddress', event.target.value)} required /><TextField label="From name" value={form.fromName} onChange={(event) => updateForm('fromName', event.target.value)} /></div>
              <TextField label="Reply-to address" type="email" value={form.replyTo} onChange={(event) => updateForm('replyTo', event.target.value)} />
              <div className="flex flex-wrap gap-5 text-sm"><label className="flex items-center gap-2"><input type="checkbox" checked={form.smtpAuth} onChange={(event) => updateForm('smtpAuth', event.target.checked)} className="h-4 w-4 accent-[var(--crm-primary)]" /> Authentication</label><label className="flex items-center gap-2"><input type="checkbox" checked={form.startTls} onChange={(event) => updateForm('startTls', event.target.checked)} className="h-4 w-4 accent-[var(--crm-primary)]" /> STARTTLS</label></div>
              {form.smtpAuth && <div className="grid gap-4 sm:grid-cols-2"><TextField label={editing ? 'New username' : 'Username'} value={form.username} onChange={(event) => updateForm('username', event.target.value)} autoComplete="off" required={!editing} /><TextField label={editing ? 'New password' : 'Password'} type="password" value={form.password} onChange={(event) => updateForm('password', event.target.value)} autoComplete="new-password" required={!editing} /></div>}
            </>}
            {formError && <p role="alert" className="rounded-lg bg-rose-500/10 px-3 py-2 text-sm text-rose-700">{formError}</p>}
            <div className="flex justify-end gap-2 border-t border-[var(--crm-border)] pt-4"><button type="button" onClick={() => setEditorOpen(false)} className="h-10 rounded-lg border border-[var(--crm-border)] px-4 text-sm font-semibold">Cancel</button><button type="submit" disabled={busy === 'save'} className="h-10 rounded-lg bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white disabled:opacity-50">{busy === 'save' ? 'Saving...' : 'Save connection'}</button></div>
          </form>
        </Modal>

        <Modal open={sendOpen} onClose={() => setSendOpen(false)} title="Send test message" description={selectedConnection ? `Queue through ${selectedConnection.name}.` : undefined}>
          <form onSubmit={(event) => void sendTest(event)} className="space-y-4">
            <TextField label={selectedConnection?.provider === 'SMTP' ? 'Recipient email' : 'Recipient phone'} type={selectedConnection?.provider === 'SMTP' ? 'email' : 'tel'} value={destination} onChange={(event) => setDestination(event.target.value)} placeholder={selectedConnection?.provider === 'SMTP' ? 'person@example.com' : '+252612345678'} required />
            {selectedConnection?.provider === 'SMTP' && <TextField label="Subject" value={subject} onChange={(event) => setSubject(event.target.value)} maxLength={200} required />}
            <TextAreaField label="Message" value={messageBody} onChange={(event) => setMessageBody(event.target.value)} rows={6} maxLength={selectedConnection?.provider === 'SMTP' ? 50000 : 4096} required />
            <div className="flex justify-end gap-2 border-t border-[var(--crm-border)] pt-4"><button type="button" onClick={() => setSendOpen(false)} className="h-10 rounded-lg border border-[var(--crm-border)] px-4 text-sm font-semibold">Cancel</button><button type="submit" disabled={busy === 'send'} className="inline-flex h-10 items-center gap-2 rounded-lg bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white disabled:opacity-50"><Send size={16} />{busy === 'send' ? 'Queueing...' : 'Queue test'}</button></div>
          </form>
        </Modal>

        <Modal open={detailDelivery !== null} onClose={() => setDetailDelivery(null)} title="Delivery attempts" description={detailDelivery?.publicDeliveryId}>
          {detailDelivery && <div className="mb-4 grid grid-cols-2 gap-3 rounded-lg bg-[var(--crm-surface-soft)] p-4 text-sm"><span className="text-[var(--crm-text-muted)]">Status<br /><strong className="capitalize text-[var(--crm-text)]">{label(detailDelivery.status)}</strong></span><span className="text-[var(--crm-text-muted)]">Destination<br /><strong className="text-[var(--crm-text)]">{detailDelivery.destination}</strong></span></div>}
          {attemptsLoading ? <LoadingState message="Loading attempts" /> : attempts.length === 0 ? <EmptyState icon={Clock3} title="No attempts yet" message="The worker has not claimed this delivery." /> : <ol className="space-y-3">{attempts.map((attempt) => <li key={attempt.attemptNumber} className="rounded-lg border border-[var(--crm-border)] p-4"><div className="flex items-center justify-between gap-3"><strong className="text-sm">Attempt {attempt.attemptNumber}</strong><span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold capitalize ring-1 ${statusClass(attempt.outcome)}`}>{label(attempt.outcome)}</span></div><p className="mt-2 text-xs text-[var(--crm-text-muted)]">{dateTime(attempt.attemptedAt)} · {attempt.durationMs} ms</p>{attempt.errorMessage && <p className="mt-2 text-sm text-rose-700">{attempt.errorMessage}</p>}</li>)}</ol>}
        </Modal>
      </PageShell>
    </AppLayout>
  )
}
