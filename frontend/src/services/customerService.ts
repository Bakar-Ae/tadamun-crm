import { api } from './api'
import type { PageResponse } from './userService'

export type CustomerType = 'INDIVIDUAL' | 'COMPANY'
export type CustomerStatus = 'ACTIVE' | 'ARCHIVED'

export type CustomerResponse = {
  id: number
  name: string
  email: string | null
  phone: string | null
  companyName: string | null
  customerType: CustomerType
  status: CustomerStatus
  createdAt: string
  updatedAt: string
}

export type CreateCustomerRequest = {
  name: string
  email?: string | null
  phone?: string | null
  companyName?: string | null
  customerType: CustomerType
}

export type UpdateCustomerRequest = CreateCustomerRequest & {
  status: CustomerStatus
}

export async function getCustomers(page = 0, size = 10, keyword = '') {
  const response = await api.get<PageResponse<CustomerResponse>>('/customers', {
    params: {
      page,
      size,
      keyword,
      sort: 'id,desc',
    },
  })

  return response.data
}

export async function createCustomer(request: CreateCustomerRequest) {
  const response = await api.post<CustomerResponse>('/customers', request)
  return response.data
}

export async function updateCustomer(id: number, request: UpdateCustomerRequest) {
  const response = await api.put<CustomerResponse>(`/customers/${id}`, request)
  return response.data
}

export async function archiveCustomer(id: number) {
  const response = await api.patch<CustomerResponse>(`/customers/${id}/archive`)
  return response.data
}
