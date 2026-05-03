import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import { performLogout } from '@/lib/logout'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ? `${import.meta.env.VITE_API_URL}/api` : '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Her isteğe otomatik JWT eklenecek
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRefreshing = false
let pendingQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = []

function processQueue(error: unknown, token: string | null) {
  pendingQueue.forEach((p) => (token ? p.resolve(token) : p.reject(error)))
  pendingQueue = []
}

// 401 refresh token ile yeni access token al, başarısızsa logout
client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config

    if (
      error.response?.status !== 401 ||
      original._retry ||
      original.url?.includes('/auth/refresh-token')
    ) {
      return Promise.reject(error)
    }

    const refreshToken = useAuthStore.getState().refreshToken

    // Refresh token yoksa direkt logout
    if (!refreshToken) {
      performLogout()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (isRefreshing) {
      // Refresh sürüyorsa bu isteği kuyruğa al
      return new Promise((resolve, reject) => {
        pendingQueue.push({
          resolve: (token) => {
            original.headers.Authorization = `Bearer ${token}`
            resolve(client(original))
          },
          reject,
        })
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      // Yeni token al
      const res = await axios.post<{ data: { accessToken: string; refreshToken: string } }>(
        '/api/auth/refresh-token',
        { refreshToken }
      )
      const { accessToken, refreshToken: newRefresh } = res.data.data

      useAuthStore.getState().updateTokens(accessToken, newRefresh)
      client.defaults.headers.common.Authorization = `Bearer ${accessToken}`

      processQueue(null, accessToken)

      original.headers.Authorization = `Bearer ${accessToken}`
      return client(original)
    } catch (refreshError) {
      processQueue(refreshError, null)
      performLogout()
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }
)

export default client
