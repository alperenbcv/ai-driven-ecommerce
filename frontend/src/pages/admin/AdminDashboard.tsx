import { useState, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { productsApi } from '@/api/products'
import { adminApi } from '@/api/admin'
import { aiApi } from '@/api/ai'
import client from '@/api/client'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { toast } from 'sonner'
import { Users, ClipboardCheck, Package, Store, ImagePlus, X, WandSparkles, Pencil, Trash2, ChevronLeft, ChevronRight, Sparkles, Loader2 } from 'lucide-react'
import { formatPrice } from '@/lib/utils'
import { useLocation } from 'react-router-dom'

function adminDefaultTab(pathname: string) {
  if (pathname.endsWith('/users')) return 'users'
  if (pathname.endsWith('/proposals')) return 'proposals'
  return 'proposals'
}

function RevisionDialog({ onClose, onSubmit }: {
  onClose: () => void
  onSubmit: (note: string) => void
}) {
  const [note, setNote] = useState('')
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
        <h2 className="text-lg font-bold">Revizyon Notu</h2>
        <p className="text-sm text-muted-foreground">
          Satıcıya ne değiştirmesi gerektiğini açıklayın.
        </p>
        <div className="space-y-1.5">
          <Label>Admin Notu *</Label>
          <Textarea
            placeholder="ör. Lütfen ürün açıklamasını detaylandırın ve daha gerçekçi bir fiyat girin."
            rows={4}
            value={note}
            onChange={e => setNote(e.target.value)}
          />
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <Button variant="outline" onClick={onClose}>İptal</Button>
          <Button
            disabled={!note.trim()}
            className="bg-amber-500 hover:bg-amber-600"
            onClick={() => { onSubmit(note.trim()); onClose() }}
          >
            Revizyon İste
          </Button>
        </div>
      </div>
    </div>
  )
}

interface PendingImg { file: File; preview: string; displayOrder: number }

