export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  phone?: string
  role: 'USER' | 'SELLER' | 'ADMIN'
}

export interface AuthState {
  user: User | null
  token: string | null
}

export interface Category {
  id: number
  name: string
  parentId?: number
}

export interface Brand {
  id: number
  name: string
}

export interface Product {
  id: number
  name: string
  description: string
  price: number
  category: { id: number; name: string }
  brand: { id: number; name: string }
  averageRating: number
  reviewCount: number
  active: boolean
  images?: ProductImage[]
}

export interface ProductImage {
  id: number
  url: string
  publicId: string
  displayOrder: number
}

export interface ProductListing {
  id: number
  productId: number
  productName: string
  sellerId: number
  price: number
  active: boolean
}

export interface ProductProposal {
  id: number
  sellerId: number
  proposedName: string
  proposedDescription: string
  proposedPrice: number
  categoryId?: number
  brandId?: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVISION_REQUESTED'
  adminNote?: string
  approvedProductId?: number
  createdAt: string
}

export interface Review {
  id: number
  userId: number
  userName: string
  rating: number
  title: string
  body: string
  verifiedPurchase: boolean
  helpfulCount: number
  createdAt: string
}

export interface CartItem {
  productId: number
  listingId: number
  sellerId: number
  productName: string
  unitPrice: number
  quantity: number
}

export interface Cart {
  userId: number
  items: CartItem[]
  totalItems: number
  totalQuantity: number
  totalAmount: number
}

export type OrderStatus =
  | 'PENDING'
  | 'STOCK_RESERVED'
  | 'PAYMENT_PROCESSING'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export interface OrderItem {
  id: number
  productId: number
  productName: string
  unitPrice: number
  quantity: number
  totalPrice: number
  sellerId: number
}

export interface Order {
  id: number
  orderNumber: string
  userId: number
  status: OrderStatus
  totalAmount: number
  shippingFullName: string
  shippingPhone: string
  shippingCity: string
  shippingDistrict: string
  shippingFullAddress: string
  cargoTrackingNumber?: string
  cargoProvider?: string
  cancelReason?: string
  items: OrderItem[]
  createdAt: string
  shippedAt?: string
}

export type CargoStatus =
  | 'CREATED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'FAILED'

export interface TrackingEvent {
  status: CargoStatus
  description: string
  location: string
  timestamp: string
}

export interface TrackingInfo {
  trackingNumber: string
  orderNumber: string
  provider: string
  currentStatus: CargoStatus
  recipientName: string
  city: string
  estimatedDelivery: string
  deliveredAt?: string
  events: TrackingEvent[]
}

export interface Stock {
  productId: number
  quantity: number
  reservedQty: number
  availableQty: number
  lowStockThreshold: number
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  errorCode?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
