import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081/api/v1'

export const api = axios.create({
  baseURL: apiBaseUrl,
})

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean
}

type RefreshTokenResponse = {
  accessToken: string
  tokenType: string
}

function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
}


api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined
    const status = error.response?.status
    const refreshToken = localStorage.getItem('refreshToken')
    const requestUrl = originalRequest?.url ?? ''
    const isAuthRequest =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/refresh') ||
      requestUrl.includes('/auth/logout')

    if (status !== 401 || !originalRequest || originalRequest._retry || !refreshToken || isAuthRequest) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      const response = await axios.post<RefreshTokenResponse>(`${apiBaseUrl}/auth/refresh`, {
        refreshToken,
      })

      localStorage.setItem('token', response.data.accessToken)
      originalRequest.headers.Authorization = `Bearer ${response.data.accessToken}`

      return api(originalRequest)
    } catch (refreshError) {
      clearSession()
      window.location.href = '/'
      return Promise.reject(refreshError)
    }
  },
)
