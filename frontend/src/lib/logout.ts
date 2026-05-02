import { queryClient } from '@/queryClient'
import { useAuthStore } from '@/stores/authStore'
import { useCartStore } from '@/stores/cartStore'

export function performLogout() {
  useAuthStore.getState().logout()
  useCartStore.getState().setCart(null)
  queryClient.clear()
}
