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

export type CreateTaskRequest = {
  title: string
  description?: string | null
  priority: TaskPriority
  dueDate?: string | null
  assignedToUserId?: number | null
  customerId?: number | null
  leadId?: number | null
}

export type UpdateTaskRequest = CreateTaskRequest & {
  status: TaskStatus
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

export async function createTask(request: CreateTaskRequest) {
  const response = await api.post<TaskResponse>('/tasks', request)
  return response.data
}

export async function updateTask(id: number, request: UpdateTaskRequest) {
  const response = await api.put<TaskResponse>(`/tasks/${id}`, request)
  return response.data
}
