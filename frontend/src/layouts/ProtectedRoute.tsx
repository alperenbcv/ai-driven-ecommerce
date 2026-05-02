import { Navigate, Outlet } from 'react-router-dom'
import { useIsAuthenticated, useUser } from '@/stores/authStore'

interface Props { role?: 'ADMIN' | 'SELLER' }

export function ProtectedRoute({ role }: Props) {
  const isAuth = useIsAuthenticated()
  const user = useUser()

  if (!isAuth) return <Navigate to="/login" replace />

  if (role === 'ADMIN' && user?.role !== 'ADMIN') return <Navigate to="/" replace />
  if (role === 'SELLER' && user?.role !== 'SELLER' && user?.role !== 'ADMIN') return <Navigate to="/" replace />

  return <Outlet />
}
