import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowRight, Bot, Boxes, Brain, CreditCard, ShieldCheck,
  Sparkles, Truck,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { productsApi } from '@/api/products'
import { aiApi } from '@/api/ai'
import { useIsAuthenticated, useUser } from '@/stores/authStore'
import { useState } from 'react'
import type { Product } from '@/types'
import { ProductCard } from '@/components/ProductCard'
import heroImage from '@/assets/hero.png'

const trustItems = [
  { icon: Sparkles, title: 'AI destekli keşif', desc: 'Ürünleri doğal dille ara, önerileri davranışlarına göre gör.' },
  { icon: Truck, title: 'Takip edilebilir teslimat', desc: 'Sipariş durumunu panelden ya da asistandan anlık sorgula.' },
  { icon: ShieldCheck, title: 'Güvenli ödeme', desc: 'Ödeme akışı Iyzico ile ayrışır, kart bilgileri platformda tutulmaz.' },
]

export function HomePage() {
  const navigate = useNavigate()
  const isAuth = useIsAuthenticated()
  const user = useUser()
  const [searchQ, setSearchQ] = useState('')

  const { data: products, isLoading } = useQuery({
    queryKey: ['products', 'featured'],
    queryFn: () => productsApi.getAll({ size: 8 }),
  })

  const { data: recommendedIds } = useQuery({
    queryKey: ['recommendations', user?.id],
    queryFn: () => aiApi.getRecommendationsForUser(user!.id, 8),
    enabled: isAuth && !!user,
  })

  const { data: popularIds } = useQuery({
    queryKey: ['recommendations', 'popular'],
    queryFn: () => aiApi.getPopularProducts(8),
    enabled: !isAuth || (isAuth && recommendedIds !== undefined && recommendedIds.length === 0),
  })

  const { data: recommendedProducts } = useQuery({
    queryKey: ['products', 'recommended', recommendedIds],
    queryFn: async () => {
      if (!recommendedIds?.length) return []
      const results = await Promise.allSettled(recommendedIds.slice(0, 8).map(id => productsApi.getById(id)))
      return results.filter(r => r.status === 'fulfilled').map(r => (r as PromiseFulfilledResult<Product>).value)
    },
    enabled: !!recommendedIds?.length,
  })

  const { data: popularProducts } = useQuery({
    queryKey: ['products', 'popular', popularIds],
    queryFn: async () => {
      if (!popularIds?.length) return []
      const results = await Promise.allSettled(popularIds.slice(0, 4).map(id => productsApi.getById(id)))
      return results.filter(r => r.status === 'fulfilled').map(r => (r as PromiseFulfilledResult<Product>).value)
    },
    enabled: !!popularIds?.length,
  })

  const aiRecommendations = recommendedProducts?.length ? recommendedProducts : popularProducts
  const isPersonalized = !!recommendedProducts?.length

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchQ.trim()) navigate(`/products?q=${encodeURIComponent(searchQ.trim())}`)
  }

  return (
    <div className="space-y-12">
      <section className="relative overflow-hidden rounded-[2rem] bg-slate-950 text-white shadow-2xl shadow-slate-300">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_10%,rgba(20,184,166,.32),transparent_30%),radial-gradient(circle_at_80%_20%,rgba(251,146,60,.22),transparent_28%)]" />
        <div className="relative grid min-h-[520px] items-center gap-10 p-6 sm:p-10 lg:grid-cols-[1.05fr_.95fr] lg:p-14">
          <div className="max-w-2xl space-y-7">
            <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-1.5 text-xs font-semibold text-teal-100 backdrop-blur">
              <Sparkles className="h-3.5 w-3.5" />
              Neo4j öneri motoru · Tool calling asistan · Iyzico ödeme
            </div>
            <div className="space-y-4">
              <h1 className="text-4xl font-black leading-[1.02] tracking-tight sm:text-6xl">
                Alışverişi arama kutusundan çıkar.
              </h1>
              <p className="max-w-xl text-base leading-7 text-slate-300 sm:text-lg">
                Ürünleri klasik filtrelerle keşfet, AI asistana siparişini sor, geçmişine göre öneri al ve ödeme akışını uçtan uca takip et.
              </p>
            </div>

            <form onSubmit={handleSearch} className="flex max-w-2xl flex-col gap-3 rounded-2xl border border-white/10 bg-white/10 p-2 backdrop-blur sm:flex-row">
              <div className="relative flex-1">
                <Sparkles className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-teal-400" />
                <input
                  type="text"
                  placeholder="Örn. gaming mouse altı bin lira, hafif seyahat laptop, gürültü engelleyen kulaklık"
                  value={searchQ}
                  onChange={(e) => setSearchQ(e.target.value)}
                  className="focus-ring h-12 w-full rounded-xl border border-white/10 bg-white pl-11 pr-4 text-sm font-medium text-slate-950 placeholder:text-slate-400"
                />
              </div>
              <Button type="submit" size="lg" className="bg-teal-500 text-slate-950 hover:bg-teal-400 shrink-0">
                <Sparkles className="h-4 w-4" />
                AI ile Ara
              </Button>
            </form>
            <p className="text-xs text-slate-400 -mt-1">
              pgvector semantik arama — anahtar kelime yerine anlam bazlı sonuçlar
            </p>

            <div className="grid max-w-xl grid-cols-3 gap-3 text-sm">
              {[
                ['80+', 'ürün'],
                ['5', 'AI tool'],
                ['24/7', 'asistan'],
              ].map(([value, label]) => (
                <div key={label} className="rounded-2xl border border-white/10 bg-white/10 p-4">
                  <p className="text-2xl font-black text-white">{value}</p>
                  <p className="text-slate-300">{label}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="relative hidden lg:block">
            <div className="absolute inset-x-8 bottom-8 h-32 rounded-full bg-teal-400/20 blur-3xl" />
            <div className="relative mx-auto max-w-md rounded-[2rem] border border-white/10 bg-white/10 p-8 shadow-2xl backdrop-blur">
              <img src={heroImage} alt="AI commerce layers" className="mx-auto h-72 w-auto object-contain" />
              <div className="mt-6 grid grid-cols-2 gap-3">
                <div className="rounded-2xl bg-white p-4 text-slate-950">
                  <Bot className="mb-3 h-5 w-5 text-teal-700" />
                  <p className="text-sm font-bold">Asistan hazır</p>
                  <p className="text-xs text-slate-500">Sipariş ve öneri tool’ları bağlı</p>
                </div>
                <div className="rounded-2xl bg-orange-100 p-4 text-orange-950">
                  <CreditCard className="mb-3 h-5 w-5" />
                  <p className="text-sm font-bold">Ödeme güvenli</p>
                  <p className="text-xs text-orange-800">Backend fiyat doğrulamalı</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {trustItems.map(({ icon: Icon, title, desc }) => (
          <div key={title} className="surface rounded-2xl p-5">
            <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-50 text-teal-700">
              <Icon className="h-5 w-5" />
            </div>
            <h2 className="font-bold text-slate-950">{title}</h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">{desc}</p>
          </div>
        ))}
      </section>

      {/* AI Öneri Bölümü — giriş yapmışsa kişisel, yapmamışsa popüler */}
      {aiRecommendations && aiRecommendations.length > 0 && (
        <section className="space-y-5 rounded-2xl border border-purple-100 bg-gradient-to-br from-purple-50 via-white to-blue-50 p-6">
          <div className="flex items-end justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="mt-0.5 flex h-10 w-10 items-center justify-center rounded-2xl bg-purple-600 text-white shadow-lg shadow-purple-200">
                <Brain className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-black tracking-tight text-slate-950">
                    {isPersonalized ? 'Senin için seçtiklerimiz' : 'Şu an popüler'}
                  </h2>
                  <Badge className="bg-purple-100 text-purple-700 border-purple-200 hover:bg-purple-100">
                    {isPersonalized ? 'AI Kişisel Öneri' : 'Neo4j Graf'}
                  </Badge>
                </div>
                <p className="text-sm text-slate-500 mt-0.5">
                  {isPersonalized
                    ? 'Satın alma ve görüntüleme geçmişine göre Neo4j collaborative filtering ile önerildi'
                    : 'En çok satın alınan ürünler — giriş yap, kişisel öneriler al'}
                </p>
              </div>
            </div>
            <Button variant="outline" className="shrink-0 border-purple-200 text-purple-700 hover:bg-purple-50" asChild>
              <Link to="/chat">
                <Bot className="h-3.5 w-3.5 mr-1.5" />
                Asistan'a sor
              </Link>
            </Button>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {aiRecommendations.slice(0, 4).map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
          {!isAuth && (
            <div className="flex items-center justify-center gap-3 rounded-xl border border-dashed border-purple-300 py-3 text-sm text-purple-600">
              <Brain className="h-4 w-4" />
              <span>
                <Link to="/login" className="font-semibold hover:underline">Giriş yap</Link>
                {' '}veya{' '}
                <Link to="/register" className="font-semibold hover:underline">üye ol</Link>
                {' '}— geçmişine özel kişiselleştirilmiş öneriler al
              </span>
            </div>
          )}
        </section>
      )}

      <section className="space-y-5">
        <div className="flex items-end justify-between gap-4">
          <div>
            <p className="eyebrow">Katalog</p>
            <h2 className="text-2xl font-black tracking-tight text-slate-950">Öne çıkan ürünler</h2>
          </div>
          <Button variant="outline" asChild>
            <Link to="/products">
              Tümünü Gör
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
        </div>
        {isLoading ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {Array(8).fill(0).map((_, i) => <Skeleton key={i} className="h-72 rounded-2xl" />)}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {products?.content.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </section>

      <section className="grid gap-4 rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/70 md:grid-cols-[1fr_auto] md:items-center md:p-8">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-950 text-white">
            <Boxes className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-xl font-black tracking-tight text-slate-950">AI asistanla alışverişi hızlandır</h2>
            <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">
              “Geçmiş siparişlerime göre öner”, “ORD numaram nerede?” veya “30 bin altı monitör bul” gibi gerçek aksiyonlar tetiklenir.
            </p>
          </div>
        </div>
        <Button asChild size="lg">
          <Link to="/chat">
            <Bot className="h-4 w-4" />
            Asistanı Aç
          </Link>
        </Button>
      </section>
    </div>
  )
}
