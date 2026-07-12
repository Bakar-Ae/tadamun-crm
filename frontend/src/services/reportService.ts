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

export type ReportBreakdownItem = {
  key: string
  count: number
}

export type ReportDailyActivity = {
  date: string
  count: number
}

export type AdvancedReport = {
  from: string
  to: string
  customersCreated: number
  leadsCreated: number
  leadConversions: number
  tasksCreated: number
  taskCompletions: number
  activitiesRecorded: number
  customerActivities: number
  leadStatusBreakdown: ReportBreakdownItem[]
  taskStatusBreakdown: ReportBreakdownItem[]
  taskPriorityBreakdown: ReportBreakdownItem[]
  dailyActivity: ReportDailyActivity[]
}

export async function getReportSummary() {
  const response = await api.get<ReportSummary>('/reports/summary')
  return response.data
}

export async function getAdvancedReport(from: string, to: string) {
  const response = await api.get<AdvancedReport>('/reports/advanced', {
    params: { from, to },
  })

  return response.data
}

export type ReportExportFormat = 'excel' | 'pdf'

export async function downloadAdvancedReport(
  format: ReportExportFormat,
  from: string,
  to: string,
) {
  const response = await api.get<Blob>(
    `/reports/advanced/export/${format}`,
    {
      params: { from, to },
      responseType: 'blob',
    },
  )

  return response.data
}
