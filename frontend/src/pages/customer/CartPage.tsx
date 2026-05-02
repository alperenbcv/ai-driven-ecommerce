import { useQuery, useMutation } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Minus, Package, Plus, Trash2, ShoppingBag } from 'lucide-react'
import { toast } from 'sonner'
import { cartApi } from '@/api/cart'
import { formatPrice } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { useCartStore } from '@/stores/cartStore'
import { useEffect } from 'react'

export function CartPage() {
  const { cart, setCart } = useCartStore()

  const { data, isLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: cartApi.getCart,
  })

  useEffect(() => { if (data) setCart(data) }, [data, setCart])

  const updateQty = useMutation({
    mutationFn: ({ productId, listingId, qty }: { productId: number; listingId: number; qty: number }) =>
      cartApi.updateQuantity(productId, listingId, qty),
    onSuccess: (newCart) => { setCart(newCart); toast.success('Sepet güncellendi') },
  })

  const remove = useMutation({
    mutationFn: ({ productId, listingId }: { productId: number; listingId: number }) =>
      cartApi.removeItem(productId, listingId),
    onSuccess: (newCart) => { setCart(newCart); toast.success('Ürün sepetten çıkarıldı') },
  })

  const items = cart?.items ?? data?.items ?? []

  if (isLoading) return <div className="space-y-4">{Array(3).fill(0).map((_, i) => <Skeleton key={i} className="h-24 rounded-xl" />)}</div>

  if (items.length === 0) return (
    <div className="surface mx-auto max-w-xl rounded-[2rem] py-20 text-center">
      <ShoppingBag className="mx-auto mb-4 h-16 w-16 text-teal-700" />
      <h2 className="mb-2 text-2xl font-black text-slate-950">Sepetiniz boş</h2>
      <p className="mb-6 text-slate-500">Alışverişe başlamak için ürün ekleyin</p>
      <Button asChild><Link to="/products">Ürünleri Keşfet</Link></Button>
    </div>
  )

  const total = cart?.totalAmount ?? data?.totalAmount ?? 0

  return (
    <div className="grid gap-8 lg:grid-cols-3">
      <div className="space-y-3 lg:col-span-2">
        <div className="mb-6">
          <p className="eyebrow">Sepet</p>
          <h1 className="text-3xl font-black tracking-tight text-slate-950">Sepetim ({items.length} ürün)</h1>
        </div>
        {items.map((item) => (
          <Card key={`${item.productId}-${item.listingId}`} className="overflow-hidden rounded-2xl">
            <CardContent className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
              <div className="flex h-20 w-20 flex-shrink-0 items-center justify-center rounded-2xl bg-slate-100">
                <Package className="h-8 w-8 text-slate-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="line-clamp-2 text-sm font-bold text-slate-900">{item.productName}</p>
                <p className="mt-1 text-sm font-black text-teal-700">{formatPrice(item.unitPrice)}</p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline" size="icon" className="h-8 w-8"
                  onClick={() => item.quantity === 1
                    ? remove.mutate({ productId: item.productId, listingId: item.listingId })
                    : updateQty.mutate({ productId: item.productId, listingId: item.listingId, qty: item.quantity - 1 })
                  }
                >
                  <Minus className="h-4 w-4" />
                </Button>
                <span className="w-8 text-center font-bold">{item.quantity}</span>
                <Button
                  variant="outline" size="icon" className="h-8 w-8"
                  onClick={() => updateQty.mutate({ productId: item.productId, listingId: item.listingId, qty: item.quantity + 1 })}
                >
                  <Plus className="h-4 w-4" />
                </Button>
              </div>
              <div className="text-right">
                <p className="font-bold">{formatPrice(item.unitPrice * item.quantity)}</p>
                <Button
                  variant="ghost" size="sm" className="text-red-500 hover:text-red-600 mt-1 h-7 px-2"
                  onClick={() => remove.mutate({ productId: item.productId, listingId: item.listingId })}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Özet */}
      <div>
        <Card className="sticky top-24 rounded-[2rem]">
          <CardContent className="p-6 space-y-4">
            <h2 className="text-lg font-black text-slate-950">Sipariş Özeti</h2>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Ara toplam</span>
              <span>{formatPrice(total)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Kargo</span>
              <span className="text-green-600">Ücretsiz</span>
            </div>
            <Separator />
            <div className="flex justify-between font-bold text-lg">
              <span>Toplam</span>
              <span className="text-teal-700">{formatPrice(total)}</span>
            </div>
            <Button asChild size="lg" className="w-full">
              <Link to="/checkout">Siparişi Tamamla</Link>
            </Button>
            <Button variant="outline" asChild className="w-full">
              <Link to="/products">Alışverişe Devam Et</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
