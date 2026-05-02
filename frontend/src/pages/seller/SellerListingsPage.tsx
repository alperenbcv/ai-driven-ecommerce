import { useState, useRef, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { productsApi } from '@/api/products'
import { aiApi } from '@/api/ai'
import client from '@/api/client'
import type { ApiResponse } from '@/types'
import {
  Plus, Search, Package, Edit2,
  ToggleLeft, ToggleRight, ChevronUp, ImagePlus, X, Loader2,
  ArrowUp, ArrowDown, Trash2, Sparkles,
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { formatPrice } from '@/lib/utils'
import type { ProductListing } from '@/types'
import { useAuthStore } from '@/stores/authStore'

interface CatalogProduct {
  id: number
  name: string
  description?: string
  categoryName?: string
  brandName?: string
  price: number
}

interface PendingImage { file: File; preview: string; displayOrder: number }

function AddListingModal({ product, onClose, onSuccess }: {
  product: CatalogProduct
  onClose: () => void
  onSuccess: () => void
}) {
  const [price, setPrice] = useState('')
  const [stock, setStock] = useState('10')
  const [images, setImages] = useState<PendingImage[]>([])
  const fileRef = useRef<HTMLInputElement>(null)

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? [])
    const next = files.map((f, i) => ({
      file: f,
      preview: URL.createObjectURL(f),
      displayOrder: images.length + i,
    }))
    setImages(prev => [...prev, ...next])
    e.target.value = ''
  }

  const removeImage = (idx: number) => {
    setImages(prev => prev.filter((_, i) => i !== idx).map((img, i) => ({ ...img, displayOrder: i })))
  }

  const create = useMutation({
    mutationFn: async () => {
      await productsApi.createListing(product.id, { price: Number(price) })
      const sellerId = useAuthStore.getState().user?.id
      if (sellerId == null) throw new Error('Satıcı kimliği bulunamadı')
      await client.post<ApiResponse<unknown>>(`/stocks`, {
        productId: product.id,
        sellerId,
        initialQuantity: Number(stock),
        lowStockThreshold: 5,
      })
      for (const img of images) {
        const form = new FormData()
        form.append('file', img.file)
        form.append('displayOrder', String(img.displayOrder))
        await client.post(`/products/${product.id}/images`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      }
    },
    onSuccess: () => {
      toast.success('Listing oluşturuldu!')
      onSuccess()
      onClose()
    },
    onError: () => toast.error('Listing oluşturulamadı.'),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-5 max-h-[90vh] overflow-y-auto">
        <div>
          <h2 className="text-lg font-bold">Listing Ekle</h2>
          <p className="text-sm text-muted-foreground mt-0.5">{product.name}</p>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label>Satış Fiyatı (TL) *</Label>
            <Input type="number" min="0.01" step="0.01" placeholder="0.00"
              value={price} onChange={e => setPrice(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label>Başlangıç Stok</Label>
            <Input type="number" min="0" placeholder="10"
              value={stock} onChange={e => setStock(e.target.value)} />
          </div>
        </div>

        {/* Görsel Yükleme */}
        <div className="space-y-2">
          <Label>Ürün Görselleri</Label>
          <div className="flex flex-wrap gap-2">
            {images.map((img, idx) => (
              <div key={idx} className="relative group">
                <img src={img.preview} alt="" className="h-20 w-20 rounded-lg object-cover border" />
                <div className="absolute -top-1 -right-1 flex gap-0.5">
                  <button
                    onClick={() => removeImage(idx)}
                    className="bg-red-500 text-white rounded-full h-5 w-5 flex items-center justify-center shadow"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
                <div className="absolute bottom-0 left-0 right-0 bg-black/50 rounded-b-lg px-1 py-0.5">
                  <input
                    type="number" min="0" className="w-full bg-transparent text-white text-xs text-center outline-none"
                    value={img.displayOrder}
                    onChange={e => setImages(prev => prev.map((im, i) =>
                      i === idx ? { ...im, displayOrder: Number(e.target.value) } : im
                    ))}
                  />
                </div>
              </div>
            ))}
            <button
              onClick={() => fileRef.current?.click()}
              className="h-20 w-20 rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center gap-1 hover:border-blue-400 hover:bg-blue-50 transition-colors"
            >
              <ImagePlus className="h-6 w-6 text-gray-400" />
              <span className="text-[10px] text-gray-400">Ekle</span>
            </button>
            <input ref={fileRef} type="file" accept="image/*" multiple className="hidden" onChange={handleFileChange} />
          </div>
          <p className="text-xs text-muted-foreground">Görselin altındaki sayı sırasını belirtir (0 = ana görsel).</p>
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <Button variant="outline" onClick={onClose}>İptal</Button>
          <Button
            disabled={!price || Number(price) <= 0 || create.isPending}
            onClick={() => create.mutate()}
          >
            {create.isPending ? <><Loader2 className="h-4 w-4 animate-spin mr-2" />Oluşturuluyor...</> : 'Listing Oluştur'}
          </Button>
        </div>
      </div>
    </div>
  )
}

function ImageManagerSection({ productId, productName, categoryName }: {
  productId: number
  productName?: string
  categoryName?: string
}) {
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [pendingFiles, setPendingFiles] = useState<{ file: File; preview: string }[]>([])

  // Mevcut görselleri product sorgusuyla al
  const { data: product, isLoading } = useQuery({
    queryKey: ['product', 'detail', productId],
    queryFn: () => productsApi.getById(productId),
    staleTime: 5_000,
  })

  const existingImages = [...(product?.images ?? [])].sort((a, b) => a.displayOrder - b.displayOrder)

  const deleteImg = useMutation({
    mutationFn: (imageId: number) =>
      client.delete<ApiResponse<void>>(`/products/${productId}/images/${imageId}`),
    onSuccess: () => { toast.success('Görsel silindi'); qc.invalidateQueries({ queryKey: ['product', 'detail', productId] }) },
    onError: () => toast.error('Görsel silinemedi'),
  })

  const reorderImg = useMutation({
    mutationFn: ({ imageId, displayOrder }: { imageId: number; displayOrder: number }) =>
      client.patch<ApiResponse<void>>(`/products/${productId}/images/${imageId}?displayOrder=${displayOrder}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['product', 'detail', productId] }),
    onError: () => toast.error('Sıra güncellenemedi'),
  })

  const moveUp = (img: { id: number; displayOrder: number }, idx: number) => {
    if (idx === 0) return
    const prev = existingImages[idx - 1]
    reorderImg.mutate({ imageId: img.id, displayOrder: prev.displayOrder })
    reorderImg.mutate({ imageId: prev.id, displayOrder: img.displayOrder })
  }

  const moveDown = (img: { id: number; displayOrder: number }, idx: number) => {
    if (idx === existingImages.length - 1) return
    const next = existingImages[idx + 1]
    reorderImg.mutate({ imageId: img.id, displayOrder: next.displayOrder })
    reorderImg.mutate({ imageId: next.id, displayOrder: img.displayOrder })
  }

  const generateAiImage = useMutation({
    mutationFn: async () => {
      const url = await aiApi.generateProductImage(productName ?? 'product', undefined, categoryName)
      await productsApi.addImageFromUrl(productId, url, existingImages.length + 1)
    },
    onSuccess: () => {
      toast.success('AI görsel oluşturuldu ve eklendi!')
      qc.invalidateQueries({ queryKey: ['product', 'detail', productId] })
      qc.invalidateQueries({ queryKey: ['product', String(productId)] })
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? 'AI görsel üretilemedi'),
  })

  const uploadPending = useMutation({
    mutationFn: async () => {
      const nextOrder = existingImages.length
      for (let i = 0; i < pendingFiles.length; i++) {
        const form = new FormData()
        form.append('file', pendingFiles[i].file)
        form.append('displayOrder', String(nextOrder + i + 1))
        await client.post(`/products/${productId}/images`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      }
    },
    onSuccess: () => {
      toast.success(`${pendingFiles.length} görsel yüklendi`)
      setPendingFiles([])
      qc.invalidateQueries({ queryKey: ['product', 'detail', productId] })
      qc.invalidateQueries({ queryKey: ['product', String(productId)] })
    },
    onError: () => toast.error('Görsel yüklenemedi'),
  })

  const handleFiles = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? [])
    setPendingFiles(prev => [...prev, ...files.map(f => ({ file: f, preview: URL.createObjectURL(f) }))])
    e.target.value = ''
  }

  return (
    <div className="space-y-3">
      <Label className="text-xs font-semibold">Ürün Görselleri</Label>

      {/* Mevcut görseller */}
      {isLoading ? (
        <div className="text-xs text-muted-foreground">Yükleniyor...</div>
      ) : existingImages.length > 0 ? (
        <div className="space-y-1.5">
          {existingImages.map((img, idx) => (
            <div key={img.id} className="flex items-center gap-2 p-2 rounded-lg border bg-white">
              <img src={img.url} alt="" className="h-12 w-12 rounded object-cover border flex-shrink-0"
                onError={e => { (e.target as HTMLImageElement).src = 'https://picsum.photos/seed/placeholder/80/80' }} />
              <div className="flex-1 min-w-0">
                <p className="text-xs font-medium truncate">{idx === 0 ? '⭐ Ana görsel' : `Görsel #${idx + 1}`}</p>
                <p className="text-[10px] text-muted-foreground">Sıra: {img.displayOrder}</p>
              </div>
              <div className="flex items-center gap-1 flex-shrink-0">
                <button
                  disabled={idx === 0 || reorderImg.isPending}
                  onClick={() => moveUp(img, idx)}
                  className="h-6 w-6 rounded flex items-center justify-center hover:bg-slate-100 disabled:opacity-30"
                >
                  <ArrowUp className="h-3 w-3" />
                </button>
                <button
                  disabled={idx === existingImages.length - 1 || reorderImg.isPending}
                  onClick={() => moveDown(img, idx)}
                  className="h-6 w-6 rounded flex items-center justify-center hover:bg-slate-100 disabled:opacity-30"
                >
                  <ArrowDown className="h-3 w-3" />
                </button>
                <button
                  onClick={() => deleteImg.mutate(img.id)}
                  disabled={deleteImg.isPending}
                  className="h-6 w-6 rounded flex items-center justify-center text-red-500 hover:bg-red-50 disabled:opacity-30"
                >
                  <Trash2 className="h-3 w-3" />
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-xs text-muted-foreground">Henüz görsel yok.</p>
      )}

      {/* Yeni görsel ekleme */}
      <div className="flex flex-wrap gap-2">
        {pendingFiles.map((pf, idx) => (
          <div key={idx} className="relative">
            <img src={pf.preview} alt="" className="h-14 w-14 rounded-lg object-cover border" />
            <button
              onClick={() => setPendingFiles(prev => prev.filter((_, i) => i !== idx))}
              className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full h-4 w-4 flex items-center justify-center"
            >
              <X className="h-2.5 w-2.5" />
            </button>
          </div>
        ))}
        <button
          onClick={() => fileRef.current?.click()}
          className="h-14 w-14 rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center gap-0.5 hover:border-blue-400 hover:bg-blue-50 transition-colors"
        >
          <ImagePlus className="h-4 w-4 text-gray-400" />
          <span className="text-[9px] text-gray-400">Ekle</span>
        </button>
        <input ref={fileRef} type="file" accept="image/*" multiple className="hidden" onChange={handleFiles} />
      </div>

      {pendingFiles.length > 0 && (
        <Button
          size="sm" className="h-8 text-xs w-full"
          disabled={uploadPending.isPending}
          onClick={() => uploadPending.mutate()}
        >
          {uploadPending.isPending
            ? <><Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />Yükleniyor...</>
            : <><ImagePlus className="h-3.5 w-3.5 mr-1" />{pendingFiles.length} görseli yükle</>}
        </Button>
      )}

      {/* AI Görsel Üret */}
      <Button
        size="sm" variant="outline"
        className="h-8 text-xs w-full gap-1.5 border-purple-200 text-purple-700 hover:bg-purple-50"
        disabled={generateAiImage.isPending}
        onClick={() => generateAiImage.mutate()}
      >
        {generateAiImage.isPending
          ? <><Loader2 className="h-3.5 w-3.5 animate-spin" />DALL-E 2 üretiyor (~10sn)...</>
          : <><Sparkles className="h-3.5 w-3.5" />AI ile Görsel Üret (DALL-E 2)</>}
      </Button>

      <p className="text-[10px] text-muted-foreground">İlk sıradaki (en küçük numara) görsel ürün detay sayfasında ana görsel olarak gösterilir.</p>
    </div>
  )
}

function EditListingRow({ listing, onDone }: { listing: ProductListing & { categoryName?: string }; onDone: () => void }) {
  const qc = useQueryClient()
  const [price, setPrice] = useState(String(listing.price))
  const [stock, setStock] = useState('')

  const updatePrice = useMutation({
    mutationFn: () => productsApi.updateListingPrice(listing.productId, { price: Number(price) }),
    onSuccess: () => { toast.success('Fiyat güncellendi'); qc.invalidateQueries({ queryKey: ['seller', 'listings'] }); onDone() },
    onError: () => toast.error('Fiyat güncellenemedi'),
  })

  const adjustStock = useMutation({
    mutationFn: () =>
      client.patch<ApiResponse<unknown>>(
        `/stocks/product/${listing.productId}/adjust`,
        { quantity: Number(stock), reason: 'Satıcı stok güncelleme' },
        { params: { sellerId: listing.sellerId } },
      ),
    onSuccess: () => { toast.success('Stok güncellendi'); setStock('') },
    onError: () => toast.error('Stok güncellenemedi'),
  })

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label className="text-xs">Yeni Fiyat (TL)</Label>
          <div className="flex gap-2">
            <Input
              type="number" min="0.01" step="0.01" className="h-8 text-sm"
              value={price} onChange={e => setPrice(e.target.value)}
            />
            <Button size="sm" className="h-8 text-xs" disabled={updatePrice.isPending}
              onClick={() => updatePrice.mutate()}>
              {updatePrice.isPending ? '...' : 'Güncelle'}
            </Button>
          </div>
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Stok Miktarı (mutlak)</Label>
          <div className="flex gap-2">
            <Input
              type="number" min="0" className="h-8 text-sm" placeholder="ör: 50"
              value={stock} onChange={e => setStock(e.target.value)}
            />
            <Button size="sm" variant="outline" className="h-8 text-xs"
              disabled={!stock || adjustStock.isPending}
              onClick={() => adjustStock.mutate()}>
              {adjustStock.isPending ? '...' : 'Ayarla'}
            </Button>
          </div>
        </div>
      </div>

      <ImageManagerSection
        productId={listing.productId}
        productName={listing.productName}
        categoryName={(listing as any).categoryName}
      />

      <button className="text-xs text-muted-foreground hover:underline" onClick={onDone}>Kapat</button>
    </div>
  )
}

export function SellerListingsPage() {
  const qc = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [keyword, setKeyword] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedProduct, setSelectedProduct] = useState<CatalogProduct | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [showSearch, setShowSearch] = useState(false)

  const addProductId = searchParams.get('addProduct')
  const { data: preloadedProduct } = useQuery({
    queryKey: ['product', 'detail', addProductId],
    queryFn: () => productsApi.getById(Number(addProductId)),
    enabled: !!addProductId,
  })
  useEffect(() => {
    if (preloadedProduct && addProductId) {
      setSelectedProduct({
        id: preloadedProduct.id,
        name: preloadedProduct.name,
        description: preloadedProduct.description,
        categoryName: preloadedProduct.category?.name,
        brandName: preloadedProduct.brand?.name,
        price: preloadedProduct.price,
      })
      setSearchParams({})
    }
  }, [preloadedProduct, addProductId, setSearchParams])

  const { data: listings, isLoading } = useQuery({
    queryKey: ['seller', 'listings'],
    queryFn: productsApi.getMyListings,
  })

  const { data: searchResults, isFetching: searching } = useQuery({
    queryKey: ['catalog', 'search', searchTerm],
    queryFn: () => productsApi.getAll({ keyword: searchTerm, page: 0, size: 10 }),
    enabled: searchTerm.length >= 2,
  })

  const deactivate = useMutation({
    mutationFn: (productId: number) => productsApi.deactivateListing(productId),
    onSuccess: () => { toast.success('Listing devre dışı bırakıldı'); qc.invalidateQueries({ queryKey: ['seller', 'listings'] }) },
    onError: () => toast.error('İşlem başarısız'),
  })

  const reactivate = useMutation({
    mutationFn: (productId: number) =>
      client.post<ApiResponse<unknown>>(`/products/${productId}/listings/activate`).then(r => r.data),
    onSuccess: () => { toast.success('Listing aktif edildi'); qc.invalidateQueries({ queryKey: ['seller', 'listings'] }) },
    onError: () => toast.error('İşlem başarısız'),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Listing'lerim</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Katalogdaki ürünlere kendi fiyatınızla listing ekleyin ve stok yönetin.
          </p>
        </div>
        <Button onClick={() => setShowSearch(v => !v)} className="gap-2">
          {showSearch ? <ChevronUp className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {showSearch ? 'Kapat' : 'Ürün Ekle'}
        </Button>
      </div>

      {/* Katalog Ürün Arama */}
      {showSearch && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Search className="h-4 w-4" /> Katalogdan Ürün Ara
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex gap-2">
              <Input
                placeholder="Ürün adı, kategori veya marka..."
                value={keyword}
                onChange={e => setKeyword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && setSearchTerm(keyword)}
              />
              <Button onClick={() => setSearchTerm(keyword)} disabled={keyword.length < 2}>
                Ara
              </Button>
            </div>

            {searching && <div className="text-sm text-muted-foreground">Aranıyor...</div>}

            {searchResults && searchResults.content.length === 0 && searchTerm && (
              <p className="text-sm text-muted-foreground text-center py-4">Sonuç bulunamadı.</p>
            )}

            {searchResults && searchResults.content.length > 0 && (
              <div className="space-y-2 max-h-80 overflow-y-auto">
                {searchResults.content.map((p: any) => (
                  <div key={p.id} className="flex items-center justify-between p-3 rounded-lg border hover:bg-gray-50">
                    <div>
                      <p className="font-medium text-sm">{p.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {p.categoryName ?? ''} {p.brandName ? `· ${p.brandName}` : ''} · {formatPrice(p.price)}
                      </p>
                    </div>
                    <Button
                      size="sm" variant="outline"
                      onClick={() => setSelectedProduct(p)}
                    >
                      <Plus className="h-3.5 w-3.5 mr-1" /> Listing Ekle
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Mevcut Listing'ler */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Package className="h-5 w-5" /> Aktif Listing'lerim
            {listings && (
              <Badge variant="secondary" className="ml-auto">
                {listings.filter((l: ProductListing) => l.active).length} aktif / {listings.length} toplam
              </Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {Array(4).fill(0).map((_, i) => <Skeleton key={i} className="h-16 rounded-lg" />)}
            </div>
          ) : !listings?.length ? (
            <div className="text-center py-10 text-muted-foreground">
              <Package className="h-10 w-10 mx-auto mb-3 opacity-30" />
              <p className="font-medium">Henüz listing oluşturmadınız</p>
              <p className="text-sm mt-1">Katalogdan ürün arayıp fiyatınızı girerek başlayın.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {listings.map((l: ProductListing) => (
                <div key={l.id}>
                  <div className={`flex items-center justify-between p-3 rounded-lg border transition-colors ${
                    !l.active ? 'opacity-60 bg-gray-50' : 'hover:bg-gray-50'
                  }`}>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <p className="font-medium text-sm">{l.productName}</p>
                        <Badge variant={l.active ? 'default' : 'secondary'} className="text-xs">
                          {l.active ? 'Aktif' : 'Pasif'}
                        </Badge>
                      </div>
                      <p className="text-xs text-muted-foreground mt-0.5">Ürün #{l.productId}</p>
                    </div>

                    <div className="flex items-center gap-2 ml-4 shrink-0">
                      <span className="font-bold text-blue-600 text-sm whitespace-nowrap">
                        {formatPrice(l.price)}
                      </span>
                      <Separator orientation="vertical" className="h-5" />
                      <Button
                        size="sm" variant="outline"
                        className="h-7 text-xs gap-1"
                        onClick={() => setEditingId(editingId === l.id ? null : l.id)}
                      >
                        <Edit2 className="h-3 w-3" />
                        Düzenle
                      </Button>
                      {l.active ? (
                        <Button
                          size="sm" variant="outline"
                          className="h-7 text-xs gap-1 text-red-600 border-red-200 hover:bg-red-50"
                          disabled={deactivate.isPending}
                          onClick={() => deactivate.mutate(l.productId)}
                        >
                          <ToggleRight className="h-3.5 w-3.5" />
                          Devre Dışı
                        </Button>
                      ) : (
                        <Button
                          size="sm" variant="outline"
                          className="h-7 text-xs gap-1 text-green-600 border-green-200 hover:bg-green-50"
                          disabled={reactivate.isPending}
                          onClick={() => reactivate.mutate(l.productId)}
                        >
                          <ToggleLeft className="h-3.5 w-3.5" />
                          Aktif Et
                        </Button>
                      )}
                    </div>
                  </div>

                  {/* Inline düzenleme formu */}
                  {editingId === l.id && (
                    <div className="mt-1">
                      <EditListingRow
                        listing={l}
                        onDone={() => setEditingId(null)}
                      />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Listing Ekleme Modalı */}
      {selectedProduct && (
        <AddListingModal
          product={selectedProduct}
          onClose={() => setSelectedProduct(null)}
          onSuccess={() => {
            qc.invalidateQueries({ queryKey: ['seller', 'listings'] })
            setShowSearch(false)
          }}
        />
      )}
    </div>
  )
}
