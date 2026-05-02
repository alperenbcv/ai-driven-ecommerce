import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ordersApi } from '@/api/orders'
import { formatPrice, formatDate } from '@/lib/utils'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Package } from 'lucide-react'
import type { OrderStatus } from '@/types'

const statusConfig: Record<OrderStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
  PENDING: { label: 'Bekliyor', variant: 'secondary' },
  STOCK_RESERVED: { label: 'Stok Ayrıldı', variant: 'secondary' },
  PAYMENT_PROCESSING: { label: 'Ödeme İşleniyor', variant: 'secondary' },
  CONFIRMED: { label: 'Onaylandı', variant: 'default' },
  SHIPPED: { label: 'Kargoda', variant: 'default' },
  DELIVERED: { label: 'Teslim Edildi', variant: 'default' },
  CANCELLED: { label: 'İptal Edildi', variant: 'destructive' },
  REFUNDED: { label: 'İade Edildi', variant: 'outline' },
}

export function OrdersPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['orders'],
    queryFn: () => ordersApi.getAll(),
  })

  if (isLoading) return <div className="space-y-4">{Array(3).fill(0).map((_, i) => <Skeleton key={i} className="h-32 rounded-xl" />)}</div>

  const orders = data?.content ?? []

  if (orders.length === 0) return (
    <div className="text-center py-20">
      <Package className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
      <h2 className="text-xl font-semibold mb-2">Henüz siparişiniz yok</h2>
      <Button asChild><Link to="/products">Alışverişe Başla</Link></Button>
    </div>
  )

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Siparişlerim</h1>
      {orders.map(order => {
        const cfg = statusConfig[order.status]
        return (
          <Card key={order.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-5">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <p className="font-bold">{order.orderNumber}</p>
                  <p className="text-sm text-muted-foreground">{formatDate(order.createdAt)}</p>
                </div>
                <Badge variant={cfg.variant}>{cfg.label}</Badge>
              </div>
              <div className="text-sm text-muted-foreground mb-3">
                {order.items.slice(0, 2).map(i => i.productName).join(', ')}
                {order.items.length > 2 && ` ve ${order.items.length - 2} ürün daha`}
              </div>
              <div className="flex items-center justify-between">
                <span className="font-black text-teal-700">{formatPrice(order.totalAmount)}</span>
                <Button variant="outline" size="sm" asChild>
                  <Link to={`/orders/${order.orderNumber}`}>Detay →</Link>
                </Button>
              </div>
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}
