import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/queryClient'
import { Toaster } from '@/components/ui/sonner'
import { ErrorBoundary } from '@/components/ErrorBoundary'

import { CustomerLayout } from '@/layouts/CustomerLayout'
import { DashboardLayout } from '@/layouts/DashboardLayout'
import { ProtectedRoute } from '@/layouts/ProtectedRoute'

import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage'
import { EmailVerifyPage } from '@/pages/auth/EmailVerifyPage'
import { HomePage } from '@/pages/customer/HomePage'
import { ProductsPage } from '@/pages/customer/ProductsPage'
import { ProductDetailPage } from '@/pages/customer/ProductDetailPage'
import { CartPage } from '@/pages/customer/CartPage'
import { CheckoutPage } from '@/pages/customer/CheckoutPage'
import { PaymentPage } from '@/pages/customer/PaymentPage'
import { OrdersPage } from '@/pages/customer/OrdersPage'
import { OrderDetailPage } from '@/pages/customer/OrderDetailPage'
import { ChatPage } from '@/pages/customer/ChatPage'
import { ProfilePage } from '@/pages/customer/ProfilePage'

import { SellerDashboard } from '@/pages/seller/SellerDashboard'
import { SellerProposalsPage } from '@/pages/seller/SellerProposalsPage'
import { SellerListingsPage } from '@/pages/seller/SellerListingsPage'
import { AdminDashboard } from '@/pages/admin/AdminDashboard'

import {
  LayoutDashboard, Package, ClipboardList,
  Users, ShieldCheck, ClipboardCheck,
} from 'lucide-react'

export default function App() {
  return (
    <ErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Auth */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/verify-email" element={<EmailVerifyPage />} />

          {/* Müşteri */}
          <Route element={<CustomerLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/products/:id" element={<ProductDetailPage />} />
            <Route path="/chat" element={<ChatPage />} />

            <Route element={<ProtectedRoute />}>
              <Route path="/cart" element={<CartPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
              <Route path="/payment/:orderNumber" element={<PaymentPage />} />
              <Route path="/orders" element={<OrdersPage />} />
              <Route path="/orders/:orderNumber" element={<OrderDetailPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>
          </Route>

          {/* Seller Panel */}
          <Route element={<ProtectedRoute role="SELLER" />}>
            <Route
              element={
                <DashboardLayout
                  title="Satıcı Paneli"
                  items={[
                    { label: 'Dashboard', to: '/seller', icon: <LayoutDashboard className="h-4 w-4" /> },
                    { label: 'Listing\'lerim', to: '/seller/listings', icon: <Package className="h-4 w-4" /> },
                    { label: 'Ürün Önerileri', to: '/seller/proposals', icon: <ClipboardList className="h-4 w-4" /> },
                  ]}
                />
              }
            >
              <Route path="/seller" element={<SellerDashboard />} />
              <Route path="/seller/listings" element={<SellerListingsPage />} />
              <Route path="/seller/proposals" element={<SellerProposalsPage />} />
            </Route>
          </Route>

          {/* Admin Panel */}
          <Route element={<ProtectedRoute role="ADMIN" />}>
            <Route
              element={
                <DashboardLayout
                  title="Admin Paneli"
                  items={[
                    { label: 'Dashboard', to: '/admin', icon: <ShieldCheck className="h-4 w-4" /> },
                    { label: 'Kullanıcılar', to: '/admin/users', icon: <Users className="h-4 w-4" /> },
                    { label: 'Ürün Önerileri', to: '/admin/proposals', icon: <ClipboardCheck className="h-4 w-4" /> },
                  ]}
                />
              }
            >
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/users" element={<AdminDashboard />} />
              <Route path="/admin/proposals" element={<AdminDashboard />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
    </ErrorBoundary>
  )
}
