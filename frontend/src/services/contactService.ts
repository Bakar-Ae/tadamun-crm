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

export async function getContacts(page = 0, size = 10, keyword = '') {
  const response = await api.get<PageResponse<ContactResponse>>('/contacts', {
    params: {
      page,
      size,
      keyword,
      sort: 'id,desc',
    },
  })

  return response.data
}