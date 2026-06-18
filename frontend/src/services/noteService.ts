import { api } from './api'
import type { PageResponse } from './userService'

export type NoteResponse = {
  id: number
  content: string
  customerId: number | null
  customerName: string | null
  leadId: number | null
  leadName: string | null
  createdByUserId: number
  createdByUserName: string
  createdAt: string
  updatedAt: string
}

export type CreateNoteRequest = {
  content: string
  customerId?: number | null
  leadId?: number | null
}

export async function getCustomerNotes(customerId: number, page = 0, size = 10) {
  const response = await api.get<PageResponse<NoteResponse>>(`/notes/customers/${customerId}`, {
    params: {
      page,
      size,
      sort: 'id,desc',
    },
  })

  return response.data
}

export async function getLeadNotes(leadId: number, page = 0, size = 10) {
  const response = await api.get<PageResponse<NoteResponse>>(`/notes/leads/${leadId}`, {
    params: {
      page,
      size,
      sort: 'id,desc',
    },
  })

  return response.data
}

export async function createNote(request: CreateNoteRequest) {
  const response = await api.post<NoteResponse>('/notes', request)
  return response.data
}
