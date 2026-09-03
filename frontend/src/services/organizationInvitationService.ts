import { api, publicApi } from './api'
import type { PageResponse, RoleName } from './userService'

export type OrganizationInvitationPreview = {
  organizationName: string
  email: string
  role: RoleName
  expiresAt: string
  requiresAccountCreation: boolean
}

export type AcceptOrganizationInvitationRequest = {
  token: string
  fullName?: string
  password?: string
  confirmPassword?: string
}

export type OrganizationInvitationAcceptance = {
  organizationId: number
  organizationName: string
  userId: number
  email: string
  role: RoleName
}

export type OrganizationInvitationStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REVOKED'
  | 'EXPIRED'

export type OrganizationInvitationResponse = {
  id: number
  organizationId: number
  organizationName: string
  email: string
  role: RoleName
  status: OrganizationInvitationStatus
  invitedByUserId: number
  invitedByUserName: string
  expiresAt: string
  acceptedAt: string | null
  revokedAt: string | null
  createdAt: string
}

export type CreateOrganizationInvitationRequest = {
  email: string
  role: Exclude<RoleName, 'OWNER'>
}

export async function previewOrganizationInvitation(token: string) {
  const response = await publicApi.post<OrganizationInvitationPreview>(
    '/public/organization-invitations/preview',
    { token },
  )

  return response.data
}

export async function acceptOrganizationInvitation(
  request: AcceptOrganizationInvitationRequest,
) {
  const response = await publicApi.post<OrganizationInvitationAcceptance>(
    '/public/organization-invitations/accept',
    request,
  )

  return response.data
}

export async function getOrganizationInvitations(page = 0, size = 10) {
  const response = await api.get<PageResponse<OrganizationInvitationResponse>>(
    '/organization-invitations',
    {
      params: {
        page,
        size,
        sort: 'id,desc',
      },
    },
  )

  return response.data
}

export async function createOrganizationInvitation(
  request: CreateOrganizationInvitationRequest,
) {
  const response = await api.post<OrganizationInvitationResponse>(
    '/organization-invitations',
    request,
  )
  return response.data
}

export async function revokeOrganizationInvitation(invitationId: number) {
  const response = await api.patch<OrganizationInvitationResponse>(
    `/organization-invitations/${invitationId}/revoke`,
  )
  return response.data
}
