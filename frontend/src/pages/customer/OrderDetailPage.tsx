import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { Package, Truck, MapPin, XCircle } from 'lucide-react'
import { ordersApi } from '@/api/orders'
import { formatPrice, formatDate } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'
import type { CargoStatus, OrderStatus } from '@/types'

const orderProgress: Record<OrderStatus, number> = {
  PENDING: 10, STOCK_RESERVED: 20, PAYMENT_PROCESSING: 35,
  CONFIRMED: 50, SHIPPED: 75, DELIVERED: 100, CANCELLED: 0, REFUNDED: 0,
}

const cargoStatusLabel: Record<CargoStatus, string> = {
  CREATED: 'Hazırlanıyor', PICKED_UP: 'Kurye Teslim Aldı',
  IN_TRANSIT: 'Dağıtım Merkezinde', OUT_FOR_DELIVERY: 'Dağıtımda',
  DELIVERED: 'Teslim Edildi', FAILED: 'Teslim Edilemedi',
}

export function OrderDetailPage() {
  const { orderNumber } = useParams<{ orderNumber: string }>()
  const qc = useQueryClient()

  const { data: order, isLoading } = useQuery({
    queryKey: ['order', orderNumber],
    queryFn: () => ordersApi.getByNumber(orderNumber!),
    refetchInterval: 30000,
  })

  const { data: tracking } = useQuery({
    queryKey: ['tracking', orderNumber],
    queryFn: () => ordersApi.trackByOrder(orderNumber!),
    enabled: !!order?.cargoTrackingNumber,
    refetchInterval: 60000,
  })

  const cancel = useMutation({
    mutationFn: () => ordersApi.cancel(orderNumber!),
    onSuccess: () => {
      toast.success('Sipariş iptal edildi')
      qc.invalidateQueries({ queryKey: ['order', orderNumber] })
    },
    onError: () => toast.error('Sipariş iptal edilemedi'),
  })

  if (isLoading) return <div className="space-y-4">{Array(4).fill(0).map((_, i) => <Skeleton key={i} className="h-32 rounded-xl" />)}</div>
  if (!order) return <p className="text-center py-20 text-muted-foreground">Sipariş bulunamadı</p>

  const canCancel = ['PENDING', 'STOCK_RESERVED', 'PAYMENT_PROCESSING'].includes(order.status)
  const progress = orderProgress[order.status]

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      {/* Başlık */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">{order.orderNumber}</h1>
          <p className="text-muted-foreground text-sm">{formatDate(order.createdAt)}</p>
        </div>
        {canCancel && (
          <Button variant="destructive" size="sm" onClick={() => cancel.mutate()} disabled={cancel.isPending}>
            <XCircle className="h-4 w-4 mr-2" />
            {cancel.isPending ? 'İptal ediliyor...' : 'Siparişi İptal Et'}
          </Button>
        )}
      </div>

      {/* İlerleme */}
      {order.status !== 'CANCELLED' && order.status !== 'REFUNDED' && (
        <Card>
          <CardContent className="p-6">
            <div className="flex justify-between text-xs text-muted-foreground mb-2">
              <span>Sipariş Alındı</span>
              <span>Onaylandı</span>
              <span>Kargoda</span>
              <span>Teslim Edildi</span>
            </div>
            <Progress value={progress} className="h-2" />
          </CardContent>
        </Card>
      )}

      {order.status === 'CANCELLED' && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-4 flex items-center gap-3">
            <XCircle className="h-5 w-5 text-red-600" />
            <div>
              <p className="font-medium text-red-700">Sipariş İptal Edildi</p>
              {order.cancelReason && <p className="text-sm text-red-600">{order.cancelReason}</p>}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Kargo Takibi */}
      {tracking && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Truck className="h-5 w-5 text-teal-700" />
              Kargo Takibi — {tracking.trackingNumber}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {tracking.events.slice().reverse().map((event, i) => (
                <div key={i} className={`flex gap-3 ${i === 0 ? 'text-foreground' : 'text-muted-foreground'}`}>
                  <div className="flex flex-col items-center">
                    <div className={`h-3 w-3 rounded-full border-2 mt-1 ${i === 0 ? 'bg-blue-600 border-blue-600' : 'bg-white border-gray-300'}`} />
                    {i < tracking.events.length - 1 && <div className="w-px flex-1 bg-gray-200 mt-1" />}
                  </div>
                  <div className="pb-3">
                    <p className="text-sm font-medium">{cargoStatusLabel[event.status]}</p>
                    <p className="text-xs">{event.description}</p>
                    {event.location && <p className="text-xs opacity-70">📍 {event.location}</p>}
                    <p className="text-xs opacity-60 mt-0.5">{formatDate(event.timestamp)}</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Ürünler */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Package className="h-5 w-5" /> Ürünler
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {order.items.map(item => (
            <div key={item.id} className="flex justify-between items-center">
              <div>
                <p className="text-sm font-medium">{item.productName}</p>
                <p className="text-xs text-muted-foreground">{formatPrice(item.unitPrice)} × {item.quantity}</p>
              </div>
              <span className="font-medium">{formatPrice(item.totalPrice)}</span>
            </div>
          ))}
          <Separator />
          <div className="flex justify-between font-bold">
            <span>Toplam</span>
            <span className="text-teal-700">{formatPrice(order.totalAmount)}</span>
          </div>
        </CardContent>
      </Card>

      {/* Teslimat Adresi */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <MapPin className="h-5 w-5" /> Teslimat Adresi
          </CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          <p className="font-medium text-foreground">{order.shippingFullName}</p>
          <p>{order.shippingPhone}</p>
          <p>{order.shippingDistrict}, {order.shippingCity}</p>
          <p>{order.shippingFullAddress}</p>
        </CardContent>
      </Card>
    </div>
  )
}
