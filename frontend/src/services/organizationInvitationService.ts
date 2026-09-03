import { publicApi } from './api'
import type { RoleName } from './userService'

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
