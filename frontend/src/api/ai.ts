import client from './client'
import type { ApiResponse } from '@/types'

export const aiApi = {
  search: (q: string, topK = 10, minScore = 0.2) =>
    client.get<ApiResponse<number[]>>('/search', { params: { q, topK, minScore } }).then(r => r.data.data),

  getRecommendationsForUser: (userId: number, limit = 10) =>
    client.get<ApiResponse<number[]>>(`/recommendations/users/${userId}`, { params: { limit } }).then(r => r.data.data),

  getProductRecommendations: (productId: number, limit = 6) =>
    client.get<ApiResponse<number[]>>(`/recommendations/products/${productId}`, { params: { limit } }).then(r => r.data.data),

  getPopularProducts: (limit = 8) =>
    client.get<ApiResponse<number[]>>('/recommendations/popular', { params: { limit } }).then(r => r.data.data),

  trackView: (productId: number, productName?: string, category?: string) =>
    client.post('/recommendations/track/view', { productId, productName, category }).catch(() => {}),

  generateProductImage: (productName: string, description?: string, categoryName?: string) =>
    client.post<ApiResponse<{ imageUrl: string }>>(
      '/assistant/generate-product-image',
      { productName, description: description ?? '', categoryName: categoryName ?? '' }
    ).then(r => r.data.data.imageUrl),

  chat: (message: string, sessionId?: string) =>
    client.post<ApiResponse<{
      sessionId: string
      reply: string
      model: string
      products?: Array<{
        id: number
        name: string
        description?: string
        price: number
        categoryName?: string
        brandName?: string
        averageRating?: number
        reviewCount?: number
      }>
    }>>('/assistant/chat', { message, sessionId }).then(r => r.data.data),

  generateProductDescription: (data: {
    productName: string
    categoryName?: string
    brandName?: string
    currentDescription?: string
  }) =>
    client.post<ApiResponse<{
      description: string
      seoTitle?: string
      tags?: string[]
    }>>('/assistant/product-description', data).then(r => r.data.data),

  clearSession: (sessionId: string) =>
    client.delete<ApiResponse<void>>(`/assistant/chat/${sessionId}`).then(r => r.data),
}
