import { api } from './api'
import type { PageResponse } from './userService'

export type ContactStatus = 'ACTIVE' | 'ARCHIVED'

export type ContactResponse = {
  id: number
  customerId: number
  customerName: string
  fullName: string
  email: string | null
  phone: string | null
  position: string | null
  status: ContactStatus
  createdAt: string
  updatedAt: string
}

export type CreateContactRequest = {
  customerId: number
  fullName: string
  email?: string | null
  phone?: string | null
  position?: string | null
}

export type UpdateContactRequest = Omit<CreateContactRequest, 'customerId'> & {
  status: ContactStatus
}
export type ContactFilters = {
  keyword?: string
  status?: ContactStatus | ''
  customerId?: number | null
}

export async function getContacts(
  page = 0,
  size = 10,
  filters: ContactFilters | string = '',
) {
  const cleanFilters =
    typeof filters === 'string' ? { keyword: filters } : filters

  const response = await api.get<PageResponse<ContactResponse>>('/contacts', {
    params: {
      page,
      size,
      keyword: cleanFilters.keyword || undefined,
      status: cleanFilters.status || undefined,
      customerId: cleanFilters.customerId || undefined,
      sort: 'id,desc',
    },
  })

  return response.data
}

export async function createContact(request: CreateContactRequest) {
  const response = await api.post<ContactResponse>('/contacts', request)
  return response.data
}

export async function updateContact(id: number, request: UpdateContactRequest) {
  const response = await api.put<ContactResponse>(`/contacts/${id}`, request)
  return response.data
}

export async function archiveContact(id: number) {
  const response = await api.patch<ContactResponse>(`/contacts/${id}/archive`)
  return response.data
}
