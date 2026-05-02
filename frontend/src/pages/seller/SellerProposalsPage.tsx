import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { productsApi } from '@/api/products'
import { Plus, ClipboardList, ChevronUp, Edit2, Send, PackagePlus } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { formatPrice } from '@/lib/utils'
import type { ProductProposal } from '@/types'

const schema = z.object({
  proposedName: z.string().min(2, 'Ürün adı en az 2 karakter olmalı').max(200),
  proposedDescription: z.string().max(5000).optional(),
  proposedPrice: z.coerce.number().min(0.01, 'Fiyat 0\'dan büyük olmalı'),
  categoryId: z.coerce.number().min(1, 'Kategori seçiniz'),
  brandId: z.coerce.number().optional(),
})
type FormInput = z.input<typeof schema>
type FormData = z.output<typeof schema>

const STATUS_LABEL: Record<ProductProposal['status'], string> = {
  PENDING: 'İnceleniyor',
  APPROVED: 'Onaylandı',
  REJECTED: 'Reddedildi',
  REVISION_REQUESTED: 'Revizyon İstendi',
}
const STATUS_VARIANT: Record<ProductProposal['status'], 'secondary' | 'default' | 'destructive' | 'outline'> = {
  PENDING: 'secondary',
  APPROVED: 'default',
  REJECTED: 'destructive',
  REVISION_REQUESTED: 'outline',
}

