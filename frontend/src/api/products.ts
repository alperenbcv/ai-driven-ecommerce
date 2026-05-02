import client from './client'
import type { ApiResponse, PageResponse, Product, ProductListing, ProductProposal, Review } from '@/types'

export interface ProductFilters {
  page?: number
  size?: number
  keyword?: string
  categoryId?: number
  brandId?: number
  minPrice?: number
  maxPrice?: number
  sortBy?: string
  sortDir?: string
}

export interface ProductCreateRequest {
  name: string
  description?: string
  price: number
  categoryId: number
  brandId?: number
}

export const productsApi = {
  getAll: (filters: ProductFilters = {}) =>
    client.get<ApiResponse<PageResponse<Product>>>('/products', { params: filters }).then(r => r.data.data),

  getById: (id: number) =>
    client.get<ApiResponse<Product>>(`/products/${id}`).then(r => r.data.data),

  getListings: (productId: number) =>
    client.get<ApiResponse<ProductListing[]>>(`/products/${productId}/listings`).then(r => r.data.data),

  getReviews: (productId: number) =>
    client.get<ApiResponse<PageResponse<Review>>>(`/products/${productId}/reviews`).then(r => r.data.data.content),

  addReview: (productId: number, data: { rating: number; title: string; body: string }) =>
    client.post<ApiResponse<Review>>(`/products/${productId}/reviews`, data).then(r => r.data.data),

  markReviewHelpful: (productId: number, reviewId: number) =>
    client.post<ApiResponse<void>>(`/products/${productId}/reviews/${reviewId}/helpful`).then(r => r.data),

  createProduct: (data: ProductCreateRequest) =>
    client.post<ApiResponse<Product>>('/products', data).then(r => r.data.data),

  updateProduct: (id: number, data: Partial<Product>) =>
    client.put<ApiResponse<Product>>(`/products/${id}`, data).then(r => r.data.data),

  deleteProduct: (id: number) =>
    client.delete<ApiResponse<void>>(`/products/${id}`).then(r => r.data),

  submitProposal: (data: {
    proposedName: string
    proposedDescription?: string
    proposedPrice: number
    categoryId: number
    brandId?: number
  }) =>
    client.post<ApiResponse<ProductProposal>>('/proposals', data).then(r => r.data.data),

  getMyProposals: () =>
    client.get<ApiResponse<{ content: ProductProposal[] }>>('/proposals/my').then(r => r.data.data?.content ?? []),

  getPendingProposals: () =>
    client.get<ApiResponse<{ content: ProductProposal[] }>>('/proposals/pending').then(r => r.data.data?.content ?? []),

  reviewProposal: (id: number, data: {
    decision: string
    adminNote?: string
    existingProductId?: number
    approvedDescription?: string
  }) =>
    client.patch<ApiResponse<ProductProposal>>(`/proposals/${id}/review`, data).then(r => r.data),

  updateProposal: (id: number, data: {
    proposedName: string
    proposedDescription?: string
    proposedPrice: number
    categoryId: number
    brandId?: number
  }) =>
    client.put<ApiResponse<ProductProposal>>(`/proposals/${id}`, data).then(r => r.data.data),

  createListing: (productId: number, data: { price: number }) =>
    client.post<ApiResponse<ProductListing>>(`/products/${productId}/listings`, data).then(r => r.data.data),

  updateListingPrice: (productId: number, data: { price: number }) =>
    client.patch<ApiResponse<ProductListing>>(`/products/${productId}/listings`, data).then(r => r.data.data),

  deactivateListing: (productId: number) =>
    client.delete<ApiResponse<void>>(`/products/${productId}/listings`).then(r => r.data),

  addImageFromUrl: (productId: number, imageUrl: string, displayOrder = 0) =>
    client.post<ApiResponse<Product>>(`/products/${productId}/images/from-url`, { imageUrl, displayOrder }).then(r => r.data.data),

  getMyListings: () =>
    client.get<ApiResponse<ProductListing[]>>('/listings/my').then(r => r.data.data),

  getCategories: () =>
    client.get<ApiResponse<{ id: number; name: string }[]>>('/categories').then(r => r.data.data),

  getBrands: () =>
    client.get<ApiResponse<{ id: number; name: string }[]>>('/brands').then(r => r.data.data),
}
