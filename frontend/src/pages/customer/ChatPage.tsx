import { useState, useRef, useEffect } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  Send, Bot, User, Sparkles, Trash2, ShoppingCart, Star,
  Search, Package, Info, ChevronDown, ChevronUp,
  BookOpen, ShieldCheck, Tag,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { aiApi } from '@/api/ai'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { toast } from 'sonner'

interface ProductCard {
  id: number
  name: string
  description?: string
  price: number
  categoryName?: string
  brandName?: string
  averageRating?: number
  reviewCount?: number
}

interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  products?: ProductCard[]
  streaming?: boolean
}

function ProductCardItem({ product }: { product: ProductCard }) {
  return (
    <Link
      to={`/products/${product.id}`}
      className="group block rounded-2xl border border-slate-200 bg-white p-3 shadow-sm shadow-slate-200/60 transition-all duration-200 hover:-translate-y-0.5 hover:border-teal-200 hover:shadow-lg"
    >
      {/* Başlık */}
      <p className="line-clamp-2 text-sm font-bold leading-snug text-slate-950 transition-colors group-hover:text-teal-700">
        {product.name}
      </p>

      {/* Marka / Kategori */}
      {(product.brandName || product.categoryName) && (
        <p className="text-xs text-muted-foreground mt-0.5">
          {product.brandName ?? product.categoryName}
        </p>
      )}

      {/* Yıldız */}
      {product.averageRating != null && product.averageRating > 0 && (
        <div className="flex items-center gap-1 mt-1">
          <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
          <span className="text-xs text-slate-500">
            {product.averageRating.toFixed(1)}
            {product.reviewCount ? ` (${product.reviewCount})` : ''}
          </span>
        </div>
      )}

      {/* Fiyat + CTA */}
      <div className="flex items-center justify-between mt-2">
        <span className="text-sm font-black text-teal-700">
          {product.price.toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
        </span>
        <span className="flex items-center gap-1 text-xs font-medium text-slate-500 opacity-0 transition-opacity group-hover:opacity-100">
          <ShoppingCart className="h-3 w-3" /> İncele
        </span>
      </div>
    </Link>
  )
}

function AssistantBubble({ msg }: { msg: Message }) {
  const hasProducts = msg.products && msg.products.length > 0

  return (
    <div className="flex gap-3">
      <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-2xl bg-slate-950">
        <Bot className="h-4 w-4 text-white" />
      </div>

      <div className="flex max-w-[86%] flex-col gap-2">
        {/* Metin yanıtı */}
        {(msg.content || msg.streaming) && (
          <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-relaxed text-slate-700 shadow-sm shadow-slate-200/60 whitespace-pre-wrap">
            {msg.content}
            {msg.streaming && <span className="ml-0.5 inline-block h-4 w-1.5 animate-pulse bg-teal-600 align-[-2px]" />}
          </div>
        )}

        {/* Ürün kartları grid */}
        {hasProducts && (
          <div>
            <div className="flex items-center gap-2 mb-2 px-1">
              <Sparkles className="h-3.5 w-3.5 text-teal-600" />
              <span className="text-xs font-medium text-slate-500">
                {msg.products!.length} ürün bulundu
              </span>
              <Badge className="bg-teal-50 py-0 text-xs text-teal-800 hover:bg-teal-50">AI Önerisi</Badge>
            </div>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {msg.products!.map(p => (
                <ProductCardItem key={p.id} product={p} />
              ))}
            </div>
          </div>
        )}

        <span className="px-1 text-xs text-slate-400">
          {msg.timestamp.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}
        </span>
      </div>
    </div>
  )
}

export function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: 'Merhaba! Ben n12 AI asistanıyım. Ürün tavsiyesi, sipariş bilgisi veya alışverişle ilgili her konuda yardımcı olabilirim. Nasıl yardımcı olabilirim?',
      timestamp: new Date(),
    },
  ])
  const [input, setInput] = useState('')
  const [sessionId, setSessionId] = useState<string | undefined>()
  const [capabilitiesOpen, setCapabilitiesOpen] = useState(false)
  const [isTyping, setIsTyping] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const typingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => () => {
    if (typingTimerRef.current) {
      clearInterval(typingTimerRef.current)
    }
  }, [])

  const typeAssistantReply = (reply: string, products: ProductCard[]) => {
    if (typingTimerRef.current) {
      clearInterval(typingTimerRef.current)
    }

    setIsTyping(true)
    setMessages(prev => [
      ...prev,
      {
        role: 'assistant',
        content: '',
        timestamp: new Date(),
        products: [],
        streaming: true,
      },
    ])

    let index = 0
    const text = reply || 'Size nasıl yardımcı olabilirim?'

    typingTimerRef.current = setInterval(() => {
      index += 2
      const nextText = text.slice(0, index)

      setMessages(prev => {
        const next = [...prev]
        const lastIndex = next.length - 1
        const last = next[lastIndex]

        if (!last || last.role !== 'assistant' || !last.streaming) {
          return prev
        }

        next[lastIndex] = {
          ...last,
          content: nextText,
          products: index >= text.length ? products : [],
          streaming: index < text.length,
        }

        return next
      })

      if (index >= text.length) {
        if (typingTimerRef.current) {
          clearInterval(typingTimerRef.current)
          typingTimerRef.current = null
        }
        setIsTyping(false)
      }
    }, 18)
  }

  const send = useMutation({
    mutationFn: (msg: string) => aiApi.chat(msg, sessionId),
    onSuccess: (data) => {
      setSessionId(data.sessionId)
      typeAssistantReply(data.reply ?? '', data.products ?? [])
    },
    onError: () => {
      setIsTyping(false)
      toast.error('Yanıt alınamadı')
    },
  })

  const handleSend = () => {
    if (!input.trim() || send.isPending || isTyping) return
    const msg = input.trim()
    setMessages(prev => [...prev, { role: 'user', content: msg, timestamp: new Date() }])
    setInput('')
    send.mutate(msg)
  }

  const handleClear = async () => {
    if (typingTimerRef.current) {
      clearInterval(typingTimerRef.current)
      typingTimerRef.current = null
    }
    setIsTyping(false)
    if (sessionId) await aiApi.clearSession(sessionId)
    setSessionId(undefined)
    setMessages([
      { role: 'assistant', content: 'Konuşma sıfırlandı. Size nasıl yardımcı olabilirim?', timestamp: new Date() },
    ])
  }

  const suggestions = [
    'Stokta hangi kitaplar var?',
    'Gaming laptop önerir misin?',
    'Sony kulaklık fiyatları nedir?',
    'İade politikanız nedir?',
  ]

  return (
    <div className="mx-auto grid h-[calc(100vh-8rem)] max-w-6xl gap-5 lg:grid-cols-[320px_1fr]">
      {/* Başlık */}
      <aside className="hidden rounded-[2rem] bg-slate-950 p-5 text-white shadow-xl shadow-slate-300/70 lg:flex lg:flex-col">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-500 text-slate-950">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h1 className="font-black">AI Asistan</h1>
            <p className="text-xs text-slate-300">Tool calling · Canlı backend</p>
          </div>
        </div>
        <div className="mt-6 space-y-3">
          {[
            { label: 'Ürün ara', icon: Search },
            { label: 'Sipariş takip', icon: Package },
            { label: 'Kişisel öneri', icon: Sparkles },
            { label: 'Platform politikası', icon: BookOpen },
          ].map(({ label, icon: Icon }) => (
            <div key={label} className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/10 p-3 text-sm font-semibold text-slate-100">
              <Icon className="h-4 w-4 text-teal-300" />
              {label}
            </div>
          ))}
        </div>

        <div className="mt-auto rounded-2xl border border-white/10 bg-white/10 p-4 text-xs leading-5 text-slate-300">
          AI cevapları canlı servislerden gelen ürün, sipariş ve öneri verileriyle desteklenir.
        </div>
      </aside>

      <section className="flex min-h-0 flex-col rounded-[2rem] border border-slate-200 bg-white shadow-xl shadow-slate-200/70">
        <div className="flex items-center justify-between border-b border-slate-100 p-4">
          <div>
            <p className="eyebrow">n12 Copilot</p>
            <h1 className="text-xl font-black tracking-tight text-slate-950">AI Alışveriş Asistanı</h1>
          </div>
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="sm" onClick={() => setCapabilitiesOpen(v => !v)} className="text-xs">
              <Info className="h-3.5 w-3.5" />
              Yetenekler
              {capabilitiesOpen ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            </Button>
            <Button variant="ghost" size="sm" onClick={handleClear}>
              <Trash2 className="h-4 w-4" /> Sıfırla
            </Button>
          </div>
        </div>

      {/* Yetenekler paneli */}
      {capabilitiesOpen && (
        <div className="m-4 mb-0 space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm">
          <div className="flex items-center gap-2 font-semibold text-teal-800">
            <Sparkles className="h-4 w-4" />
            <span>AI Asistan Yetenekleri</span>
              <Badge className="ml-auto border-teal-200 bg-teal-50 text-xs font-normal text-teal-800">
              5 Tool · Gerçek Zamanlı
            </Badge>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {/* Tool 1 */}
            <div className="flex gap-3 bg-white rounded-lg p-3 border shadow-sm">
              <div className="h-8 w-8 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                <Search className="h-4 w-4 text-blue-600" />
              </div>
              <div>
                <p className="font-medium text-xs">Ürün Arama</p>
                <p className="text-xs text-muted-foreground leading-snug mt-0.5">
                  Canlı ürün veritabanında arama yapar. Marka, kategori veya isimle sorgulayabilirsin.
                </p>
                <div className="flex flex-wrap gap-1 mt-1.5">
                  {['kitap', 'Sony kulaklık', 'gaming laptop'].map(t => (
                    <span key={t} className="text-[10px] bg-blue-50 text-blue-600 rounded px-1.5 py-0.5 border border-blue-100">{t}</span>
                  ))}
                </div>
              </div>
            </div>

            {/* Tool 2 */}
            <div className="flex gap-3 bg-white rounded-lg p-3 border shadow-sm">
              <div className="h-8 w-8 rounded-lg bg-purple-100 flex items-center justify-center flex-shrink-0">
                <Tag className="h-4 w-4 text-purple-600" />
              </div>
              <div>
                <p className="font-medium text-xs">Ürün Detayı</p>
                <p className="text-xs text-muted-foreground leading-snug mt-0.5">
                  Belirli bir ürünün fiyatını, marka, kategori ve puan bilgisini getirir.
                </p>
                <div className="flex flex-wrap gap-1 mt-1.5">
                  {['fiyat', 'puan', 'stok durumu'].map(t => (
                    <span key={t} className="text-[10px] bg-purple-50 text-purple-600 rounded px-1.5 py-0.5 border border-purple-100">{t}</span>
                  ))}
                </div>
              </div>
            </div>

            {/* Tool 3 */}
            <div className="flex gap-3 bg-white rounded-lg p-3 border shadow-sm">
              <div className="h-8 w-8 rounded-lg bg-green-100 flex items-center justify-center flex-shrink-0">
                <Package className="h-4 w-4 text-green-600" />
              </div>
              <div>
                <p className="font-medium text-xs">Sipariş Takibi</p>
                <p className="text-xs text-muted-foreground leading-snug mt-0.5">
                  Sipariş numaranı vererek kargonu takip edebilirsin. JWT ile kimliğin doğrulanır.
                </p>
                <div className="flex flex-wrap gap-1 mt-1.5">
                  {['ORD-... nerede?', 'kargo takip', 'teslim tarihi'].map(t => (
                    <span key={t} className="text-[10px] bg-green-50 text-green-600 rounded px-1.5 py-0.5 border border-green-100">{t}</span>
                  ))}
                </div>
              </div>
            </div>

            {/* Tool 4 */}
            <div className="flex gap-3 bg-white rounded-lg p-3 border shadow-sm">
              <div className="h-8 w-8 rounded-lg bg-orange-100 flex items-center justify-center flex-shrink-0">
                <BookOpen className="h-4 w-4 text-orange-600" />
              </div>
              <div>
                <p className="font-medium text-xs">Platform Politikaları</p>
                <p className="text-xs text-muted-foreground leading-snug mt-0.5">
                  İade, kargo süresi, ödeme yöntemleri ve güvenlik hakkında bilgi verir.
                </p>
                <div className="flex flex-wrap gap-1 mt-1.5">
                  {['iade', 'kargo ücreti', 'ödeme'].map(t => (
                    <span key={t} className="text-[10px] bg-orange-50 text-orange-600 rounded px-1.5 py-0.5 border border-orange-100">{t}</span>
                  ))}
                </div>
              </div>
            </div>

            {/* Tool 5 */}
            <div className="flex gap-3 bg-white rounded-lg p-3 border shadow-sm">
              <div className="h-8 w-8 rounded-lg bg-pink-100 flex items-center justify-center flex-shrink-0">
                <Sparkles className="h-4 w-4 text-pink-600" />
              </div>
              <div>
                <p className="font-medium text-xs">Kişisel Öneri</p>
                <p className="text-xs text-muted-foreground leading-snug mt-0.5">
                  Kullanıcının geçmiş davranışlarından gelen öneri servisini chatbot içinde tetikler.
                </p>
                <div className="flex flex-wrap gap-1 mt-1.5">
                  {['bana özel', 'benim için seç', 'geçmişime göre'].map(t => (
                    <span key={t} className="text-[10px] bg-pink-50 text-pink-600 rounded px-1.5 py-0.5 border border-pink-100">{t}</span>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Teknik not */}
          <div className="flex items-start gap-2 text-[11px] text-muted-foreground bg-white/60 rounded-lg px-3 py-2 border">
            <ShieldCheck className="h-3.5 w-3.5 mt-0.5 text-green-500 flex-shrink-0" />
            <span>
              AI kendi varsayımlarına değil, <strong>canlı backend verilerine</strong> dayanarak yanıt verir.
              Sipariş sorgularında JWT token kimlik doğrulaması yapılır — başka kullanıcının verisine erişilemez.
            </span>
          </div>
        </div>
      )}

      {/* Mesajlar */}
      <div className="flex-1 space-y-4 overflow-y-auto p-4">
        {messages.map((msg, i) =>
          msg.role === 'assistant' ? (
            <AssistantBubble key={i} msg={msg} />
          ) : (
            <div key={i} className="flex flex-row-reverse gap-3">
              <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-2xl bg-teal-50">
                <User className="h-4 w-4 text-teal-800" />
              </div>
              <div className="flex max-w-[75%] flex-col items-end gap-1">
                <div className="rounded-2xl bg-teal-700 px-4 py-3 text-sm leading-relaxed text-white">
                  {msg.content}
                </div>
                <span className="px-1 text-xs text-slate-400">
                  {msg.timestamp.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>
            </div>
          )
        )}

        {/* Typing indicator */}
        {send.isPending && (
          <div className="flex gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-2xl bg-slate-950">
              <Bot className="h-4 w-4 text-white" />
            </div>
            <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
              <div className="flex gap-1">
                {[0, 1, 2].map(i => (
                  <div
                    key={i}
                    className="h-2 w-2 animate-bounce rounded-full bg-teal-500"
                    style={{ animationDelay: `${i * 0.15}s` }}
                  />
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Hızlı sorular — sadece ilk mesajdan sonra görünür */}
        {messages.length === 1 && !send.isPending && !isTyping && (
          <div className="flex flex-wrap gap-2 pt-2">
            {suggestions.map(s => (
              <button
                key={s}
                onClick={() => { setInput(s); }}
                className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 transition-colors hover:border-teal-200 hover:bg-teal-50 hover:text-teal-800"
              >
                {s}
              </button>
            ))}
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="flex gap-2 border-t border-slate-100 p-4">
        <Textarea
          placeholder="Mesajınızı yazın... (Örn: 'Gaming monitör önerir misin?')"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e: React.KeyboardEvent<HTMLTextAreaElement>) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              handleSend()
            }
          }}
          className="max-h-32 min-h-[52px] resize-none rounded-2xl border-slate-200 bg-slate-50"
          rows={1}
        />
        <Button
          onClick={handleSend}
          disabled={!input.trim() || send.isPending || isTyping}
          size="icon"
          className="h-12 w-12 rounded-2xl"
        >
          <Send className="h-4 w-4" />
        </Button>
      </div>
      </section>
    </div>
  )
}
