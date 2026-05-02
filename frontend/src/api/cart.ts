import client from './client'
import type { ApiResponse, Cart } from '@/types'

export const cartApi = {
  getCart: () =>
    client.get<ApiResponse<Cart>>('/cart').then(r => r.data.data),

  addItem: (item: { productId: number; listingId: number; sellerId: number; productName: string; unitPrice: number; quantity?: number }) =>
    client.post<ApiResponse<Cart>>('/cart/items', item).then(r => r.data.data),

  updateQuantity: (productId: number, listingId: number, quantity: number) =>
    client.patch<ApiResponse<Cart>>(`/cart/items/${productId}/${listingId}?quantity=${quantity}`).then(r => r.data.data),

  removeItem: (productId: number, listingId: number) =>
    client.delete<ApiResponse<Cart>>(`/cart/items/${productId}/${listingId}`).then(r => r.data.data),

  clearCart: () =>
    client.delete<ApiResponse<void>>('/cart').then(r => r.data),
}
