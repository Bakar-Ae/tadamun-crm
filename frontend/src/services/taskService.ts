import { api } from './api'
import type { PageResponse } from './userService'

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type TaskResponse = {
  id: number
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  assignedToUserId: number | null
  assignedToUserName: string | null
  customerId: number | null
  customerName: string | null
  leadId: number | null
  leadName: string | null
  createdAt: string
  updatedAt: string
}

export async function getTasks(page = 0, size = 10, keyword = '') {
  const response = await api.get<PageResponse<TaskResponse>>('/tasks', {
    params: {
      page,
      size,
      keyword,
      sort: 'id,desc',
    },
  })

  return response.data
}