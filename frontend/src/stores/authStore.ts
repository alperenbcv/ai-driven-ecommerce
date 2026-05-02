import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types'

interface AuthStore {
  user: User | null
  token: string | null
  refreshToken: string | null
  login: (user: User, token: string, refreshToken?: string | null) => void
  logout: () => void
  updateUser: (user: User) => void
  updateTokens: (accessToken: string, refreshToken: string) => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      refreshToken: null,
      login: (user, token, refreshToken = null) => set({ user, token, refreshToken }),
      logout: () => set({ user: null, token: null, refreshToken: null }),
      updateUser: (user) => set({ user }),
      updateTokens: (accessToken, refreshToken) => set({ token: accessToken, refreshToken }),
    }),
    { name: 'auth' },
  ),
)

export const useUser = () => useAuthStore((s) => s.user)
export const useToken = () => useAuthStore((s) => s.token)
export const useIsAuthenticated = () => useAuthStore((s) => !!s.token)
export const useIsAdmin = () => useAuthStore((s) => s.user?.role === 'ADMIN')
export const useIsSeller = () => useAuthStore((s) => s.user?.role === 'SELLER' || s.user?.role === 'ADMIN')
