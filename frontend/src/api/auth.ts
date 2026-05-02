import client from './client'
import type { ApiResponse, User } from '@/types'

export interface LoginRequest { email: string; password: string }
export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  confirmPassword: string
}
export interface LoginResponse { accessToken: string; refreshToken?: string; user: User }

export const authApi = {

  login: (data: LoginRequest) =>
    client.post<ApiResponse<LoginResponse>>('/auth/login', data).then(r => r.data.data),

  register: (data: RegisterRequest) =>
    client.post<ApiResponse<LoginResponse>>('/auth/register', data).then(r => r.data.data),

  verifyEmail: (token: string) =>
    client.get<ApiResponse<void>>('/auth/verify-email', { params: { token } }).then(r => r.data),

  forgotPassword: (email: string) =>
    client.post<ApiResponse<void>>('/auth/forgot-password', { email }).then(r => r.data),

  resetPassword: (token: string, newPassword: string) =>
    client.post<ApiResponse<void>>('/auth/reset-password', { token, newPassword }).then(r => r.data),

  checkEmail: (email: string) =>
    client.get<ApiResponse<{ exists: boolean }>>('/auth/check-email', { params: { email } })
      .then(r => r.data.data),

  resendVerification: (email: string) =>
    client.post<ApiResponse<void>>('/auth/resend-verification', { email }).then(r => r.data),

  refreshToken: (refreshToken: string) =>
    client.post<ApiResponse<LoginResponse>>('/auth/refresh-token', { refreshToken }).then(r => r.data.data),


  getMe: () =>
    client.get<ApiResponse<User>>('/users/me').then(r => r.data.data),

  updateProfile: (data: { firstName: string; lastName: string; phone?: string }) =>
    client.put<ApiResponse<User>>('/users/me', data).then(r => r.data.data),

  changePassword: (data: { currentPassword: string; newPassword: string; confirmNewPassword: string }) =>
    client.patch<ApiResponse<void>>('/users/me/password', data).then(r => r.data),

  getAddresses: () =>
    client.get<ApiResponse<Address[]>>('/users/me/addresses').then(r => r.data.data),

  addAddress: (data: AddressRequest) =>
    client.post<ApiResponse<Address>>('/users/me/addresses', data).then(r => r.data.data),

  deleteAddress: (id: number) =>
    client.delete<ApiResponse<void>>(`/users/me/addresses/${id}`).then(r => r.data),
}

export interface Address {
  id: number
  title: string
  firstName: string
  lastName: string
  phone: string
  city: string
  district: string
  fullAddress: string
  defaultAddress: boolean
}

export interface AddressRequest {
  title: string
  firstName: string
  lastName: string
  phone: string
  city: string
  district: string
  fullAddress: string
  defaultAddress?: boolean
}
