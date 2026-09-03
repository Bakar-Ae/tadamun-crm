import { api } from './api'
import type { PageResponse, RoleName, UserStatus } from './userService'

export type OrganizationStatus = 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED'
export type OrganizationMembershipStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE'

export type OrganizationResponse = {
  id: number
  name: string
  slug: string
  status: OrganizationStatus
  timeZone: string
  createdByUserId: number | null
  createdByName: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export type OrganizationMembershipResponse = {
  id: number
  organizationId: number
  organizationName: string
  organizationSlug: string
  organizationStatus: OrganizationStatus
  userId: number
  userFullName: string
  userEmail: string
  userStatus: UserStatus
  role: RoleName
  status: OrganizationMembershipStatus
  joinedAt: string
  version: number
  createdAt: string
  updatedAt: string
}

export type UpdateOrganizationRequest = {
  name: string
  timeZone: string
  version: number
}

export async function getCurrentOrganization() {
  const response = await api.get<OrganizationResponse>('/organization')
  return response.data
}

export async function updateCurrentOrganization(
  request: UpdateOrganizationRequest,
) {
  const response = await api.put<OrganizationResponse>(
    '/organization',
    request,
  )
  return response.data
}

export async function getOrganizationMembers(page = 0, size = 10) {
  const response = await api.get<PageResponse<OrganizationMembershipResponse>>(
    '/organization/members',
    {
      params: {
        page,
        size,
        sort: 'id,asc',
      },
    },
  )
  return response.data
}

export async function updateOrganizationMemberRole(
  membershipId: number,
  role: RoleName,
  version: number,
) {
  const response = await api.patch<OrganizationMembershipResponse>(
    `/organization/members/${membershipId}`,
    { role, version },
  )
  return response.data
}

export async function deactivateOrganizationMember(
  membershipId: number,
  version: number,
) {
  const response = await api.patch<OrganizationMembershipResponse>(
    `/organization/members/${membershipId}/deactivate`,
    { version },
  )
  return response.data
}
