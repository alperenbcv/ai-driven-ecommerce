import { useQuery } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useState, useEffect, useCallback } from 'react'
import {
  Sparkles, SlidersHorizontal, ChevronDown,
  ChevronUp, X, Search, Filter,
} from 'lucide-react'
import { productsApi } from '@/api/products'
import { aiApi } from '@/api/ai'
import { formatPrice } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import type { Product } from '@/types'
import { ProductCard } from '@/components/ProductCard'


function FilterSection({
  title,
  children,
}: {
  title: string
  children: React.ReactNode
}) {
  const [open, setOpen] = useState(true)
  return (
    <div>
      <button
        className="flex items-center justify-between w-full py-2 font-semibold text-sm"
        onClick={() => setOpen(o => !o)}
      >
        {title}
        {open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
      </button>
      {open && <div className="mt-2 space-y-1">{children}</div>}
    </div>
  )
}

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()

  const keyword   = searchParams.get('keyword') ?? ''
  const q         = searchParams.get('q') ?? ''
  const catId     = searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : undefined
  const brandId   = searchParams.get('brandId')   ? Number(searchParams.get('brandId'))   : undefined
  const minPrice  = searchParams.get('minPrice')  ? Number(searchParams.get('minPrice'))  : undefined
  const maxPrice  = searchParams.get('maxPrice')  ? Number(searchParams.get('maxPrice'))  : undefined
  const sortBy    = searchParams.get('sortBy')    ?? 'createdAt'
  const sortDir   = searchParams.get('sortDir')   ?? 'desc'
  const page      = searchParams.get('page')      ? Number(searchParams.get('page'))      : 0

  const [localKeyword, setLocalKeyword] = useState(keyword)
  const [localMin, setLocalMin] = useState(minPrice?.toString() ?? '')
  const [localMax, setLocalMax] = useState(maxPrice?.toString() ?? '')
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const [semanticProducts, setSemanticProducts] = useState<Product[]>([])
  const [semanticLoading, setSemanticLoading] = useState(false)

  const isAiMode = !!q

  const updateParams = useCallback(
    (updates: Record<string, string | undefined>) => {
      const next = new URLSearchParams(searchParams)
      next.delete('page')
      Object.entries(updates).forEach(([k, v]) => {
        if (v === undefined || v === '') next.delete(k)
        else next.set(k, v)
      })
      setSearchParams(next, { replace: true })
    },
    [searchParams, setSearchParams],
  )

  const { data, isLoading } = useQuery({
    queryKey: ['products', 'list', { keyword, catId, brandId, minPrice, maxPrice, sortBy, sortDir, page }],
    queryFn: () =>
      productsApi.getAll({
        page,
        size: 12,
        keyword: keyword || undefined,
        categoryId: catId,
        brandId,
        minPrice,
        maxPrice,
        sortBy,
        sortDir,
      }),
    enabled: !isAiMode,
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: () => productsApi.getCategories(),
    staleTime: Infinity,
  })

  const { data: brands } = useQuery({
    queryKey: ['brands'],
    queryFn: () => productsApi.getBrands(),
    staleTime: Infinity,
  })

  useEffect(() => {
    if (!q) { setSemanticProducts([]); return }
    setSemanticLoading(true)
    aiApi
      .search(q, 12, 0.2)
      .then(async (ids) => {
        if (ids.length === 0) { setSemanticProducts([]); return }
        const results = await Promise.allSettled(ids.map(id => productsApi.getById(id)))
        setSemanticProducts(
          results
            .filter(r => r.status === 'fulfilled')
            .map(r => (r as PromiseFulfilledResult<Product>).value),
        )
      })
      .catch(() => {
        productsApi.getAll({ page: 0, size: 12, keyword: q }).then(d => setSemanticProducts(d.content))
      })
      .finally(() => setSemanticLoading(false))
  }, [q])

  const products   = isAiMode ? semanticProducts : (data?.content ?? [])
  const loading    = isAiMode ? semanticLoading  : isLoading
  const totalPages = isAiMode ? 1 : (data?.totalPages ?? 1)
  const totalItems = isAiMode ? semanticProducts.length : (data?.totalElements ?? 0)

  const activeFilters = [catId, brandId, minPrice, maxPrice]
    .filter(Boolean).length + (keyword ? 1 : 0)

  const clearFilters = () => {
    setLocalKeyword('')
    setLocalMin('')
    setLocalMax('')
    navigate('/products', { replace: true })
  }

  const handleKeywordSearch = (e: React.FormEvent) => {
    e.preventDefault()
    updateParams({ keyword: localKeyword, q: undefined })
  }

  const handlePriceFilter = (e: React.FormEvent) => {
    e.preventDefault()
    updateParams({
      minPrice: localMin || undefined,
      maxPrice: localMax || undefined,
    })
  }

  const Sidebar = (
    <aside className="surface space-y-5 rounded-2xl p-4">
      {/* Arama kutusu */}
      <form onSubmit={handleKeywordSearch} className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Ürün ara..."
            className="pl-8 h-9"
            value={localKeyword}
            onChange={e => setLocalKeyword(e.target.value)}
          />
        </div>
        <Button type="submit" size="sm" className="h-9 px-3">Ara</Button>
      </form>

      {/* Filtre sıfırla */}
      {(activeFilters > 0 || isAiMode) && (
        <Button variant="ghost" size="sm" className="w-full text-red-500 hover:text-red-600" onClick={clearFilters}>
          <X className="h-4 w-4 mr-1" /> Filtreleri Temizle
        </Button>
      )}

      <Separator />

      {/* Kategoriler */}
      <FilterSection title="Kategori">
        <button
          onClick={() => updateParams({ categoryId: undefined })}
            className={`block w-full rounded-lg px-2 py-1.5 text-left text-sm transition-colors hover:bg-slate-100 ${!catId ? 'bg-teal-50 font-semibold text-teal-800' : 'text-slate-600'}`}
        >
          Tümü
        </button>
        {categories?.map(cat => (
          <button
            key={cat.id}
            onClick={() => updateParams({ categoryId: String(cat.id), brandId: undefined })}
            className={`block w-full rounded-lg px-2 py-1.5 text-left text-sm transition-colors hover:bg-slate-100 ${catId === cat.id ? 'bg-teal-50 font-semibold text-teal-800' : 'text-slate-600'}`}
          >
            {cat.name}
          </button>
        ))}
      </FilterSection>

      <Separator />

      {/* Markalar */}
      <FilterSection title="Marka">
        <button
          onClick={() => updateParams({ brandId: undefined })}
            className={`block w-full rounded-lg px-2 py-1.5 text-left text-sm transition-colors hover:bg-slate-100 ${!brandId ? 'bg-teal-50 font-semibold text-teal-800' : 'text-slate-600'}`}
        >
          Tümü
        </button>
        {brands?.map(brand => (
          <button
            key={brand.id}
            onClick={() => updateParams({ brandId: String(brand.id) })}
            className={`block w-full rounded-lg px-2 py-1.5 text-left text-sm transition-colors hover:bg-slate-100 ${brandId === brand.id ? 'bg-teal-50 font-semibold text-teal-800' : 'text-slate-600'}`}
          >
            {brand.name}
          </button>
        ))}
      </FilterSection>

      <Separator />

      {/* Fiyat aralığı */}
      <FilterSection title="Fiyat Aralığı">
        <form onSubmit={handlePriceFilter} className="space-y-2">
          <div className="flex gap-2 items-center">
            <Input
              type="number"
              placeholder="Min"
              className="h-8 text-sm"
              value={localMin}
              onChange={e => setLocalMin(e.target.value)}
              min={0}
            />
            <span className="text-muted-foreground text-sm">—</span>
            <Input
              type="number"
              placeholder="Maks"
              className="h-8 text-sm"
              value={localMax}
              onChange={e => setLocalMax(e.target.value)}
              min={0}
            />
          </div>
          <Button type="submit" size="sm" variant="outline" className="w-full h-8 text-xs">
            Uygula
          </Button>
        </form>
      </FilterSection>

      <Separator />

      {/* AI Arama */}
      <FilterSection title="AI Arama">
        <p className="text-xs text-muted-foreground mb-2">
          Doğal dilde arama — "kırmızı wireless kulaklık", "gaming laptop 30k altı" gibi
        </p>
        <Button
          variant="outline"
          size="sm"
          className="w-full gap-2 border-teal-200 text-teal-800 hover:bg-teal-50"
          onClick={() => navigate('/chat')}
        >
          <Sparkles className="h-4 w-4" />
          AI Asistan'a Sor
        </Button>
      </FilterSection>
    </aside>
  )

  return (
    <div className="flex gap-6">
      {/* Desktop Sidebar */}
      <div className="hidden w-64 shrink-0 lg:block">
        {Sidebar}
      </div>

      {/* İçerik */}
      <div className="min-w-0 flex-1 space-y-5">
        <div className="overflow-hidden rounded-[2rem] bg-slate-950 p-6 text-white shadow-xl shadow-slate-300/70">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-300">
                {isAiMode ? 'AI Arama' : 'Katalog'}
              </p>
              <h1 className="mt-2 text-3xl font-black tracking-tight">
                {isAiMode
                  ? `"${q}" için akıllı sonuçlar`
                  : keyword
                  ? `"${keyword}" araması`
                  : catId
                  ? (categories?.find(c => c.id === catId)?.name ?? 'Ürünler')
                  : 'Tüm ürünler'}
              </h1>
              <p className="mt-2 text-sm text-slate-300">
                {totalItems > 0 ? `${totalItems} ürün listeleniyor` : 'Filtreleri değiştirerek tekrar dene'}
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              {activeFilters > 0 && <Badge className="bg-white text-slate-950 hover:bg-white">{activeFilters} filtre</Badge>}
              {isAiMode && (
                <Button variant="secondary" size="sm" onClick={() => navigate('/products')}>
                  <X className="h-3.5 w-3.5" /> Temizle
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* Başlık + sıralama + mobil filtre */}
        <div className="surface flex flex-wrap items-center justify-between gap-3 rounded-2xl p-3">
          <div className="flex items-center gap-2">
            {/* Mobil filtre toggle */}
            <Button
              variant="outline"
              size="sm"
              className="lg:hidden relative"
              onClick={() => setSidebarOpen(o => !o)}
            >
              <Filter className="h-4 w-4 mr-2" />
              Filtrele
              {activeFilters > 0 && (
                <span className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-teal-700 text-xs text-white">
                  {activeFilters}
                </span>
              )}
            </Button>

            {/* Sıralama */}
            {!isAiMode && (
              <select
                className="h-10 cursor-pointer rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700"
                value={`${sortBy}-${sortDir}`}
                onChange={e => {
                  const [by, dir] = e.target.value.split('-')
                  updateParams({ sortBy: by, sortDir: dir })
                }}
              >
                <option value="createdAt-desc">En Yeni</option>
                <option value="createdAt-asc">En Eski</option>
                <option value="price-asc">Fiyat: Düşükten Yükseğe</option>
                <option value="price-desc">Fiyat: Yüksekten Düşüğe</option>
                <option value="name-asc">İsim: A → Z</option>
                <option value="name-desc">İsim: Z → A</option>
              </select>
            )}
          </div>
        </div>

        {/* Mobil sidebar overlay */}
        {sidebarOpen && (
          <div className="lg:hidden">
            <div
              className="fixed inset-0 bg-black/40 z-30"
              onClick={() => setSidebarOpen(false)}
            />
            <div className="fixed left-0 top-0 z-40 h-full w-80 overflow-y-auto bg-white p-4 shadow-xl">
              <div className="flex items-center justify-between mb-4">
                <span className="font-bold flex items-center gap-2">
                  <SlidersHorizontal className="h-4 w-4" /> Filtreler
                </span>
                <Button variant="ghost" size="sm" onClick={() => setSidebarOpen(false)}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
              {Sidebar}
            </div>
          </div>
        )}

        {/* Aktif filtre rozetleri */}
        {!isAiMode && activeFilters > 0 && (
          <div className="flex flex-wrap gap-2">
            {keyword && (
              <Badge variant="outline" className="gap-1 pr-1">
                "{keyword}"
                <button className="ml-1 hover:text-destructive" onClick={() => { setLocalKeyword(''); updateParams({ keyword: undefined }) }}>
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )}
            {catId && (
              <Badge variant="outline" className="gap-1 pr-1">
                {categories?.find(c => c.id === catId)?.name}
                <button className="ml-1 hover:text-destructive" onClick={() => updateParams({ categoryId: undefined })}>
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )}
            {brandId && (
              <Badge variant="outline" className="gap-1 pr-1">
                {brands?.find(b => b.id === brandId)?.name}
                <button className="ml-1 hover:text-destructive" onClick={() => updateParams({ brandId: undefined })}>
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )}
            {(minPrice || maxPrice) && (
              <Badge variant="outline" className="gap-1 pr-1">
                {minPrice ? formatPrice(minPrice) : '0 ₺'} — {maxPrice ? formatPrice(maxPrice) : '∞'}
                <button className="ml-1 hover:text-destructive" onClick={() => { setLocalMin(''); setLocalMax(''); updateParams({ minPrice: undefined, maxPrice: undefined }) }}>
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )}
          </div>
        )}

        {/* Ürün grid */}
        {loading ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4">
            {Array(12).fill(0).map((_, i) => <Skeleton key={i} className="h-72 rounded-2xl" />)}
          </div>
        ) : products.length === 0 ? (
          <div className="surface space-y-3 rounded-2xl py-20 text-center">
            <p className="text-lg font-semibold text-slate-700">Ürün bulunamadı</p>
            {activeFilters > 0 && (
              <Button variant="outline" onClick={clearFilters}>
                Filtreleri temizle
              </Button>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4">
            {products.map(p => <ProductCard key={p.id} product={p} />)}
          </div>
        )}

        {/* Sayfalama */}
        {!isAiMode && totalPages > 1 && (
          <div className="flex justify-center gap-2 pt-2">
            <Button
              variant="outline"
              disabled={page === 0}
              onClick={() => updateParams({ page: String(page - 1) })}
            >
              Önceki
            </Button>
            <span className="flex items-center px-4 text-sm">
              {page + 1} / {totalPages}
            </span>
            <Button
              variant="outline"
              disabled={page >= totalPages - 1}
              onClick={() => updateParams({ page: String(page + 1) })}
            >
              Sonraki
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
