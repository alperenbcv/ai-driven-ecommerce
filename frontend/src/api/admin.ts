import client from './client'
import type { ApiResponse, PageResponse, Stock, User } from '@/types'

export const adminApi = {
  getUsers: (page = 0, size = 20) =>
    client.get<ApiResponse<PageResponse<User>>>('/users', { params: { page, size } }).then(r => r.data.data),

  changeUserRole: (userId: number, role: string) =>
    client.patch<ApiResponse<void>>(`/users/${userId}/role`, null, { params: { role } }).then(r => r.data),

  updateStoreProfile: (userId: number, data: { storeName: string; storeDescription?: string }) =>
    client.put<ApiResponse<User>>(`/users/${userId}/store-profile`, data).then(r => r.data.data),

  getStock: (productId: number) =>
    client.get<ApiResponse<Stock>>(`/stocks/product/${productId}`).then(r => r.data.data),

  adjustStock: (
    productId: number,
    data: { quantity: number; reason?: string },
    sellerId?: number,
  ) =>
    client
      .patch<ApiResponse<Stock>>(`/stocks/product/${productId}/adjust`, data, {
        params: sellerId !== undefined ? { sellerId } : {},
      })
      .then(r => r.data.data),
}
