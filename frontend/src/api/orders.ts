import client from './client'
import type { ApiResponse, Order, PageResponse, TrackingInfo } from '@/types'

export interface CreateOrderRequest {
  userEmail: string
  items: {
    productId: number
    listingId: number
    sellerId: number
    productName: string
    unitPrice: number
    quantity: number
  }[]
  shippingAddress: {
    fullName: string
    phone: string
    city: string
    district: string
    fullAddress: string
  }
}

export const ordersApi = {
  create: (data: CreateOrderRequest) =>
    client.post<ApiResponse<Order>>('/orders', data).then(r => r.data.data),

  getAll: (page = 0, size = 10) =>
    client.get<ApiResponse<PageResponse<Order>>>('/orders', { params: { page, size } }).then(r => r.data.data),

  getByNumber: (orderNumber: string) =>
    client.get<ApiResponse<Order>>(`/orders/${orderNumber}`).then(r => r.data.data),

  cancel: (orderNumber: string) =>
    client.delete<ApiResponse<void>>(`/orders/${orderNumber}`).then(r => r.data),

  track: (trackingNumber: string) =>
    client.get<ApiResponse<TrackingInfo>>(`/cargo/track/${trackingNumber}`).then(r => r.data.data),

  trackByOrder: (orderNumber: string) =>
    client.get<ApiResponse<TrackingInfo>>(`/cargo/orders/${orderNumber}`).then(r => r.data.data),

  getAllOrders: (page = 0, size = 20) =>
    client.get<ApiResponse<PageResponse<Order>>>('/orders/admin', { params: { page, size } }).then(r => r.data.data),
}
