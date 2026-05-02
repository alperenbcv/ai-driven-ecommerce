import { useQuery, useMutation } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { Star, ShoppingCart, Package, AlertTriangle, CheckCircle2, XCircle, Brain, ChevronLeft, ChevronRight } from 'lucide-react'
import { toast } from 'sonner'
import { productsApi } from '@/api/products'
import { cartApi } from '@/api/cart'
import { aiApi } from '@/api/ai'
import client from '@/api/client'
import { formatPrice } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useCartStore } from '@/stores/cartStore'
import { useAuthStore, useIsAuthenticated } from '@/stores/authStore'
import { useState, useEffect } from 'react'
import type { Product, ApiResponse } from '@/types'
import { ProductCard } from '@/components/ProductCard'

interface StockInfo {
  productId: number
  sellerId?: number
  quantity: number
  reservedQty: number
  availableQty: number
  lowStockThreshold: number
}

function StarRating({ rating }: { rating: number }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((s) => (
        <Star key={s} className={`h-4 w-4 ${s <= rating ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`} />
      ))}
    </div>
  )
}

function ImageGallery({ images, name }: { images: { id: number; url: string; displayOrder: number }[]; name: string }) {
  const sorted = [...images].sort((a, b) => a.displayOrder - b.displayOrder)
  const [activeIdx, setActiveIdx] = useState(0)

  const prev = () => setActiveIdx(i => (i - 1 + sorted.length) % sorted.length)
  const next = () => setActiveIdx(i => (i + 1) % sorted.length)

  if (sorted.length === 0) {
    return (
      <div className="flex h-full w-full items-center justify-center bg-[radial-gradient(circle_at_30%_20%,#ccfbf1,transparent_32%),linear-gradient(135deg,#f8fafc,#e2e8f0)] rounded-[1.5rem]">
        <Package className="h-20 w-20 text-slate-400" />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {/* Ana görsel */}
      <div className="relative aspect-square overflow-hidden rounded-[1.5rem] bg-slate-100 group">
        <img
          src={sorted[activeIdx].url}
          alt={`${name} - ${activeIdx + 1}`}
          className="w-full h-full object-cover transition-opacity duration-200"
          onError={e => { (e.target as HTMLImageElement).src = `https://picsum.photos/seed/product-${activeIdx}/800/600` }}
        />
        {sorted.length > 1 && (
          <>
            <button
              onClick={prev}
              className="absolute left-2 top-1/2 -translate-y-1/2 h-8 w-8 rounded-full bg-white/80 shadow flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-white"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              onClick={next}
              className="absolute right-2 top-1/2 -translate-y-1/2 h-8 w-8 rounded-full bg-white/80 shadow flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-white"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
              {sorted.map((_, i) => (
                <button
                  key={i}
                  onClick={() => setActiveIdx(i)}
                  className={`h-1.5 rounded-full transition-all ${i === activeIdx ? 'w-4 bg-teal-500' : 'w-1.5 bg-white/60'}`}
                />
              ))}
            </div>
          </>
        )}
      </div>

      {/* Thumbnail şeridi */}
      {sorted.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {sorted.map((img, i) => (
            <button
              key={img.id}
              onClick={() => setActiveIdx(i)}
              className={`flex-shrink-0 h-16 w-16 rounded-xl overflow-hidden border-2 transition-all ${
                i === activeIdx ? 'border-teal-500 scale-105' : 'border-transparent opacity-60 hover:opacity-100'
              }`}
            >
              <img
                src={img.url}
                alt={`${name} thumbnail ${i + 1}`}
                className="w-full h-full object-cover"
                onError={e => { (e.target as HTMLImageElement).src = `https://picsum.photos/seed/thumb-${i}/80/80` }}
              />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function useSellerStock(productId: string | undefined, sellerId: number | undefined) {
  return useQuery<StockInfo>({
    queryKey: ['stock', productId, sellerId],
    queryFn: () =>
      client.get<ApiResponse<StockInfo>>(
        sellerId ? `/stocks/product/${productId}/seller/${sellerId}` : `/stocks/product/${productId}`
      ).then(r => r.data.data),
    enabled: !!productId && sellerId !== undefined,
    retry: false,
    staleTime: 30_000,
  })
}

function StockBadge({ stock }: { stock?: StockInfo }) {
  if (!stock) return null
  if (stock.availableQty <= 0)
    return <span className="text-xs text-red-600 font-medium flex items-center gap-1"><XCircle className="h-3 w-3" />Stokta yok</span>
  if (stock.availableQty <= stock.lowStockThreshold)
    return <span className="text-xs text-amber-600 font-medium flex items-center gap-1"><AlertTriangle className="h-3 w-3" />Son {stock.availableQty} adet</span>
  return <span className="text-xs text-green-600 font-medium flex items-center gap-1"><CheckCircle2 className="h-3 w-3" />{stock.availableQty} adet</span>
}

function SellerListingCard({
  listing, isActive, onClick, productId,
}: {
  listing: any
  isActive: boolean
  onClick: () => void
  productId: string | undefined
}) {
  const { data: stock } = useSellerStock(productId, listing.sellerId)

  return (
    <button
      onClick={onClick}
      className={`flex w-full items-center justify-between rounded-2xl border p-3 text-sm transition-colors ${
        isActive ? 'border-teal-300 bg-teal-50' : 'border-slate-200 bg-white hover:bg-slate-50'
      }`}
    >
      <div className="text-left">
        <span className="font-medium text-slate-700 block">{(listing as any).sellerName ?? `Satıcı #${listing.sellerId}`}</span>
        <StockBadge stock={stock} />
      </div>
      <span className="font-black text-teal-700 text-base ml-4 shrink-0">{formatPrice(listing.price)}</span>
    </button>
  )
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { setCart } = useCartStore()
  const isAuth = useIsAuthenticated()
  const userId = useAuthStore(s => s.user?.id)
  const [selectedListingId, setSelectedListingId] = useState<number | null>(null)

  const { data: product, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.getById(Number(id)),
  })

  useEffect(() => {
    if (!id || !userId || !product) return
    aiApi.trackView(Number(id), product.name, product.category?.name)
  }, [id, userId, product?.name])

  const { data: listings } = useQuery({
    queryKey: ['listings', id],
    queryFn: () => productsApi.getListings(Number(id)),
    enabled: !!id,
  })

  const { data: reviews } = useQuery({
    queryKey: ['reviews', id],
    queryFn: () => productsApi.getReviews(Number(id)),
    enabled: !!id,
  })

  const { data: similarIds } = useQuery({
    queryKey: ['similar', id],
    queryFn: () => aiApi.getProductRecommendations(Number(id), 4),
    enabled: !!id,
  })

  const { data: similarProducts } = useQuery({
    queryKey: ['products', 'similar', similarIds],
    queryFn: async () => {
      if (!similarIds?.length) return []
      const r = await Promise.allSettled(similarIds.map(i => productsApi.getById(i)))
      return r.filter(x => x.status === 'fulfilled').map(x => (x as PromiseFulfilledResult<Product>).value)
    },
    enabled: !!similarIds?.length,
  })

  const cheapestListing = listings?.slice().sort((a, b) => a.price - b.price)[0]
  const activeListing = listings?.find(l => l.id === selectedListingId) ?? cheapestListing

  const { data: activeStock } = useSellerStock(id, activeListing?.sellerId)

  const addToCart = useMutation({
    mutationFn: (listing: { id: number; sellerId: number; price: number }) =>
      cartApi.addItem({
        productId: product!.id,
        listingId: listing.id,
        sellerId: listing.sellerId,
        productName: product!.name,
        unitPrice: listing.price,
        quantity: 1,
      }),
    onSuccess: (cart) => {
      setCart(cart)
      toast.success('Sepete eklendi!')
    },
    onError: () => toast.error('Sepete eklenemedi'),
  })

  if (isLoading) return (
    <div className="grid gap-8 md:grid-cols-2">
      <Skeleton className="aspect-square rounded-[2rem]" />
      <div className="space-y-4">
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="h-6 w-1/4" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    </div>
  )

  if (!product) return <div className="text-center py-20 text-muted-foreground">Ürün bulunamadı</div>

  const images = product.images ?? []

  return (
    <div className="space-y-10">
      <div className="grid gap-8 lg:grid-cols-[1fr_.9fr]">
        {/* Görseller — galeri */}
        <div className="surface overflow-hidden rounded-[2rem] bg-slate-100 p-3">
          <ImageGallery images={images} name={product.name} />
        </div>

        {/* Bilgiler */}
        <div className="soft-panel space-y-5 rounded-[2rem] p-6">
          <div>
            {product.category?.name && <Badge className="bg-teal-50 text-teal-800 hover:bg-teal-50">{product.category.name}</Badge>}
            <h1 className="mt-3 text-3xl font-black leading-tight tracking-tight text-slate-950">{product.name}</h1>
            <p className="mt-2 font-medium text-slate-500">{product.brand?.name ?? 'n12 seçkisi'}</p>
          </div>

          {product.averageRating > 0 && (
            <div className="flex items-center gap-2">
              <StarRating rating={Math.round(product.averageRating)} />
              <span className="text-sm font-medium">{product.averageRating.toFixed(1)}</span>
              <span className="text-sm text-slate-500">({product.reviewCount} değerlendirme)</span>
            </div>
          )}

          <Separator />

          {/* Satıcı seçimi — her satıcının stoğu kendi kartında */}
          {listings && listings.length > 0 ? (
            <div className="space-y-2">
              <p className="text-sm font-bold text-slate-800">Satıcı & Stok Seçin</p>
              <div className="space-y-2">
                {listings.slice(0, 4).map(l => (
                  <SellerListingCard
                    key={l.id}
                    listing={l}
                    isActive={l.id === activeListing?.id}
                    onClick={() => setSelectedListingId(l.id)}
                    productId={id}
                  />
                ))}
              </div>
            </div>
          ) : (
            <div className="text-4xl font-black text-teal-700">{formatPrice(product.price)}</div>
          )}

          {/* Seçili satıcı stok durumu — büyük gösterim */}
          {activeListing && activeStock != null && (
            <div className={`flex items-center gap-2 text-sm rounded-2xl px-4 py-3 ${
              activeStock.availableQty <= 0
                ? 'bg-red-50 text-red-700'
                : activeStock.availableQty <= activeStock.lowStockThreshold
                ? 'bg-amber-50 text-amber-700'
                : 'bg-green-50 text-green-700'
            }`}>
              {activeStock.availableQty <= 0 ? (
                <><XCircle className="h-4 w-4" /><span className="font-semibold">Stokta Yok</span></>
              ) : activeStock.availableQty <= activeStock.lowStockThreshold ? (
                <><AlertTriangle className="h-4 w-4" /><span className="font-semibold">Son {activeStock.availableQty} adet kaldı!</span></>
              ) : (
                <><CheckCircle2 className="h-4 w-4" /><span className="font-semibold">Stokta Var</span><span className="text-sm opacity-70">— {activeStock.availableQty} adet mevcut</span></>
              )}
            </div>
          )}

          <Button
            size="lg"
            className="h-12 w-full rounded-2xl"
            disabled={!isAuth || !activeListing || addToCart.isPending || (activeStock != null && activeStock.availableQty <= 0)}
            onClick={() => activeListing && addToCart.mutate({ id: activeListing.id, sellerId: activeListing.sellerId, price: activeListing.price })}
          >
            <ShoppingCart className="h-5 w-5 mr-2" />
            {!isAuth
              ? 'Giriş Yaparak Sepete Ekle'
              : activeStock != null && activeStock.availableQty <= 0
              ? 'Stokta Yok'
              : addToCart.isPending
              ? 'Ekleniyor...'
              : 'Sepete Ekle'}
          </Button>

          <div className="flex items-center gap-2 rounded-2xl bg-slate-50 p-3 text-sm font-medium text-slate-600">
            <Package className="h-4 w-4" />
            <span>Ücretsiz kargo • 3-5 iş günü teslimat</span>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="desc" className="surface rounded-[2rem] p-4">
        <TabsList className="rounded-2xl">
          <TabsTrigger value="desc">Açıklama</TabsTrigger>
          <TabsTrigger value="reviews">Değerlendirmeler ({reviews?.length ?? 0})</TabsTrigger>
        </TabsList>
        <TabsContent value="desc" className="mt-4">
          <p className="max-w-4xl leading-7 text-slate-600">{product.description || 'Açıklama bulunmamaktadır.'}</p>
        </TabsContent>
        <TabsContent value="reviews" className="mt-4 space-y-4">
          {reviews?.length === 0 ? (
            <p className="text-muted-foreground">Henüz değerlendirme yapılmamış.</p>
          ) : (
            reviews?.map(r => (
              <Card key={r.id}>
                <CardContent className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <span className="font-medium text-sm">{r.userName}</span>
                      {r.verifiedPurchase && <Badge variant="secondary" className="ml-2 text-xs">Doğrulanmış Alım</Badge>}
                    </div>
                    <StarRating rating={r.rating} />
                  </div>
                  <p className="font-medium text-sm">{r.title}</p>
                  <p className="text-sm text-muted-foreground mt-1">{r.body}</p>
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>
      </Tabs>

      {/* Benzer ürünler — Neo4j collaborative filtering */}
      {similarProducts && similarProducts.length > 0 && (
        <section className="mt-8 p-6 bg-gradient-to-br from-purple-50 to-blue-50 rounded-2xl border border-purple-100">
          <div className="flex items-center gap-2 mb-5">
            <div className="h-8 w-8 rounded-full bg-purple-600 flex items-center justify-center">
              <Brain className="h-4 w-4 text-white" />
            </div>
            <div>
              <h2 className="text-base font-bold text-gray-900">Bunu Alanlar Bunu da Aldı</h2>
              <p className="text-xs text-purple-600">Neo4j Graf Tabanlı AI Öneri</p>
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-4">
            {similarProducts.map(p => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
