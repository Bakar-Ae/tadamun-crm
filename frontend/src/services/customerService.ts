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