function ApproveDialog({ proposal, onClose, onSubmit }: {
  proposal: any
  onClose: () => void
  onSubmit: (existingProductId?: number, images?: PendingImg[], approvedDescription?: string) => void
}) {
  const [mode, setMode] = useState<'new' | 'existing'>('new')
  const [keyword, setKeyword] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [selectedName, setSelectedName] = useState('')
  const [images, setImages] = useState<PendingImg[]>([])
  const [description, setDescription] = useState(proposal.proposedDescription ?? '')
  const [generatedTags, setGeneratedTags] = useState<string[]>([])
  const [aiImgLoading, setAiImgLoading] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  const handleGenerateAiImage = async () => {
    setAiImgLoading(true)
    try {
      const url = await aiApi.generateProductImage(
        proposal.proposedName ?? 'product',
        description,
        proposal.categoryName
      )
      const res = await fetch(url)
      const blob = await res.blob()
      const file = new File([blob], `ai-${Date.now()}.png`, { type: 'image/png' })
      setImages(prev => [...prev, { file, preview: URL.createObjectURL(file), displayOrder: prev.length }])
      toast.success('AI görsel oluşturuldu ve eklendi!')
    } catch {
      toast.error('AI görsel üretilemedi')
    } finally {
      setAiImgLoading(false)
    }
  }

  const { data: results, isFetching } = useQuery({
    queryKey: ['catalog', 'search', 'admin', searchTerm],
    queryFn: () => productsApi.getAll({ keyword: searchTerm, page: 0, size: 8 }),
    enabled: searchTerm.length >= 2,
  })

  const handleFiles = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? [])
    setImages(prev => [
      ...prev,
      ...files.map((f, i) => ({ file: f, preview: URL.createObjectURL(f), displayOrder: prev.length + i })),
    ])
    e.target.value = ''
  }

  const generateDescription = useMutation({
    mutationFn: () => aiApi.generateProductDescription({
      productName: proposal.proposedName,
      categoryName: proposal.categoryName,
      brandName: proposal.brandName,
      currentDescription: description,
    }),
    onSuccess: data => {
      setDescription(data.description ?? '')
      setGeneratedTags(data.tags ?? [])
      toast.success('AI açıklaması hazır')
    },
    onError: () => toast.error('AI açıklaması üretilemedi'),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-5 max-h-[90vh] overflow-y-auto">
        <div>
          <h2 className="text-lg font-bold">Öneriyi Onayla</h2>
          <p className="text-sm text-muted-foreground mt-1">
            <span className="font-medium text-gray-800">{proposal.proposedName}</span> için nasıl devam edilsin?
          </p>
        </div>

        {/* Mod seçimi */}
        <div className="grid grid-cols-2 gap-3">
          <button
            onClick={() => { setMode('new'); setSelectedId(null) }}
            className={`p-4 rounded-lg border-2 text-left transition-colors ${
              mode === 'new' ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-gray-300'
            }`}
          >
            <p className="font-semibold text-sm">Yeni Ürün Oluştur</p>
            <p className="text-xs text-muted-foreground mt-1">
              Kataloga yeni kayıt açılır. İstersen hemen görsel de yükleyebilirsin.
            </p>
          </button>
          <button
            onClick={() => { setMode('existing'); setImages([]) }}
            className={`p-4 rounded-lg border-2 text-left transition-colors ${
              mode === 'existing' ? 'border-green-500 bg-green-50' : 'border-gray-200 hover:border-gray-300'
            }`}
          >
            <p className="font-semibold text-sm">Mevcut Ürüne Bağla</p>
            <p className="text-xs text-muted-foreground mt-1">
              Bu ürün zaten katalogda var. Yeni kayıt açılmaz.
            </p>
          </button>
        </div>

        {/* Yeni ürün: görsel yükleme */}
        {mode === 'new' && (
          <div className="space-y-4">
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-3">
                <Label className="text-xs">Ürün Açıklaması</Label>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  className="h-8 text-xs"
                  disabled={generateDescription.isPending}
                  onClick={() => generateDescription.mutate()}
                >
                  <WandSparkles className="h-3.5 w-3.5 mr-1" />
                  {generateDescription.isPending ? 'Üretiliyor...' : 'AI ile üret'}
                </Button>
              </div>
              <Textarea
                rows={5}
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="Ürün açıklaması..."
              />
              {generatedTags.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {generatedTags.map(tag => (
                    <Badge key={tag} variant="secondary" className="text-[10px]">
                      {tag}
                    </Badge>
                  ))}
                </div>
              )}
            </div>

            <div className="space-y-2">
            <Label className="text-xs">Ürün Görselleri <span className="text-muted-foreground">(opsiyonel)</span></Label>
            <div className="flex flex-wrap gap-2">
              {images.map((img, idx) => (
                <div key={idx} className="relative">
                  <img src={img.preview} alt="" className="h-16 w-16 rounded-lg object-cover border" />
                  <button
                    onClick={() => setImages(prev => prev.filter((_, i) => i !== idx).map((im, i) => ({ ...im, displayOrder: i })))}
                    className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full h-4 w-4 flex items-center justify-center"
                  >
                    <X className="h-2.5 w-2.5" />
                  </button>
                  <div className="absolute bottom-0 left-0 right-0 bg-black/50 rounded-b-lg px-1 py-0.5 text-center text-[9px] text-white">
                    {img.displayOrder === 0 ? 'Ana' : `#${img.displayOrder}`}
                  </div>
                </div>
              ))}
              <button
                onClick={() => fileRef.current?.click()}
                className="h-16 w-16 rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center gap-1 hover:border-blue-400 hover:bg-blue-50 transition-colors"
              >
                <ImagePlus className="h-5 w-5 text-gray-400" />
                <span className="text-[10px] text-gray-400">Ekle</span>
              </button>
              <input ref={fileRef} type="file" accept="image/*" multiple className="hidden" onChange={handleFiles} />
            </div>
            <Button
              type="button" size="sm" variant="outline"
              className="w-full gap-1.5 border-purple-200 text-purple-700 hover:bg-purple-50"
              disabled={aiImgLoading}
              onClick={handleGenerateAiImage}
            >
              {aiImgLoading
                ? <><Loader2 className="h-3.5 w-3.5 animate-spin" />DALL-E 2 üretiyor (~10sn)...</>
                : <><Sparkles className="h-3.5 w-3.5" />AI ile Görsel Üret (DALL-E 2)</>}
            </Button>
            <p className="text-xs text-muted-foreground">
              Görseller ürün kataloğunda görünür. Satıcı da sonradan listing'i üzerinden görsel ekleyebilir.
            </p>
            </div>
          </div>
        )}

        {/* Mevcut ürün arama */}
        {mode === 'existing' && (
          <div className="space-y-2">
            <Label className="text-xs">Katalogda Ara</Label>
            <div className="flex gap-2">
              <Input
                placeholder="ör. iPhone 256GB..."
                value={keyword}
                onChange={e => setKeyword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && setSearchTerm(keyword)}
              />
              <Button size="sm" variant="outline" onClick={() => setSearchTerm(keyword)}>Ara</Button>
            </div>
            {isFetching && <p className="text-xs text-muted-foreground">Aranıyor...</p>}
            {results?.content && results.content.length > 0 && (
              <div className="max-h-40 overflow-y-auto space-y-1 border rounded-lg p-1">
                {results.content.map((p: any) => (
                  <button
                    key={p.id}
                    onClick={() => { setSelectedId(p.id); setSelectedName(p.name) }}
                    className={`w-full text-left p-2 rounded text-sm transition-colors ${
                      selectedId === p.id ? 'bg-green-100 border border-green-400' : 'hover:bg-gray-50'
                    }`}
                  >
                    <span className="font-medium">{p.name}</span>
                    <span className="text-muted-foreground ml-2 text-xs">#{p.id} · {p.categoryName ?? ''}</span>
                  </button>
                ))}
              </div>
            )}
            {selectedId && (
              <div className="text-xs bg-green-50 border border-green-200 rounded p-2 text-green-800">
                Seçili: <span className="font-semibold">{selectedName}</span> (ID: {selectedId})
              </div>
            )}
          </div>
        )}

        <div className="flex justify-end gap-3 pt-1">
          <Button variant="outline" onClick={onClose}>İptal</Button>
          <Button
            className="bg-green-600 hover:bg-green-700"
            disabled={mode === 'existing' && !selectedId}
            onClick={() => {
              onSubmit(
                mode === 'existing' && selectedId ? selectedId : undefined,
                mode === 'new' ? images : undefined,
                mode === 'new' ? description.trim() || undefined : undefined,
              )
              onClose()
            }}
          >
            {mode === 'new' ? 'Onayla ve Oluştur' : 'Onayla ve Bağla'}
          </Button>
        </div>
      </div>
    </div>
  )
}

