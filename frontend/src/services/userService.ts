import { api } from './api'

export type UserStatus = 'ACTIVE' | 'INACTIVE'
export type RoleName = 'ADMIN' | 'MANAGER' | 'SALES_REP' | 'SUPPORT_STAFF'

export type UserResponse = {
  id: number
  fullName: string
  email: string
  role: RoleName
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export type CreateUserRequest = {
  fullName: string
  email: string
  password: string
  role: RoleName
}

export type UpdateUserRequest = {
  fullName: string
  email: string
  role: RoleName
  status: UserStatus
}

export async function getUsers(page = 0, size = 10, keyword = '') {
  const response = await api.get<PageResponse<UserResponse>>('/users', {
    params: {
      page,
      size,
      keyword,
      sort: 'id,desc',
    },
  })

  return response.data
}

export async function createUser(request: CreateUserRequest) {
  const response = await api.post<UserResponse>('/users', request)
  return response.data
}

export async function updateUser(id: number, request: UpdateUserRequest) {
  const response = await api.put<UserResponse>(`/users/${id}`, request)
  return response.data
}

export async function deactivateUser(id: number) {
  const response = await api.patch<UserResponse>(`/users/${id}/deactivate`)
  return response.data
}
