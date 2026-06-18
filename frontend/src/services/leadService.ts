import { api } from './api'
import type { PageResponse } from './userService'

export type LeadStatus = 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'LOST' | 'CONVERTED' | 'ARCHIVED'

export type LeadResponse = {
  id: number
  fullName: string
  email: string | null
  phone: string | null
  companyName: string | null
  source: string | null
  status: LeadStatus
  estimatedValue: number | null
  assignedToUserId: number | null
  assignedToUserName: string | null
  convertedCustomerId: number | null
  createdAt: string
  updatedAt: string
}

export type CreateLeadRequest = {
  fullName: string
  email?: string | null
  phone?: string | null
  companyName?: string | null
  source?: string | null
  estimatedValue?: number | null
  assignedToUserId?: number | null
}

export async function getLeads(page = 0, size = 10, keyword = '') {
  const response = await api.get<PageResponse<LeadResponse>>('/leads', {
    params: {
      page,
      size,
      keyword,
      sort: 'id,desc',
    },
  })

  return response.data
}

export async function createLead(request: CreateLeadRequest) {
  const response = await api.post<LeadResponse>('/leads', request)
  return response.data
}
