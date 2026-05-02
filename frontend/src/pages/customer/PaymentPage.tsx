import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { CreditCard, CheckCircle2, XCircle, Loader2, AlertTriangle, FlaskConical } from 'lucide-react'
import { toast } from 'sonner'
import client from '@/api/client'
import type { ApiResponse } from '@/types'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

interface PaymentStatus {
  orderNumber: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
  amount: number
  paymentId?: string
  failureReason?: string
  checkoutFormContent?: string
}

const paymentsApi = {
  getStatus: (orderNumber: string) =>
    client.get<ApiResponse<PaymentStatus>>(`/payments/orders/${orderNumber}`).then(r => r.data.data),
  simulateSuccess: (orderNumber: string) =>
    client.post<ApiResponse<string>>(`/payments/orders/${orderNumber}/simulate-success`).then(r => r.data),
}

export function PaymentPage() {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const navigate = useNavigate()
  const formContainerRef = useRef<HTMLDivElement>(null)
  const [formInjected, setFormInjected] = useState(false)
  const [pollCount, setPollCount] = useState(0)

  // Ödeme durumunu polling ile kontrol et
  const { data: payment, refetch } = useQuery({
    queryKey: ['payment', orderNumber],
    queryFn: () => paymentsApi.getStatus(orderNumber!),
    enabled: !!orderNumber,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      if (status === 'SUCCESS' || status === 'FAILED' || status === 'CANCELLED') return false
      return 3000
    },
    retry: 3,
  })

  const simulate = useMutation({
    mutationFn: () => paymentsApi.simulateSuccess(orderNumber!),
    onSuccess: () => {
      toast.success('Ödeme simüle edildi!')
      setTimeout(() => refetch(), 1500)
    },
    onError: () => toast.error('Simülasyon başarısız'),
  })

  useEffect(() => {
    if (!payment?.checkoutFormContent || formInjected || !formContainerRef.current) return

    const container = formContainerRef.current
    container.innerHTML = payment.checkoutFormContent

    container.querySelectorAll('script').forEach(oldScript => {
      const newScript = document.createElement('script')
      Array.from(oldScript.attributes).forEach(attr =>
        newScript.setAttribute(attr.name, attr.value)
      )
      newScript.textContent = oldScript.textContent
      oldScript.parentNode?.replaceChild(newScript, oldScript)
    })

    setFormInjected(true)
  }, [payment?.checkoutFormContent, formInjected])

  useEffect(() => {
    if (payment?.status === 'SUCCESS') {
      setTimeout(() => navigate(`/orders/${orderNumber}`), 2000)
    }
  }, [payment?.status])

  useEffect(() => {
    if (payment?.status === 'PENDING') {
      const id = setTimeout(() => setPollCount(c => c + 1), 3000)
      return () => clearTimeout(id)
    }
  }, [payment?.status, pollCount])

  if (!payment && !orderNumber) return null

  if (payment?.status === 'SUCCESS') {
    return (
      <div className="max-w-md mx-auto pt-16 text-center space-y-4">
        <div className="h-20 w-20 rounded-full bg-green-100 flex items-center justify-center mx-auto">
          <CheckCircle2 className="h-10 w-10 text-green-600" />
        </div>
        <h1 className="text-2xl font-bold text-green-700">Ödeme Başarılı!</h1>
        <p className="text-muted-foreground">
          Siparişiniz onaylandı. Sipariş detayına yönlendiriliyorsunuz...
        </p>
        <Badge variant="secondary">Sipariş: {payment.orderNumber}</Badge>
        <div className="pt-2">
          <Button onClick={() => navigate(`/orders/${orderNumber}`)}>
            Siparişimi Görüntüle
          </Button>
        </div>
      </div>
    )
  }

  if (payment?.status === 'FAILED' || payment?.status === 'CANCELLED') {
    return (
      <div className="max-w-md mx-auto pt-16 text-center space-y-4">
        <div className="h-20 w-20 rounded-full bg-red-100 flex items-center justify-center mx-auto">
          <XCircle className="h-10 w-10 text-red-600" />
        </div>
        <h1 className="text-2xl font-bold text-red-700">Ödeme Başarısız</h1>
        {payment.failureReason && (
          <p className="text-muted-foreground">{payment.failureReason}</p>
        )}
        <div className="flex gap-3 justify-center pt-2">
          <Button variant="outline" onClick={() => navigate('/cart')}>
            Sepete Dön
          </Button>
          <Button onClick={() => navigate('/checkout')}>
            Tekrar Dene
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
          <CreditCard className="h-5 w-5 text-teal-700" />
        </div>
        <div>
          <h1 className="text-xl font-bold">Güvenli Ödeme</h1>
          <p className="text-sm text-muted-foreground">Sipariş: {orderNumber}</p>
        </div>
      </div>

      {/* Iyzico form alanı */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            Iyzico Ödeme Formu
            <Badge variant="secondary" className="text-xs font-normal">Sandbox</Badge>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {!payment?.checkoutFormContent ? (
            // Form henüz hazır değil — bekliyor
            <div className="flex flex-col items-center gap-3 py-8 text-muted-foreground">
              <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
              <p className="text-sm">Ödeme formu hazırlanıyor...</p>
              <p className="text-xs">
                {pollCount > 5 && 'Iyzico sandbox erişimi yavaş olabilir.'}
              </p>
            </div>
          ) : (
            // Iyzico form HTML enjekte edilir
            <div ref={formContainerRef} className="min-h-[200px]" />
          )}

          {/* Sandbox uyarısı + simüle butonu */}
          <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-amber-700">
              <AlertTriangle className="h-4 w-4" />
              Sandbox Ortamı
            </div>
            <p className="text-xs text-amber-600">
              Iyzico callback'i localhost'a ulaşamaz. Aşağıdaki butonu kullanarak
              başarılı ödemeyi simüle edebilirsiniz.
            </p>
            <Button
              variant="outline"
              size="sm"
              className="border-amber-300 text-amber-700 hover:bg-amber-100 gap-2"
              onClick={() => simulate.mutate()}
              disabled={simulate.isPending}
            >
              <FlaskConical className="h-3.5 w-3.5" />
              {simulate.isPending ? 'Simüle ediliyor...' : 'Ödemeyi Simüle Et (Sandbox)'}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
