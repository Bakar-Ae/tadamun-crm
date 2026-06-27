import { api } from './api'
import type { PermissionName } from './permissionService'

export type LoginRequest = {
  email: string
  password: string
}

export type LoginResponse = {
  accessToken: string | null
  refreshToken: string | null
  tokenType: string | null
  userId: number
  fullName: string
  email: string
  role: string
  permissions: PermissionName[]
  passwordChangeRequired: boolean
}

export type ChangePasswordRequest = {
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}

export type ForgotPasswordRequest = {
  email: string
}

export type ResetPasswordRequest = {
  token: string
  newPassword: string
  confirmNewPassword: string
}

export async function login(request: LoginRequest) {
  const response = await api.post<LoginResponse>('/auth/login', request)
  return response.data
}

export async function logout(refreshToken: string) {
  await api.post('/auth/logout', { refreshToken })
}

export async function getMe() {
  const response = await api.get<LoginResponse>('/auth/me')
  return response.data
}

export async function changePassword(request: ChangePasswordRequest) {
  await api.patch('/auth/password', request)
}

export async function forgotPassword(request: ForgotPasswordRequest): Promise<void> {
  await api.post('/auth/forgot-password', request)
}

export async function resetPassword(request: ResetPasswordRequest): Promise<void> {
  await api.post('/auth/reset-password', request)
}
