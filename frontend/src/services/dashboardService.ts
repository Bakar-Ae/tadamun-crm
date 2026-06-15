import { api } from './api'

export type DashboardSummary = {
  totalUsers: number
  activeCustomers: number
  archivedCustomers: number
  activeLeads: number
  openTasks: number
  completedTasks: number
}

export async function getDashboardSummary() {
  const response = await api.get<DashboardSummary>('/dashboard/summary')
  return response.data
}