function RevisionEditForm({ proposal, onClose }: { proposal: ProductProposal; onClose: () => void }) {
  const qc = useQueryClient()

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: productsApi.getCategories,
  })

  const { data: brands } = useQuery({
    queryKey: ['brands'],
    queryFn: productsApi.getBrands,
  })

  const { register, handleSubmit, formState: { errors } } = useForm<FormInput, unknown, FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      proposedName: proposal.proposedName,
      proposedDescription: proposal.proposedDescription ?? '',
      proposedPrice: proposal.proposedPrice,
      categoryId: proposal.categoryId,
      brandId: proposal.brandId ?? undefined,
    },
  })

  const resubmit = useMutation({
    mutationFn: (data: FormData) =>
      productsApi.updateProposal(proposal.id, {
        proposedName: data.proposedName,
        proposedDescription: data.proposedDescription,
        proposedPrice: data.proposedPrice,
        categoryId: data.categoryId,
        brandId: data.brandId || undefined,
      }),
    onSuccess: () => {
      toast.success('Teklifiniz revize edilerek yeniden gönderildi.')
      qc.invalidateQueries({ queryKey: ['seller', 'proposals'] })
      onClose()
    },
    onError: () => toast.error('Revizyon gönderilemedi, lütfen tekrar deneyin.'),
  })

  return (
    <div className="mt-3 bg-amber-50 border border-amber-200 rounded-lg p-4">
      <p className="text-xs font-semibold text-amber-700 mb-3 flex items-center gap-1">
        <Edit2 className="h-3.5 w-3.5" /> Revize Et ve Yeniden Gönder
      </p>
      <form onSubmit={handleSubmit(d => resubmit.mutate(d))} className="space-y-3">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div className="md:col-span-2 space-y-1">
            <Label className="text-xs">Ürün Adı *</Label>
            <Input placeholder="ör. Apple iPhone 15 128GB" {...register('proposedName')} />
            {errors.proposedName && <p className="text-xs text-red-500">{errors.proposedName.message}</p>}
          </div>

          <div className="space-y-1">
            <Label className="text-xs">Kategori *</Label>
            <select
              className="w-full h-9 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              {...register('categoryId')}
            >
              <option value="">Kategori seçin...</option>
              {categories?.map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
            {errors.categoryId && <p className="text-xs text-red-500">{errors.categoryId.message}</p>}
          </div>

          <div className="space-y-1">
            <Label className="text-xs">Marka</Label>
            <select
              className="w-full h-9 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              {...register('brandId')}
            >
              <option value="">Marka seçin (opsiyonel)</option>
              {brands?.map(b => (
                <option key={b.id} value={b.id}>{b.name}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <Label className="text-xs">Önerilen Fiyat (TL) *</Label>
            <Input type="number" step="0.01" min="0.01" placeholder="0.00" {...register('proposedPrice')} />
            {errors.proposedPrice && <p className="text-xs text-red-500">{errors.proposedPrice.message}</p>}
          </div>

          <div className="md:col-span-2 space-y-1">
            <Label className="text-xs">Açıklama</Label>
            <Textarea
              placeholder="Ürün hakkında detaylı bilgi girin..."
              rows={3}
              {...register('proposedDescription')}
            />
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" size="sm" variant="outline" onClick={onClose}>İptal</Button>
          <Button type="submit" size="sm" disabled={resubmit.isPending} className="gap-1.5">
            <Send className="h-3.5 w-3.5" />
            {resubmit.isPending ? 'Gönderiliyor...' : 'Yeniden Gönder'}
          </Button>
        </div>
      </form>
    </div>
  )
}

export function SellerProposalsPage() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)

  const { data: proposals, isLoading } = useQuery({
    queryKey: ['seller', 'proposals'],
    queryFn: productsApi.getMyProposals,
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: productsApi.getCategories,
  })

  const { data: brands } = useQuery({
    queryKey: ['brands'],
    queryFn: productsApi.getBrands,
  })

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormInput, unknown, FormData>({
    resolver: zodResolver(schema),
  })

  const submitProposal = useMutation({
    mutationFn: (data: FormData) =>
      productsApi.submitProposal({
        proposedName: data.proposedName,
        proposedDescription: data.proposedDescription,
        proposedPrice: data.proposedPrice,
        categoryId: data.categoryId,
        brandId: data.brandId || undefined,
      }),
    onSuccess: () => {
      toast.success('Ürün teklifiniz iletildi, admin onayı bekleniyor.')
      qc.invalidateQueries({ queryKey: ['seller', 'proposals'] })
      reset()
      setShowForm(false)
    },
    onError: () => toast.error('Öneri gönderilemedi, lütfen tekrar deneyin.'),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Ürün Önerileri</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Kataloğa yeni ürün eklemek için öneri gönderin. Admin onayladıktan sonra ürüne listing ekleyebilirsiniz.
          </p>
        </div>
        <Button onClick={() => setShowForm(v => !v)} className="gap-2">
          {showForm ? <ChevronUp className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {showForm ? 'İptal' : 'Yeni Öneri'}
        </Button>
      </div>

      {/* Yeni Öneri Formu */}
      {showForm && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Yeni Ürün Önerisi</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(d => submitProposal.mutate(d))} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="md:col-span-2 space-y-1.5">
                  <Label>Ürün Adı *</Label>
                  <Input placeholder="ör. Apple iPhone 15 128GB" {...register('proposedName')} />
                  {errors.proposedName && <p className="text-xs text-red-500">{errors.proposedName.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <Label>Kategori *</Label>
                  <select
                    className="w-full h-10 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    {...register('categoryId')}
                  >
                    <option value="">Kategori seçin...</option>
                    {categories?.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                  {errors.categoryId && <p className="text-xs text-red-500">{errors.categoryId.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <Label>Marka (opsiyonel)</Label>
                  <select
                    className="w-full h-10 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    {...register('brandId')}
                  >
                    <option value="">Marka seçin (opsiyonel)</option>
                    {brands?.map(b => (
                      <option key={b.id} value={b.id}>{b.name}</option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1.5">
                  <Label>Önerilen Fiyat (TL) *</Label>
                  <Input type="number" step="0.01" min="0.01" placeholder="0.00" {...register('proposedPrice')} />
                  {errors.proposedPrice && <p className="text-xs text-red-500">{errors.proposedPrice.message}</p>}
                </div>

                <div className="md:col-span-2 space-y-1.5">
                  <Label>Açıklama</Label>
                  <Textarea placeholder="Ürün hakkında detaylı bilgi girin..." rows={4} {...register('proposedDescription')} />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <Button type="button" variant="outline" onClick={() => { setShowForm(false); reset() }}>
                  İptal
                </Button>
                <Button type="submit" disabled={submitProposal.isPending}>
                  {submitProposal.isPending ? 'Gönderiliyor...' : 'Öneri Gönder'}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Mevcut Öneriler */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <ClipboardList className="h-5 w-5" /> Önerilerim
            {proposals && proposals.length > 0 && (
              <Badge variant="secondary" className="ml-auto">{proposals.length} adet</Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {Array(3).fill(0).map((_, i) => <Skeleton key={i} className="h-16 rounded-lg" />)}
            </div>
          ) : !proposals?.length ? (
            <div className="text-center py-10 text-muted-foreground">
              <ClipboardList className="h-10 w-10 mx-auto mb-3 opacity-30" />
              <p className="font-medium">Henüz öneri göndermediniz</p>
              <p className="text-sm mt-1">Yeni bir ürün önermek için yukarıdaki "Yeni Öneri" butonunu kullanın.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {proposals.map(p => (
                <div key={p.id} className={`rounded-lg border p-4 ${
                  p.status === 'REVISION_REQUESTED' ? 'border-amber-300 bg-amber-50/50' : ''
                }`}>
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <p className="font-medium text-sm">{p.proposedName}</p>
                        <Badge variant={STATUS_VARIANT[p.status]} className="text-xs">
                          {STATUS_LABEL[p.status]}
                        </Badge>
                      </div>
                      {p.proposedDescription && (
                        <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{p.proposedDescription}</p>
                      )}
                      <div className="flex items-center gap-3 mt-2 text-xs text-muted-foreground">
                        <span className="font-semibold text-blue-600">{formatPrice(p.proposedPrice)}</span>
                        <Separator orientation="vertical" className="h-3" />
                        <span>{new Date(p.createdAt).toLocaleDateString('tr-TR')}</span>
                      </div>
                      {p.adminNote && (
                        <div className="mt-2 text-xs bg-amber-100 text-amber-900 border border-amber-300 rounded p-2">
                          <span className="font-semibold">Admin notu: </span>{p.adminNote}
                        </div>
                      )}
                    </div>

                    <div className="flex flex-col gap-1.5 shrink-0">
                      {/* Onaylandıysa: listing oluştur CTA */}
                      {p.status === 'APPROVED' && p.approvedProductId && (
                        <Button
                          size="sm"
                          className="gap-1.5 bg-green-600 hover:bg-green-700 text-white"
                          onClick={() => navigate(`/seller/listings?addProduct=${p.approvedProductId}`)}
                        >
                          <PackagePlus className="h-3.5 w-3.5" />
                          Listing Oluştur
                        </Button>
                      )}
                      {/* Revizyon istendiyse düzenleme butonu */}
                      {p.status === 'REVISION_REQUESTED' && (
                        <Button
                          size="sm" variant="outline"
                          className="text-amber-700 border-amber-300 hover:bg-amber-100 gap-1.5"
                          onClick={() => setEditingId(editingId === p.id ? null : p.id)}
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                          {editingId === p.id ? 'Kapat' : 'Revize Et'}
                        </Button>
                      )}
                    </div>
                  </div>

                  {/* Revizyon inline formu */}
                  {editingId === p.id && p.status === 'REVISION_REQUESTED' && (
                    <RevisionEditForm proposal={p} onClose={() => setEditingId(null)} />
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
