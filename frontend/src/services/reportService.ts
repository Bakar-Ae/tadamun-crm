import { api } from './api'

export type ReportSummary = {
  totalCustomers: number
  activeCustomers: number
  archivedCustomers: number
  totalLeads: number
  newLeads: number
  qualifiedLeads: number
  convertedLeads: number
  lostLeads: number
  totalTasks: number
  openTasks: number
  inProgressTasks: number
  completedTasks: number
  cancelledTasks: number
}

export async function getReportSummary() {
  const response = await api.get<ReportSummary>('/reports/summary')
  return response.data
}