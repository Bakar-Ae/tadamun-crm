import { api } from './api'
import type { PageResponse } from './userService'

export type IntegrationProvider = 'WHATSAPP_CLOUD' | 'SMTP'
export type IntegrationConnectionStatus =
  | 'DRAFT'
  | 'ACTIVE'
  | 'DISABLED'
  | 'ERROR'
  | 'REVOKED'
export type IntegrationCapability =
  | 'SEND_MESSAGE'
  | 'SEND_EMAIL'
  | 'TEST_CONNECTION'
export type IntegrationDeliveryType = 'WHATSAPP_TEXT' | 'EMAIL'
export type IntegrationDeliveryStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'RETRY_SCHEDULED'
  | 'SUCCEEDED'
  | 'TERMINAL_FAILURE'
  | 'DEAD'
  | 'CANCELLED'

export type IntegrationConnection = {
  id: number
  publicConnectionId: string
  provider: IntegrationProvider
  name: string
  status: IntegrationConnectionStatus
  configuration: Record<string, unknown>
  capabilities: IntegrationCapability[]
  credentialsConfigured: boolean
  externalAccountId: string | null
  credentialsUpdatedAt: string | null
  lastVerifiedAt: string | null
  lastErrorCategory: string | null
  lastErrorMessage: string | null
  createdByUserId: number
  createdByName: string
  revokedAt: string | null
  createdAt: string
  updatedAt: string
}

export type SaveIntegrationConnectionRequest = {
  name: string
  provider?: IntegrationProvider
  configuration: Record<string, unknown>
  credentials?: Record<string, string>
}

export type IntegrationDelivery = {
  publicDeliveryId: string
  connectionId: number
  connectionName: string
  provider: IntegrationProvider
  type: IntegrationDeliveryType
  destination: string
  subject: string | null
  status: IntegrationDeliveryStatus
  attemptCount: number
  maximumAttempts: number
  nextAttemptAt: string | null
  providerMessageId: string | null
  lastErrorCategory: string | null
  lastErrorMessage: string | null
  createdByUserId: number | null
  createdByName: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export type IntegrationDeliveryAttempt = {
  attemptNumber: number
  outcome: 'SUCCEEDED' | 'RETRYABLE_FAILURE' | 'TERMINAL_FAILURE'
  durationMs: number
  providerMessageId: string | null
  errorCategory: string | null
  errorMessage: string | null
  attemptedAt: string
}

export type QueueIntegrationDeliveryRequest = {
  type: IntegrationDeliveryType
  destination: string
  subject?: string | null
  body: string
  idempotencyKey?: string
}

export async function getIntegrationConnections(page = 0, size = 50) {
  const response = await api.get<PageResponse<IntegrationConnection>>(
    '/integrations',
    { params: { page, size, sort: 'createdAt,desc' } },
  )
  return response.data
}

export async function createIntegrationConnection(
  request: SaveIntegrationConnectionRequest & { provider: IntegrationProvider },
) {
  const response = await api.post<IntegrationConnection>('/integrations', request)
  return response.data
}

export async function updateIntegrationConnection(
  id: number,
  request: Omit<SaveIntegrationConnectionRequest, 'provider'>,
) {
  const response = await api.put<IntegrationConnection>(`/integrations/${id}`, request)
  return response.data
}

export async function verifyIntegrationConnection(id: number) {
  const response = await api.post<IntegrationConnection>(`/integrations/${id}/verify`)
  return response.data
}

export async function disableIntegrationConnection(id: number) {
  const response = await api.post<IntegrationConnection>(`/integrations/${id}/disable`)
  return response.data
}

export async function revokeIntegrationConnection(id: number) {
  const response = await api.post<IntegrationConnection>(`/integrations/${id}/revoke`)
  return response.data
}

export async function getIntegrationDeliveries(
  connectionId: number,
  page = 0,
  size = 10,
) {
  const response = await api.get<PageResponse<IntegrationDelivery>>(
    `/integrations/${connectionId}/deliveries`,
    { params: { page, size, sort: 'createdAt,desc' } },
  )
  return response.data
}

export async function queueIntegrationDelivery(
  connectionId: number,
  request: QueueIntegrationDeliveryRequest,
) {
  const response = await api.post<IntegrationDelivery>(
    `/integrations/${connectionId}/deliveries`,
    request,
  )
  return response.data
}

export async function getIntegrationDeliveryAttempts(publicDeliveryId: string) {
  const response = await api.get<IntegrationDeliveryAttempt[]>(
    `/integrations/deliveries/${publicDeliveryId}/attempts`,
  )
  return response.data
}

export async function retryIntegrationDelivery(publicDeliveryId: string) {
  const response = await api.post<IntegrationDelivery>(
    `/integrations/deliveries/${publicDeliveryId}/retry`,
  )
  return response.data
}

export async function cancelIntegrationDelivery(publicDeliveryId: string) {
  const response = await api.post<IntegrationDelivery>(
    `/integrations/deliveries/${publicDeliveryId}/cancel`,
  )
  return response.data
}
