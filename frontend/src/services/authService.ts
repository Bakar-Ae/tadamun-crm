import { api } from './api'

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
}
export async function logout(refreshToken: string) {
  await api.post('/auth/logout', { refreshToken })
}

export async function login(request: LoginRequest) {
  const response = await api.post<LoginResponse>('/auth/login', request)
  return response.data
}
export type ChangePasswordRequest = {
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}

export async function getMe() {
  const response = await api.get<LoginResponse>('/auth/me')
  return response.data
}
export async function changePassword(request: ChangePasswordRequest) {
  await api.patch('/auth/password', request)
}