import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  Archive,
  Bell,
  CirclePause,
  CirclePlay,
  ListRestart,
  LockKeyhole,
  Plus,
  RefreshCw,
  RotateCcw,
  Workflow as WorkflowIcon,
  XCircle,
} from 'lucide-react'
import { isAxiosError } from 'axios'
import toast from 'react-hot-toast'
import { AppLayout } from '../layouts/AppLayout'
import { EmptyState, ErrorState, LoadingState, Modal, PageShell } from '../components/ui'
import { getLoadErrorMessage, getSaveErrorMessage } from '../lib/errors'
import { useWorkspace } from '../workspace/useWorkspace'
import {
  activateWorkflow,
  archiveWorkflow,
  cancelWorkflowExecution,
  createWorkflow,
  getWorkflowExecutions,
  getWorkflows,
  pauseWorkflow,
  retryWorkflowExecution,
  type CreateWorkflowRequest,
  type Workflow,
  type WorkflowActionType,
  type WorkflowExecution,
} from '../services/workflowService'

const EVENT_OPTIONS = [
  ['customer.created', 'Customer created'],
  ['customer.updated', 'Customer updated'],
  ['lead.created', 'Lead created'],
  ['lead.updated', 'Lead updated'],
  ['contact.created', 'Contact created'],
  ['contact.updated', 'Contact updated'],
  ['task.updated', 'Task updated'],
  ['task.completed', 'Task completed'],
  ['note.created', 'Note created'],
] as const

type ViewMode = 'definitions' | 'history'

function statusClass(status: string) {
  if (status === 'ACTIVE' || status === 'SUCCEEDED') {
    return 'bg-emerald-500/10 text-emerald-700 ring-emerald-500/20'
  }
  if (status === 'FAILED' || status === 'DEAD' || status === 'CANCELLED') {
    return 'bg-rose-500/10 text-rose-700 ring-rose-500/20'
  }
  if (status === 'PROCESSING' || status === 'RETRY_SCHEDULED') {
    return 'bg-amber-500/10 text-amber-700 ring-amber-500/20'
  }
  return 'bg-slate-500/10 text-[var(--crm-text-muted)] ring-slate-500/20'
}

function formatLabel(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replaceAll('.', ' ')
}

