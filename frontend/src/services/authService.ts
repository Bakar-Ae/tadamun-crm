import { api } from './api'

export type LoginRequest = {
  email: string
  password: string
}

export type LoginResponse = {
  token: string
  tokenType: string
  userId: number
  fullName: string
  email: string
  role: string
}

export async function login(request: LoginRequest) {
  const response = await api.post<LoginResponse>('/auth/login', request)
  return response.data
}

export async function getMe() {
  const response = await api.get<LoginResponse>('/auth/me')
  return response.data
}