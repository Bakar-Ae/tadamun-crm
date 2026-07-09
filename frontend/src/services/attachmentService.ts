import { api } from './api'
import type { PageResponse } from './userService'

export type AttachmentStatus = 'ACTIVE' | 'DELETED'

export type AttachmentResponse = {
  id: number
  originalFileName: string
  contentType: string
  sizeBytes: number
  checksumSha256: string
  customerId: number | null
  leadId: number | null
  uploadedByUserId: number
  uploadedByName: string
  status: AttachmentStatus
  createdAt: string
}

export async function getCustomerAttachments(
  customerId: number,
  page = 0,
  size = 20,
) {
  const response = await api.get<PageResponse<AttachmentResponse>>(
    `/attachments/customers/${customerId}`,
    { params: { page, size } },
  )

  return response.data
}

export async function getLeadAttachments(
  leadId: number,
  page = 0,
  size = 20,
) {
  const response = await api.get<PageResponse<AttachmentResponse>>(
    `/attachments/leads/${leadId}`,
    { params: { page, size } },
  )

  return response.data
}

export async function uploadCustomerAttachment(
  customerId: number,
  file: File,
) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await api.post<AttachmentResponse>(
    `/attachments/customers/${customerId}`,
    formData,
  )

  return response.data
}

export async function uploadLeadAttachment(
  leadId: number,
  file: File,
) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await api.post<AttachmentResponse>(
    `/attachments/leads/${leadId}`,
    formData,
  )

  return response.data
}

export async function downloadAttachment(
  attachment: AttachmentResponse,
) {
  const response = await api.get<Blob>(
    `/attachments/${attachment.id}/download`,
    { responseType: 'blob' },
  )

  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')

  link.href = url
  link.download = attachment.originalFileName

  document.body.appendChild(link)
  link.click()
  link.remove()

  URL.revokeObjectURL(url)
}

export async function deleteAttachment(id: number) {
  await api.delete(`/attachments/${id}`)
}