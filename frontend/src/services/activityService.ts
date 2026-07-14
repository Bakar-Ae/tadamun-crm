import { api } from './api'
import type { PageResponse } from './userService'

type ActivityEventApiResponse = {
  eventKey: string
  type: 'NOTE' | 'TASK' | 'AUDIT'
  sourceId: number
  title: string
  description: string | null
  actorName: string | null
  status: string | null
  occurredAt: string | null
}

export type ActivityEvent = {
  id: string
  type: 'note' | 'task' | 'audit'
  sourceId: number
  title: string
  description: string | null
  actor: string | null
  status: string | null
  createdAt: string | null
}

function normalizeActivityPage(
  page: PageResponse<ActivityEventApiResponse>,
): PageResponse<ActivityEvent> {
  return {
    ...page,
    content: page.content.map((event) => ({
      id: event.eventKey,
      type: event.type.toLowerCase() as ActivityEvent['type'],
      sourceId: event.sourceId,
      title: event.title,
      description: event.description,
      actor: event.actorName,
      status: event.status,
      createdAt: event.occurredAt,
    })),
  }
}

export async function getCustomerActivity(
  customerId: number,
  page = 0,
  size = 20,
) {
  const response = await api.get<PageResponse<ActivityEventApiResponse>>(
    `/activities/customers/${customerId}`,
    {
      params: { page, size },
    },
  )

  return normalizeActivityPage(response.data)
}

export async function getLeadActivity(
  leadId: number,
  page = 0,
  size = 20,
) {
  const response = await api.get<PageResponse<ActivityEventApiResponse>>(
    `/activities/leads/${leadId}`,
    {
      params: { page, size },
    },
  )

  return normalizeActivityPage(response.data)
}