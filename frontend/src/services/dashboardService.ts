import { api } from './api'

export type DashboardSummary = {
  totalUsers: number
  activeCustomers: number
  archivedCustomers: number
  activeLeads: number
  openTasks: number
  completedTasks: number
}

export type TeamMemberWorkload = {
  userId: number
  fullName: string
  activeCustomers: number
  activeLeads: number
  openTasks: number
  completedTasks: number
  recentActivities: number
}

export type TeamActivity = {
  id: number
  actorUserId: number | null
  actorName: string
  action: string
  entityType: string
  entityId: number | null
  createdAt: string
}

export type TeamDashboard = {
  scope: 'OWN' | 'TEAM' | 'ALL'
  teamId: number | null
  teamName: string
  summary: DashboardSummary
  overdueTasks: number
  taskCompletionRate: number
  members: TeamMemberWorkload[]
  recentActivity: TeamActivity[]
}

export async function getDashboardSummary() {
  const response = await api.get<DashboardSummary>('/dashboard/summary')
  return response.data
}

export async function getTeamDashboard() {
  const response = await api.get<TeamDashboard>('/dashboard/team')
  return response.data
}
