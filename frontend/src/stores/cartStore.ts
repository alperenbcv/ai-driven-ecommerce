import { create } from 'zustand'
import type { Cart } from '@/types'

interface CartStore {
  cart: Cart | null
  setCart: (cart: Cart | null) => void
  totalItems: number
}

export const useCartStore = create<CartStore>((set) => ({
  cart: null,
  setCart: (cart) => set({ cart, totalItems: cart?.totalQuantity ?? 0 }),
  totalItems: 0,
}))