function EditProductDialog({ product, onClose }: { product: any; onClose: () => void }) {
  const qc = useQueryClient()
  const [name, setName] = useState(product.name ?? '')
  const [description, setDescription] = useState(product.description ?? '')
  const [price, setPrice] = useState(String(product.price ?? ''))

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: productsApi.getCategories,
  })

  const [categoryId, setCategoryId] = useState<number>(product.category?.id ?? product.categoryId ?? 0)

  const update = useMutation({
    mutationFn: () =>
      productsApi.updateProduct(product.id, {
        name,
        description,
        price: Number(price),
        categoryId,
      } as any),
    onSuccess: () => {
      toast.success('Ürün güncellendi')
      qc.invalidateQueries({ queryKey: ['admin', 'products'] })
      qc.invalidateQueries({ queryKey: ['product', String(product.id)] })
      onClose()
    },
    onError: () => toast.error('Güncelleme başarısız'),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center gap-2">
          <Pencil className="h-5 w-5 text-blue-600" />
          <h2 className="text-lg font-bold">Ürün Düzenle</h2>
          <span className="text-xs text-muted-foreground ml-auto">#{product.id}</span>
        </div>

        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>Ürün Adı *</Label>
            <Input value={name} onChange={e => setName(e.target.value)} placeholder="Ürün adı..." />
          </div>
          <div className="space-y-1.5">
            <Label>Açıklama</Label>
            <Textarea rows={4} value={description} onChange={e => setDescription(e.target.value)} placeholder="Ürün açıklaması..." />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label>Fiyat (TL) *</Label>
              <Input type="number" min="0.01" step="0.01" value={price} onChange={e => setPrice(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label>Kategori *</Label>
              <select
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
                value={categoryId}
                onChange={e => setCategoryId(Number(e.target.value))}
              >
                {categories?.map((c: { id: number; name: string }) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <Button variant="outline" onClick={onClose}>İptal</Button>
          <Button
            disabled={!name.trim() || !price || Number(price) <= 0 || !categoryId || update.isPending}
            onClick={() => update.mutate()}
          >
            {update.isPending ? 'Kaydediliyor...' : 'Kaydet'}
          </Button>
        </div>
      </div>
    </div>
  )
}

function MakeSellerDialog({ user, onClose }: { user: any; onClose: () => void }) {
  const qc = useQueryClient()
  const [storeName, setStoreName] = useState('')
  const [storeDesc, setStoreDesc] = useState('')

  const makeSellerMutation = useMutation({
    mutationFn: async () => {
      await adminApi.changeUserRole(user.id, 'SELLER')
      if (storeName.trim()) {
        await adminApi.updateStoreProfile(user.id, {
          storeName: storeName.trim(),
          storeDescription: storeDesc.trim() || undefined,
        })
      }
    },
    onSuccess: () => {
      toast.success(`${user.firstName} satıcı yapıldı`)
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
      onClose()
    },
    onError: () => toast.error('İşlem başarısız'),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
        <div className="flex items-center gap-2">
          <Store className="h-5 w-5 text-blue-600" />
          <h2 className="text-lg font-bold">Satıcı Yap</h2>
        </div>
        <p className="text-sm text-muted-foreground">
          <span className="font-medium">{user.firstName} {user.lastName}</span> adlı kullanıcıyı satıcı yapıyorsunuz.
        </p>

        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>Mağaza Adı <span className="text-muted-foreground text-xs">(opsiyonel, satıcı sonradan girebilir)</span></Label>
            <Input
              placeholder="ör. TechStore Türkiye"
              value={storeName}
              onChange={e => setStoreName(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label>Mağaza Açıklaması</Label>
            <Textarea
              placeholder="Kısa mağaza açıklaması..."
              rows={2}
              value={storeDesc}
              onChange={e => setStoreDesc(e.target.value)}
            />
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <Button variant="outline" onClick={onClose}>İptal</Button>
          <Button
            disabled={makeSellerMutation.isPending}
            onClick={() => makeSellerMutation.mutate()}
          >
            {makeSellerMutation.isPending ? 'Kaydediliyor...' : 'Satıcı Yap'}
          </Button>
        </div>
      </div>
    </div>
  )
}

export function AdminDashboard() {
  const location = useLocation()
  const qc = useQueryClient()
  const [revisionProposalId, setRevisionProposalId] = useState<number | null>(null)
  const [approveProposal, setApproveProposal] = useState<any | null>(null)
  const [makeSellerUser, setMakeSellerUser] = useState<any | null>(null)
  const [editProduct, setEditProduct] = useState<any | null>(null)
  const [productPage, setProductPage] = useState(0)
  const [productKeyword, setProductKeyword] = useState('')
  const [productSearch, setProductSearch] = useState('')

  const { data: pending, isLoading } = useQuery({
    queryKey: ['admin', 'proposals', 'pending'],
    queryFn: productsApi.getPendingProposals,
  })

  const { data: users, isLoading: loadingUsers } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => adminApi.getUsers(),
  })

  const { data: allProducts, isLoading: loadingProducts } = useQuery({
    queryKey: ['admin', 'products', productPage, productSearch],
    queryFn: () => productsApi.getAll({ page: productPage, size: 10, keyword: productSearch || undefined }),
  })

  const deleteProduct = useMutation({
    mutationFn: (id: number) => productsApi.deleteProduct(id),
    onSuccess: () => {
      toast.success('Ürün silindi')
      qc.invalidateQueries({ queryKey: ['admin', 'products'] })
    },
    onError: () => toast.error('Ürün silinemedi'),
  })

  const reviewProposal = useMutation({
    mutationFn: async ({ id, decision, adminNote, existingProductId, images, approvedDescription }: {
      id: number; decision: string; adminNote?: string; existingProductId?: number; images?: PendingImg[]; approvedDescription?: string
    }) => {
      // 1. Öneriyi değerlendir (onay/red/revizyon)
      const result = await productsApi.reviewProposal(id, { decision, adminNote, existingProductId, approvedDescription })
      // 2. Yeni ürün oluşturulduysa görselleri yükle
      if (decision === 'APPROVED' && images && images.length > 0) {
        const productId = (result as any)?.data?.approvedProductId
        if (productId) {
          for (const img of images) {
            const form = new FormData()
            form.append('file', img.file)
            form.append('displayOrder', String(img.displayOrder))
            await client.post(`/products/${productId}/images`, form, {
              headers: { 'Content-Type': 'multipart/form-data' },
            })
          }
        }
      }
    },
    onSuccess: (_, vars) => {
      const label = vars.decision === 'APPROVED' ? 'Onaylandı' : vars.decision === 'REJECTED' ? 'Reddedildi' : 'Revizyon istendi'
      toast.success(label)
      qc.invalidateQueries({ queryKey: ['admin', 'proposals', 'pending'] })
    },
    onError: () => toast.error('İşlem başarısız'),
  })

  const changeRole = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: string }) =>
      adminApi.changeUserRole(userId, role),
    onSuccess: (_, vars) => {
      toast.success(`Kullanıcı rolü → ${vars.role}`)
      qc.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: () => toast.error('Rol değiştirilemedi'),
  })

  return (
    <div className="space-y-6">
      <div className="rounded-[2rem] bg-slate-950 p-6 text-white shadow-xl shadow-slate-300/70">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-300">Admin</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight">Operasyon Paneli</h1>
        <p className="mt-2 max-w-2xl text-sm text-slate-300">
          Satıcı rolleri, ürün önerileri ve AI açıklama üretimi tek panelde yönetilir.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="rounded-2xl">
          <CardContent className="flex items-center gap-4 p-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-teal-50">
              <Users className="h-6 w-6 text-teal-700" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Kullanıcılar</p>
              <p className="text-2xl font-bold">{users?.totalElements ?? '—'}</p>
            </div>
          </CardContent>
        </Card>
        <Card className="rounded-2xl">
          <CardContent className="flex items-center gap-4 p-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-100">
              <ClipboardCheck className="h-6 w-6 text-amber-600" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Bekleyen Öneri</p>
              <p className="text-2xl font-bold">{pending?.length ?? '—'}</p>
            </div>
          </CardContent>
        </Card>
        <Card className="rounded-2xl">
          <CardContent className="flex items-center gap-4 p-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100">
              <Package className="h-6 w-6 text-slate-700" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Bekleyen Satıcı</p>
              <p className="text-2xl font-bold">
                {users ? (users.content?.filter((u: any) => u.role === 'SELLER').length ?? 0) : '—'}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Tabs key={location.pathname} defaultValue={adminDefaultTab(location.pathname)}>
        <TabsList>
          <TabsTrigger value="proposals">
            Ürün Önerileri {pending?.length ? `(${pending.length})` : ''}
          </TabsTrigger>
          <TabsTrigger value="products">Ürünler</TabsTrigger>
          <TabsTrigger value="users">Kullanıcılar</TabsTrigger>
        </TabsList>

        {/* Ürün Önerileri */}
        <TabsContent value="proposals" className="space-y-3 mt-4">
          {isLoading
            ? Array(3).fill(0).map((_, i) => <Skeleton key={i} className="h-24 rounded-xl" />)
            : pending?.length === 0
            ? <p className="text-muted-foreground text-center py-8">Bekleyen öneri yok</p>
            : pending?.map(p => (
              <Card key={p.id}>
                <CardContent className="p-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0 space-y-1">
                      <p className="font-medium">{p.proposedName}</p>
                      {p.proposedDescription && (
                        <p className="text-sm text-muted-foreground line-clamp-2">{p.proposedDescription}</p>
                      )}
                      <p className="text-sm font-black text-teal-700">{formatPrice(p.proposedPrice)}</p>
                    </div>
                    <div className="flex flex-col gap-1.5 shrink-0">
                      <Button
                        size="sm" variant="outline"
                        className="text-green-600 border-green-200 hover:bg-green-50"
                        onClick={() => setApproveProposal(p)}
                        disabled={reviewProposal.isPending}
                      >
                        Onayla
                      </Button>
                      <Button
                        size="sm" variant="outline"
                        className="text-amber-600 border-amber-200 hover:bg-amber-50"
                        onClick={() => setRevisionProposalId(p.id)}
                        disabled={reviewProposal.isPending}
                      >
                        Revizyon İste
                      </Button>
                      <Button
                        size="sm" variant="outline"
                        className="text-red-600 border-red-200 hover:bg-red-50"
                        onClick={() => reviewProposal.mutate({ id: p.id, decision: 'REJECTED' })}
                        disabled={reviewProposal.isPending}
                      >
                        Reddet
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
        </TabsContent>

        {/* Ürünler */}
        <TabsContent value="products" className="space-y-4 mt-4">
          <div className="flex gap-2">
            <Input
              placeholder="Ürün adı veya marka ara..."
              value={productKeyword}
              onChange={e => setProductKeyword(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') { setProductSearch(productKeyword); setProductPage(0) } }}
            />
            <Button variant="outline" onClick={() => { setProductSearch(productKeyword); setProductPage(0) }}>Ara</Button>
            {productSearch && <Button variant="ghost" onClick={() => { setProductSearch(''); setProductKeyword(''); setProductPage(0) }}>Temizle</Button>}
          </div>

          {loadingProducts
            ? Array(5).fill(0).map((_, i) => <Skeleton key={i} className="h-16 rounded-lg" />)
            : allProducts?.content.map((p: any) => (
              <Card key={p.id}>
                <CardContent className="flex items-center gap-3 p-3">
                  {p.images?.[0]?.url && (
                    <img
                      src={p.images[0].url}
                      alt={p.name}
                      className="h-12 w-12 rounded-lg object-cover border flex-shrink-0"
                      onError={e => { (e.target as HTMLImageElement).style.display = 'none' }}
                    />
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm truncate">{p.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {p.categoryName ?? p.category?.name ?? '—'} · {formatPrice(p.price)}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <Button
                      size="sm" variant="outline"
                      className="h-7 text-xs gap-1"
                      onClick={() => setEditProduct(p)}
                    >
                      <Pencil className="h-3 w-3" /> Düzenle
                    </Button>
                    <Button
                      size="sm" variant="outline"
                      className="h-7 text-xs gap-1 text-red-600 border-red-200 hover:bg-red-50"
                      disabled={deleteProduct.isPending}
                      onClick={() => {
                        if (window.confirm(`"${p.name}" ürününü silmek istediğinizden emin misiniz?`)) {
                          deleteProduct.mutate(p.id)
                        }
                      }}
                    >
                      <Trash2 className="h-3 w-3" /> Sil
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))
          }

          {allProducts && allProducts.totalPages > 1 && (
            <div className="flex items-center justify-between pt-2">
              <Button
                variant="outline" size="sm" disabled={productPage === 0}
                onClick={() => setProductPage(p => p - 1)}
              >
                <ChevronLeft className="h-4 w-4" /> Önceki
              </Button>
              <span className="text-sm text-muted-foreground">
                Sayfa {productPage + 1} / {allProducts.totalPages}
              </span>
              <Button
                variant="outline" size="sm" disabled={productPage >= allProducts.totalPages - 1}
                onClick={() => setProductPage(p => p + 1)}
              >
                Sonraki <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </TabsContent>

        {/* Kullanıcılar */}
        <TabsContent value="users" className="space-y-2 mt-4">
          {loadingUsers
            ? Array(5).fill(0).map((_, i) => <Skeleton key={i} className="h-16 rounded-lg" />)
            : users?.content.map((u: any) => {
              const roleColors: Record<string, 'secondary' | 'default' | 'destructive'> = {
                USER: 'secondary',
                SELLER: 'default',
                ADMIN: 'destructive',
              }
              return (
                <Card key={u.id}>
                  <CardContent className="flex items-center justify-between p-4">
                    <div>
                      <p className="font-medium">{u.firstName} {u.lastName}</p>
                      <p className="text-xs text-muted-foreground">{u.email}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant={roleColors[u.role] ?? 'secondary'}>{u.role}</Badge>
                      {u.role === 'USER' && (
                        <Button
                          size="sm" variant="outline"
                          onClick={() => setMakeSellerUser(u)}
                        >
                          Satıcı Yap
                        </Button>
                      )}
                      {u.role === 'SELLER' && (
                        <Button
                          size="sm" variant="outline"
                          className="text-red-600 border-red-200 hover:bg-red-50"
                          onClick={() => changeRole.mutate({ userId: u.id, role: 'USER' })}
                          disabled={changeRole.isPending}
                        >
                          Rolü Geri Al
                        </Button>
                      )}
                    </div>
                  </CardContent>
                </Card>
              )
            })}
        </TabsContent>
      </Tabs>

      {/* Onaylama Dialog */}
      {approveProposal && (
        <ApproveDialog
          proposal={approveProposal}
          onClose={() => setApproveProposal(null)}
          onSubmit={(existingProductId, images, approvedDescription) =>
            reviewProposal.mutate({
              id: approveProposal.id,
              decision: 'APPROVED',
              existingProductId,
              images,
              approvedDescription,
            })
          }
        />
      )}

      {/* Satıcı Yap Dialog */}
      {makeSellerUser && (
        <MakeSellerDialog user={makeSellerUser} onClose={() => setMakeSellerUser(null)} />
      )}

      {/* Ürün Düzenleme Dialog */}
      {editProduct && (
        <EditProductDialog product={editProduct} onClose={() => setEditProduct(null)} />
      )}

      {/* Revizyon Dialog */}
      {revisionProposalId !== null && (
        <RevisionDialog
          onClose={() => setRevisionProposalId(null)}
          onSubmit={note =>
            reviewProposal.mutate({
              id: revisionProposalId,
              decision: 'REVISION_REQUESTED',
              adminNote: note,
            })
          }
        />
      )}
    </div>
  )
}
