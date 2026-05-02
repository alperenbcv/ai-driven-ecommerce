import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { productsApi } from '@/api/products'
import client from '@/api/client'
import type { ApiResponse } from '@/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Skeleton } from '@/components/ui/skeleton'
import { Package, ClipboardList, TrendingUp, Store, Edit2, Save, X } from 'lucide-react'
import { formatPrice } from '@/lib/utils'
import { toast } from 'sonner'

export function SellerDashboard() {
  const qc = useQueryClient()
  const [editingStore, setEditingStore] = useState(false)
  const [storeName, setStoreName] = useState('')
  const [storeDesc, setStoreDesc] = useState('')

  const { data: listings, isLoading: loadingListings } = useQuery({
    queryKey: ['seller', 'listings'],
    queryFn: productsApi.getMyListings,
  })

  const { data: proposals, isLoading: loadingProposals } = useQuery({
    queryKey: ['seller', 'proposals'],
    queryFn: productsApi.getMyProposals,
  })

  const { data: profile } = useQuery({
    queryKey: ['seller', 'profile'],
    queryFn: () => client.get<ApiResponse<{ storeName?: string; storeDescription?: string; firstName: string; lastName: string }>>('/users/me').then(r => r.data.data),
  })

  const updateStore = useMutation({
    mutationFn: () => client.put('/users/me/store-profile', {
      storeName: storeName.trim(),
      storeDescription: storeDesc.trim() || undefined,
    }),
    onSuccess: () => {
      toast.success('Mağaza profili güncellendi')
      qc.invalidateQueries({ queryKey: ['seller', 'profile'] })
      setEditingStore(false)
    },
    onError: () => toast.error('Güncelleme başarısız'),
  })

  const openEdit = () => {
    setStoreName(profile?.storeName ?? '')
    setStoreDesc(profile?.storeDescription ?? '')
    setEditingStore(true)
  }

  const activeListings = listings?.filter(l => l.active).length ?? 0
  const pendingProposals = proposals?.filter(p => p.status === 'PENDING').length ?? 0

  return (
    <div className="space-y-6">
      <div className="rounded-[2rem] bg-slate-950 p-6 text-white shadow-xl shadow-slate-300/70">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-300">Seller</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight">Satıcı Paneli</h1>
        <p className="mt-2 max-w-2xl text-sm text-slate-300">
          Listing fiyatlarını, mağaza profilini ve katalog önerilerini tek yerden yönet.
        </p>
      </div>

      {/* Mağaza Profili */}
      <Card className="rounded-2xl border-teal-100 bg-teal-50/40">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2 text-base">
              <Store className="h-5 w-5 text-teal-700" /> Mağaza Profilim
            </CardTitle>
            {!editingStore && (
              <Button size="sm" variant="outline" className="gap-1.5" onClick={openEdit}>
                <Edit2 className="h-3.5 w-3.5" /> Düzenle
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {editingStore ? (
            <div className="space-y-3">
              <div className="space-y-1.5">
                <Label>Mağaza Adı *</Label>
                <Input placeholder="ör. TechStore Türkiye" value={storeName} onChange={e => setStoreName(e.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label>Mağaza Açıklaması</Label>
                <Textarea placeholder="Mağazanızı tanıtın..." rows={2} value={storeDesc} onChange={e => setStoreDesc(e.target.value)} />
              </div>
              <div className="flex gap-2 pt-1">
                <Button size="sm" disabled={!storeName.trim() || updateStore.isPending} onClick={() => updateStore.mutate()} className="gap-1.5">
                  <Save className="h-3.5 w-3.5" />
                  {updateStore.isPending ? 'Kaydediliyor...' : 'Kaydet'}
                </Button>
                <Button size="sm" variant="outline" onClick={() => setEditingStore(false)} className="gap-1.5">
                  <X className="h-3.5 w-3.5" /> İptal
                </Button>
              </div>
            </div>
          ) : (
            <div>
              {profile?.storeName ? (
                <>
                  <p className="font-semibold text-teal-800">{profile.storeName}</p>
                  {profile.storeDescription && (
                    <p className="text-sm text-muted-foreground mt-1">{profile.storeDescription}</p>
                  )}
                </>
              ) : (
                <p className="text-sm text-muted-foreground italic">
                  Mağaza adınızı henüz girmediniz. Müşteriler ürün sayfasında satıcıyı mağaza adıyla görecek.
                </p>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="flex items-center gap-4 p-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-teal-50">
              <Package className="h-6 w-6 text-teal-700" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Aktif Listing</p>
              {loadingListings ? <Skeleton className="h-7 w-12" /> : <p className="text-2xl font-bold">{activeListings}</p>}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 p-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-100">
              <ClipboardList className="h-6 w-6 text-amber-600" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Bekleyen Öneri</p>
              {loadingProposals ? <Skeleton className="h-7 w-12" /> : <p className="text-2xl font-bold">{pendingProposals}</p>}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-4 p-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-100">
              <TrendingUp className="h-6 w-6 text-green-600" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Toplam Listing</p>
              {loadingListings ? <Skeleton className="h-7 w-12" /> : <p className="text-2xl font-bold">{listings?.length ?? 0}</p>}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Listing'lerim */}
      <Card>
        <CardHeader><CardTitle>Listing'lerim</CardTitle></CardHeader>
        <CardContent className="space-y-2">
          {loadingListings
            ? Array(3).fill(0).map((_, i) => <Skeleton key={i} className="h-14 rounded-lg" />)
            : listings?.length === 0
            ? <p className="text-muted-foreground text-sm py-4 text-center">Henüz listing oluşturmadınız</p>
            : listings?.map(l => (
              <div key={l.id} className="flex items-center justify-between p-3 rounded-lg border">
                <div>
                  <p className="font-medium text-sm">{l.productName}</p>
                  <p className="text-xs text-muted-foreground">Ürün #{l.productId}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-black text-teal-700">{formatPrice(l.price)}</span>
                  <Badge variant={l.active ? 'default' : 'secondary'}>{l.active ? 'Aktif' : 'Pasif'}</Badge>
                </div>
              </div>
            ))}
        </CardContent>
      </Card>

      {/* Öneri geçmişi */}
      <Card>
        <CardHeader><CardTitle>Ürün Önerilerim</CardTitle></CardHeader>
        <CardContent className="space-y-2">
          {loadingProposals
            ? Array(2).fill(0).map((_, i) => <Skeleton key={i} className="h-14 rounded-lg" />)
            : proposals?.length === 0
            ? <p className="text-muted-foreground text-sm py-4 text-center">Henüz öneri göndermediniz</p>
            : proposals?.map(p => {
              const variantMap = { PENDING: 'secondary', APPROVED: 'default', REJECTED: 'destructive', REVISION_REQUESTED: 'outline' } as const
              const labelMap = { PENDING: 'Bekliyor', APPROVED: 'Onaylandı', REJECTED: 'Reddedildi', REVISION_REQUESTED: 'Revizyon' }
              return (
                <div key={p.id} className="flex items-center justify-between p-3 rounded-lg border">
                  <div>
                    <p className="font-medium text-sm">{p.proposedName}</p>
                    {p.adminNote && <p className="text-xs text-muted-foreground">{p.adminNote}</p>}
                  </div>
                  <Badge variant={variantMap[p.status]}>{labelMap[p.status]}</Badge>
                </div>
              )
            })}
        </CardContent>
      </Card>
    </div>
  )
}