function formatDate(value: string | null) {
  if (!value) return 'Not yet'
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function payloadRoot(eventType: string) {
  return eventType.split('.')[0]
}

export function WorkflowsPage() {
  const { activeWorkspace } = useWorkspace()
  const canManage = activeWorkspace?.permissions.includes('WORKFLOW_MANAGE') ?? false
  const [view, setView] = useState<ViewMode>('definitions')
  const [workflows, setWorkflows] = useState<Workflow[]>([])
  const [executions, setExecutions] = useState<WorkflowExecution[]>([])
  const [selectedExecutionId, setSelectedExecutionId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [featureUnavailable, setFeatureUnavailable] = useState(false)
  const [busyId, setBusyId] = useState<string | number | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [eventType, setEventType] = useState('customer.created')
  const [actionType, setActionType] = useState<WorkflowActionType>('CREATE_TASK')

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    setFeatureUnavailable(false)
    try {
      const [workflowPage, executionPage] = await Promise.all([
        getWorkflows(),
        getWorkflowExecutions(),
      ])
      setWorkflows(workflowPage.content)
      setExecutions(executionPage.content)
    } catch (error) {
      if (
        isAxiosError<{ code?: string }>(error) &&
        error.response?.data.code === 'SUBSCRIPTION_FEATURE_REQUIRED'
      ) {
        setFeatureUnavailable(true)
      } else {
        setLoadError(getLoadErrorMessage('workflows'))
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => void load(), 0)
    return () => window.clearTimeout(timeoutId)
  }, [activeWorkspace?.organizationId, load])

  const selectedExecution = useMemo(
    () => executions.find((item) => item.publicExecutionId === selectedExecutionId),
    [executions, selectedExecutionId],
  )

  async function submitCreate(event: FormEvent) {
    event.preventDefault()
    const root = payloadRoot(eventType)
    const configuration =
      actionType === 'CREATE_TASK'
        ? {
            titleTemplate: `Follow up {{${root}.id}}`,
            priority: 'MEDIUM',
            dueDateOffsetDays: 1,
            assigneePolicy: 'WORKFLOW_CREATOR',
            relationPolicy: 'TRIGGER_RECORD',
          }
        : {
            titleTemplate: 'CRM workflow update',
            messageTemplate: `${formatLabel(eventType)} requires attention.`,
            recipientPolicy: 'WORKFLOW_CREATOR',
          }
    const request: CreateWorkflowRequest = {
      name: name.trim(),
      description: description.trim() || null,
      failurePolicy: 'STOP_ON_FAILURE',
      executionTimeoutSeconds: 60,
      maximumAttempts: 3,
      maximumExecutionsPerHour: 100,
      trigger: {
        eventType,
        conditionConfig: null,
        enabled: true,
      },
      actions: [
        {
          name: actionType === 'CREATE_TASK' ? 'Create follow-up task' : 'Notify workflow owner',
          actionType,
          configuration,
          enabled: true,
          timeoutSeconds: 15,
        },
      ],
    }

    setBusyId('create')
    try {
      const created = await createWorkflow(request)
      setWorkflows((current) => [created, ...current])
      setCreateOpen(false)
      setName('')
      setDescription('')
      toast.success('Workflow draft created')
    } catch {
      toast.error(getSaveErrorMessage('workflow'))
    } finally {
      setBusyId(null)
    }
  }

  async function lifecycle(workflow: Workflow, action: 'activate' | 'pause' | 'archive') {
    setBusyId(workflow.id)
    try {
      const updated = await (action === 'activate'
        ? activateWorkflow(workflow.id)
        : action === 'pause'
          ? pauseWorkflow(workflow.id)
          : archiveWorkflow(workflow.id))
      setWorkflows((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      )
      toast.success(`Workflow ${action}d`)
    } catch {
      toast.error(getSaveErrorMessage('workflow'))
    } finally {
      setBusyId(null)
    }
  }

  async function executionAction(execution: WorkflowExecution, action: 'retry' | 'cancel') {
    setBusyId(execution.publicExecutionId)
    try {
      const updated = await (action === 'retry'
        ? retryWorkflowExecution(execution.publicExecutionId)
        : cancelWorkflowExecution(execution.publicExecutionId))
      setExecutions((current) =>
        current.map((item) =>
          item.publicExecutionId === updated.publicExecutionId ? updated : item,
        ),
      )
      toast.success(action === 'retry' ? 'Execution queued again' : 'Execution cancelled')
    } catch {
      toast.error(getSaveErrorMessage('workflow execution'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <AppLayout>
      <PageShell
        title="Workflows"
        description="Automate repeatable CRM follow-ups and monitor every run."
        action={
          canManage && !featureUnavailable ? (
            <button
              type="button"
              onClick={() => setCreateOpen(true)}
              className="inline-flex h-10 items-center gap-2 rounded-lg bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white shadow-sm transition hover:opacity-90"
            >
              <Plus size={17} /> New workflow
            </button>
          ) : undefined
        }
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 border-b border-[var(--crm-border)]">
          <div className="flex" role="tablist" aria-label="Workflow views">
            {(['definitions', 'history'] as ViewMode[]).map((item) => (
              <button
                key={item}
                type="button"
                role="tab"
                aria-selected={view === item}
                onClick={() => setView(item)}
                className={`border-b-2 px-4 py-3 text-sm font-semibold capitalize transition ${
                  view === item
                    ? 'border-[var(--crm-primary)] text-[var(--crm-primary)]'
                    : 'border-transparent text-[var(--crm-text-muted)] hover:text-[var(--crm-text)]'
                }`}
              >
                {item}
              </button>
            ))}
          </div>
          <button
            type="button"
            onClick={() => void load()}
              aria-label="Refresh workflows"
            className="grid h-9 w-9 place-items-center rounded-lg border border-[var(--crm-border)] text-[var(--crm-text-muted)] transition hover:text-[var(--crm-primary)]"
          >
            <RefreshCw size={16} />
          </button>
        </div>

        {loading ? (
          <LoadingState message="Loading workflows" />
        ) : featureUnavailable ? (
          <EmptyState
            icon={LockKeyhole}
            title="Workflow automation is not included in this plan"
            message="Upgrade to Professional or higher to create and run workflows."
          />
        ) : loadError ? (
          <ErrorState message={loadError} onRetry={() => void load()} />
        ) : view === 'definitions' ? (
          workflows.length === 0 ? (
            <EmptyState
              icon={WorkflowIcon}
              title="No workflows yet"
              message="Create a draft, review it, then activate it when ready."
            />
          ) : (
            <div className="overflow-hidden rounded-lg border border-[var(--crm-border)] bg-[var(--crm-surface)]">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left text-sm">
                  <thead className="bg-[var(--crm-surface-soft)] text-xs uppercase text-[var(--crm-text-muted)]">
                    <tr>
                      <th className="px-4 py-3">Workflow</th>
                      <th className="px-4 py-3">Trigger</th>
                      <th className="px-4 py-3">Action</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3 text-right">Controls</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[var(--crm-border)]">
                    {workflows.map((workflow) => (
                      <tr key={workflow.publicWorkflowId}>
                        <td className="px-4 py-4">
                          <p className="font-semibold text-[var(--crm-text)]">{workflow.name}</p>
                          <p className="mt-1 max-w-xs truncate text-xs text-[var(--crm-text-muted)]">
                            {workflow.description || `Version ${workflow.definitionVersion}`}
                          </p>
                        </td>
                        <td className="px-4 py-4 capitalize text-[var(--crm-text-muted)]">
                          {formatLabel(workflow.trigger.eventType)}
                        </td>
                        <td className="px-4 py-4 text-[var(--crm-text-muted)]">
                          {workflow.actions[0]?.name ?? 'No enabled action'}
                        </td>
                        <td className="px-4 py-4">
                          <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold capitalize ring-1 ${statusClass(workflow.status)}`}>
                            {formatLabel(workflow.status)}
                          </span>
                        </td>
                        <td className="px-4 py-4">
                          {canManage && (
                            <div className="flex justify-end gap-1">
                              {(workflow.status === 'DRAFT' || workflow.status === 'PAUSED') && (
                                <button
                                  type="button"
                                  onClick={() => void lifecycle(workflow, 'activate')}
                                  disabled={busyId === workflow.id}
                                  className="grid h-9 w-9 place-items-center rounded-lg text-emerald-700 transition hover:bg-emerald-500/10 disabled:opacity-50"
                                  title="Activate workflow"
                                  aria-label="Activate workflow"
                                >
                                  <CirclePlay size={17} />
                                </button>
                              )}
                              {workflow.status === 'ACTIVE' && (
                                <button
                                  type="button"
                                  onClick={() => void lifecycle(workflow, 'pause')}
                                  disabled={busyId === workflow.id}
                                  className="grid h-9 w-9 place-items-center rounded-lg text-amber-700 transition hover:bg-amber-500/10 disabled:opacity-50"
                                  title="Pause workflow"
                                  aria-label="Pause workflow"
                                >
                                  <CirclePause size={17} />
                                </button>
                              )}
                              {workflow.status !== 'ACTIVE' && workflow.status !== 'ARCHIVED' && (
                                <button
                                  type="button"
                                  onClick={() => void lifecycle(workflow, 'archive')}
                                  disabled={busyId === workflow.id}
                                  className="grid h-9 w-9 place-items-center rounded-lg text-[var(--crm-text-muted)] transition hover:bg-rose-500/10 hover:text-rose-700 disabled:opacity-50"
                                  title="Archive workflow"
                                  aria-label="Archive workflow"
                                >
                                  <Archive size={17} />
                                </button>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )
        ) : executions.length === 0 ? (
          <EmptyState
            icon={ListRestart}
            title="No executions yet"
            message="Runs appear here after an active workflow receives a matching CRM event."
          />
        ) : (
          <div className="space-y-3">
            {executions.map((execution) => {
              const expanded = selectedExecutionId === execution.publicExecutionId
              const retryable = ['FAILED', 'DEAD', 'PARTIALLY_SUCCEEDED'].includes(execution.status)
              const cancellable = ['PENDING', 'RETRY_SCHEDULED'].includes(execution.status)
              return (
                <section
                  key={execution.publicExecutionId}
                  className="overflow-hidden rounded-lg border border-[var(--crm-border)] bg-[var(--crm-surface)]"
                >
                  <button
                    type="button"
                    onClick={() => setSelectedExecutionId(expanded ? null : execution.publicExecutionId)}
                    className="grid w-full gap-3 p-4 text-left sm:grid-cols-[1fr_auto_auto] sm:items-center"
                    aria-expanded={expanded}
                  >
                    <span>
                      <span className="block font-semibold">{execution.workflowName}</span>
                      <span className="mt-1 block text-xs capitalize text-[var(--crm-text-muted)]">
                        {formatLabel(execution.triggerEventType)} · {formatDate(execution.createdAt)}
                      </span>
                    </span>
                    <span className={`w-fit rounded-full px-2.5 py-1 text-xs font-semibold capitalize ring-1 ${statusClass(execution.status)}`}>
                      {formatLabel(execution.status)}
                    </span>
                    <span className="text-xs text-[var(--crm-text-muted)]">
                      {execution.actions.length} action{execution.actions.length === 1 ? '' : 's'}
                    </span>
                  </button>
                  {expanded && selectedExecution && (
                    <div className="border-t border-[var(--crm-border)] bg-[var(--crm-surface-soft)] p-4">
                      <div className="space-y-2">
                        {selectedExecution.actions.map((action) => (
                          <div key={action.publicActionExecutionId} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 py-3">
                            <div>
                              <p className="text-sm font-semibold">{action.actionOrder}. {action.name}</p>
                              <p className="mt-1 text-xs text-[var(--crm-text-muted)]">
                                {formatLabel(action.actionType)} · {action.attemptCount} attempt{action.attemptCount === 1 ? '' : 's'}
                              </p>
                              {action.lastError && <p className="mt-1 text-xs text-rose-700">{action.lastError}</p>}
                            </div>
                            <span className={`rounded-full px-2.5 py-1 text-xs font-semibold capitalize ring-1 ${statusClass(action.status)}`}>
                              {formatLabel(action.status)}
                            </span>
                          </div>
                        ))}
                      </div>
                      {execution.lastError && <p className="mt-3 text-sm text-rose-700">{execution.lastError}</p>}
                      {canManage && (retryable || cancellable) && (
                        <div className="mt-4 flex justify-end gap-2">
                          {retryable && (
                            <button
                              type="button"
                              onClick={() => void executionAction(execution, 'retry')}
                              disabled={busyId === execution.publicExecutionId}
                              className="inline-flex h-9 items-center gap-2 rounded-lg border border-[var(--crm-border)] px-3 text-sm font-semibold transition hover:text-[var(--crm-primary)] disabled:opacity-50"
                            >
                              <RotateCcw size={15} /> Retry
                            </button>
                          )}
                          {cancellable && (
                            <button
                              type="button"
                              onClick={() => void executionAction(execution, 'cancel')}
                              disabled={busyId === execution.publicExecutionId}
                              className="inline-flex h-9 items-center gap-2 rounded-lg border border-rose-300 px-3 text-sm font-semibold text-rose-700 transition hover:bg-rose-500/10 disabled:opacity-50"
                            >
                              <XCircle size={15} /> Cancel
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </section>
              )
            })}
          </div>
        )}
      </PageShell>

      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="New workflow"
        description="Start with one reliable trigger and action."
      >
        <form onSubmit={submitCreate} className="space-y-4">
          <label className="block text-sm font-semibold">
            Name
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
              maxLength={100}
              className="mt-2 h-11 w-full rounded-lg border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 font-normal outline-none transition focus:border-[var(--crm-primary)]"
              placeholder="New customer follow-up"
            />
          </label>
          <label className="block text-sm font-semibold">
            Description
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              maxLength={500}
              rows={3}
              className="mt-2 w-full resize-none rounded-lg border border-[var(--crm-border)] bg-[var(--crm-surface)] p-3 font-normal outline-none transition focus:border-[var(--crm-primary)]"
            />
          </label>
          <label className="block text-sm font-semibold">
            When
            <select
              value={eventType}
              onChange={(event) => setEventType(event.target.value)}
              className="mt-2 h-11 w-full rounded-lg border border-[var(--crm-border)] bg-[var(--crm-surface)] px-3 font-normal outline-none focus:border-[var(--crm-primary)]"
            >
              {EVENT_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <fieldset>
            <legend className="text-sm font-semibold">Then</legend>
            <div className="mt-2 grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setActionType('CREATE_TASK')}
                className={`flex min-h-20 flex-col items-center justify-center gap-2 rounded-lg border text-sm font-semibold transition ${actionType === 'CREATE_TASK' ? 'border-[var(--crm-primary)] bg-violet-500/10 text-[var(--crm-primary)]' : 'border-[var(--crm-border)]'}`}
                aria-pressed={actionType === 'CREATE_TASK'}
              >
                <ListRestart size={20} /> Create task
              </button>
              <button
                type="button"
                onClick={() => setActionType('SEND_IN_APP_NOTIFICATION')}
                className={`flex min-h-20 flex-col items-center justify-center gap-2 rounded-lg border text-sm font-semibold transition ${actionType === 'SEND_IN_APP_NOTIFICATION' ? 'border-[var(--crm-primary)] bg-violet-500/10 text-[var(--crm-primary)]' : 'border-[var(--crm-border)]'}`}
                aria-pressed={actionType === 'SEND_IN_APP_NOTIFICATION'}
              >
                <Bell size={20} /> Send notification
              </button>
            </div>
          </fieldset>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={() => setCreateOpen(false)} className="h-10 rounded-lg border border-[var(--crm-border)] px-4 text-sm font-semibold">Cancel</button>
            <button type="submit" disabled={busyId === 'create'} className="h-10 rounded-lg bg-[var(--crm-primary)] px-4 text-sm font-semibold text-white disabled:opacity-50">
              {busyId === 'create' ? 'Creating...' : 'Create draft'}
            </button>
          </div>
        </form>
      </Modal>
    </AppLayout>
  )
}
