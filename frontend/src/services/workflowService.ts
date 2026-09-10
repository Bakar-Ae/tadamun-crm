import { api } from './api'
import type { PageResponse } from './userService'

export type WorkflowStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED'
export type WorkflowFailurePolicy =
  | 'STOP_ON_FAILURE'
  | 'CONTINUE_ON_FAILURE'
export type WorkflowActionType =
  | 'CREATE_TASK'
  | 'SEND_IN_APP_NOTIFICATION'
export type WorkflowExecutionStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'RETRY_SCHEDULED'
  | 'SUCCEEDED'
  | 'PARTIALLY_SUCCEEDED'
  | 'FAILED'
  | 'DEAD'
  | 'CANCELLED'
export type WorkflowActionExecutionStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'RETRY_SCHEDULED'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'DEAD'
  | 'SKIPPED'

export type WorkflowTrigger = {
  id: number
  triggerType: 'CRM_EVENT'
  eventType: string
  conditionConfig: Record<string, unknown> | null
  enabled: boolean
}

export type WorkflowAction = {
  id: number
  publicActionId: string
  actionOrder: number
  name: string
  actionType: WorkflowActionType
  configuration: Record<string, unknown>
  enabled: boolean
  timeoutSeconds: number
}

export type Workflow = {
  id: number
  publicWorkflowId: string
  name: string
  description: string | null
  status: WorkflowStatus
  failurePolicy: WorkflowFailurePolicy
  executionTimeoutSeconds: number
  maximumAttempts: number
  maximumExecutionsPerHour: number
  definitionVersion: number
  trigger: WorkflowTrigger
  actions: WorkflowAction[]
  createdByUserId: number
  createdByName: string
  activatedAt: string | null
  archivedAt: string | null
  createdAt: string
  updatedAt: string
}

export type WorkflowActionExecution = {
  publicActionExecutionId: string
  actionOrder: number
  name: string
  actionType: WorkflowActionType
  status: WorkflowActionExecutionStatus
  attemptCount: number
  resultResourceType: string | null
  resultResourceId: number | null
  lastErrorCategory: string | null
  lastError: string | null
  startedAt: string | null
  completedAt: string | null
}

export type WorkflowExecution = {
  publicExecutionId: string
  workflowId: number
  workflowName: string
  workflowVersion: number
  triggerEventType: string
  sourceEventPublicId: string
  correlationId: string
  status: WorkflowExecutionStatus
  attemptCount: number
  currentActionOrder: number | null
  lastErrorCategory: string | null
  lastError: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
  actions: WorkflowActionExecution[]
}

export type CreateWorkflowRequest = {
  name: string
  description?: string | null
  failurePolicy: WorkflowFailurePolicy
  executionTimeoutSeconds: number
  maximumAttempts: number
  maximumExecutionsPerHour: number
  trigger: {
    eventType: string
    conditionConfig: Record<string, unknown> | null
    enabled: boolean
  }
  actions: Array<{
    name: string
    actionType: WorkflowActionType
    configuration: Record<string, unknown>
    enabled: boolean
    timeoutSeconds: number
  }>
}

export async function getWorkflows(page = 0, size = 20) {
  const response = await api.get<PageResponse<Workflow>>('/workflows', {
    params: { page, size, sort: 'createdAt,desc' },
  })
  return response.data
}

export async function createWorkflow(request: CreateWorkflowRequest) {
  const response = await api.post<Workflow>('/workflows', request)
  return response.data
}

export async function activateWorkflow(id: number) {
  const response = await api.post<Workflow>(`/workflows/${id}/activate`)
  return response.data
}

export async function pauseWorkflow(id: number) {
  const response = await api.post<Workflow>(`/workflows/${id}/pause`)
  return response.data
}

export async function archiveWorkflow(id: number) {
  const response = await api.post<Workflow>(`/workflows/${id}/archive`)
  return response.data
}

export async function getWorkflowExecutions(page = 0, size = 20) {
  const response = await api.get<PageResponse<WorkflowExecution>>(
    '/workflows/executions',
    { params: { page, size, sort: 'createdAt,desc' } },
  )
  return response.data
}

export async function retryWorkflowExecution(publicExecutionId: string) {
  const response = await api.post<WorkflowExecution>(
    `/workflows/executions/${publicExecutionId}/retry`,
  )
  return response.data
}


export async function cancelWorkflowExecution(publicExecutionId: string) {
  const response = await api.post<WorkflowExecution>(
    `/workflows/executions/${publicExecutionId}/cancel`,
  )
  return response.data
}
