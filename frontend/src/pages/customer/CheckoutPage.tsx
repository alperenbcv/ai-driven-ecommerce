import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { CreditCard, MapPin, Package, Plus, Check, ChevronDown, ChevronUp } from 'lucide-react'
import { cartApi } from '@/api/cart'
import { ordersApi } from '@/api/orders'
import { authApi } from '@/api/auth'
import { formatPrice } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import { useCartStore } from '@/stores/cartStore'
import { useUser } from '@/stores/authStore'

const schema = z.object({
  fullName: z.string().min(3, 'Ad soyad zorunlu'),
  phone: z.string().min(10, 'Geçerli telefon girin'),
  city: z.string().min(2, 'Şehir zorunlu'),
  district: z.string().min(2, 'İlçe zorunlu'),
  fullAddress: z.string().min(10, 'Açık adres zorunlu'),
})

type FormData = z.infer<typeof schema>

export function CheckoutPage() {
  const navigate = useNavigate()
  const user = useUser()
  const { cart, setCart } = useCartStore()

  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [useNewAddress, setUseNewAddress] = useState(false)
  const [showAddressForm, setShowAddressForm] = useState(false)

  const { data: cartData } = useQuery({
    queryKey: ['cart'],
    queryFn: cartApi.getCart,
    enabled: !cart,
  })

  const { data: addresses } = useQuery({
    queryKey: ['addresses'],
    queryFn: authApi.getAddresses,
  })

  const { register, handleSubmit, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() },
  })

  useEffect(() => {
    if (!addresses?.length) { setUseNewAddress(true); return }

    // İlk kez yüklendiyse default adresi seç
    if (selectedAddressId === null) {
      const def = addresses.find(a => a.defaultAddress) ?? addresses[0]
      setSelectedAddressId(def.id)
    }
  }, [addresses])

  useEffect(() => {
    if (selectedAddressId == null || useNewAddress) return
    const addr = addresses?.find(a => a.id === selectedAddressId)
    if (!addr) return
    setValue('fullName', `${addr.firstName} ${addr.lastName}`)
    setValue('phone', addr.phone)
    setValue('city', addr.city)
    setValue('district', addr.district)
    setValue('fullAddress', addr.fullAddress)
  }, [selectedAddressId, useNewAddress, addresses])

  const items = cart?.items ?? cartData?.items ?? []
  const total = cart?.totalAmount ?? cartData?.totalAmount ?? 0

  const createOrder = useMutation({
    mutationFn: (addr: FormData) => ordersApi.create({
      userEmail: user!.email,
      items: items.map(i => ({
        productId: i.productId,
        listingId: i.listingId,
        sellerId: i.sellerId,
        productName: i.productName,
        unitPrice: i.unitPrice,
        quantity: i.quantity,
      })),
      shippingAddress: addr,
    }),
    onSuccess: async (order) => {
      await cartApi.clearCart()
      setCart(null)
      toast.success('Siparişiniz oluşturuldu! Ödeme sayfasına yönlendiriliyorsunuz...')
      navigate(`/payment/${order.orderNumber}`)
    },
    onError: () => toast.error('Sipariş oluşturulamadı. Lütfen tekrar deneyin.'),
  })

  if (items.length === 0) {
    navigate('/cart')
    return null
  }

  const hasAddresses = !!addresses?.length

  return (
    <div className="grid lg:grid-cols-3 gap-8">
      <div className="lg:col-span-2 space-y-6">
        <h1 className="text-2xl font-bold">Siparişi Tamamla</h1>

        {/* Teslimat Adresi */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <MapPin className="h-5 w-5 text-teal-700" /> Teslimat Adresi
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">

            {/* Kayıtlı adresler */}
            {hasAddresses && !useNewAddress && (
              <div className="space-y-2">
                {addresses!.map(addr => (
                  <button
                    key={addr.id}
                    type="button"
                    onClick={() => setSelectedAddressId(addr.id)}
                    className={`w-full text-left p-3 rounded-lg border text-sm transition-colors ${
                      selectedAddressId === addr.id
                        ? 'border-teal-300 bg-teal-50'
                        : 'hover:bg-gray-50 border-gray-200'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="flex items-center gap-2 font-medium">
                          {addr.title}
                          {addr.defaultAddress && (
                            <Badge variant="secondary" className="text-xs py-0">Varsayılan</Badge>
                          )}
                        </div>
                        <div className="text-muted-foreground mt-0.5">
                          {addr.firstName} {addr.lastName} · {addr.phone}
                        </div>
                        <div className="text-muted-foreground">
                          {addr.fullAddress}, {addr.district} / {addr.city}
                        </div>
                      </div>
                      {selectedAddressId === addr.id && (
                        <Check className="h-5 w-5 text-teal-700 flex-shrink-0 mt-0.5" />
                      )}
                    </div>
                  </button>
                ))}

                <button
                  type="button"
                  onClick={() => { setUseNewAddress(true); setShowAddressForm(true) }}
                  className="w-full flex items-center gap-2 p-3 rounded-lg border border-dashed text-sm text-muted-foreground hover:bg-gray-50 transition-colors"
                >
                  <Plus className="h-4 w-4" /> Farklı bir adres kullan
                </button>
              </div>
            )}

            {/* Manuel adres formu başlığı (kayıtlı adres yoksa ya da "farklı adres" seçildiyse) */}
            {(useNewAddress || !hasAddresses) && (
              <div>
                {hasAddresses && (
                  <button
                    type="button"
                    onClick={() => setUseNewAddress(false)}
                    className="mb-3 flex items-center gap-1 text-sm font-semibold text-teal-700 hover:underline"
                  >
                    <MapPin className="h-3.5 w-3.5" /> Kayıtlı adreslerime dön
                  </button>
                )}
              </div>
            )}

            {/* Form: kayıtlı adres yoksa veya "farklı adres" modundaysa göster;
                kayıtlı adres seçiliyse collapse göster */}
            {hasAddresses && !useNewAddress ? (
              <div>
                <button
                  type="button"
                  className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
                  onClick={() => setShowAddressForm(v => !v)}
                >
                  Adres detaylarını görüntüle
                  {showAddressForm ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
                </button>
                {showAddressForm && <AddressFormFields register={register} errors={errors} />}
              </div>
            ) : (
              <AddressFormFields register={register} errors={errors} />
            )}

            <form id="checkout-form" onSubmit={handleSubmit((d) => createOrder.mutate(d))} />
          </CardContent>
        </Card>

        {/* Ödeme Bilgisi */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <CreditCard className="h-5 w-5 text-teal-700" /> Ödeme
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-1 rounded-2xl bg-teal-50 p-4 text-sm text-teal-800">
              <p className="font-medium">Güvenli Iyzico Ödeme</p>
              <p className="text-teal-700">
                "Ödemeye Geç"e bastıktan sonra Iyzico'nun güvenli ödeme formuna yönlendirileceksiniz.
                Kart bilgileriniz bizimle paylaşılmaz — 256-bit SSL şifreleme ile korunur.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Özet */}
      <div>
        <Card className="sticky top-24">
          <CardContent className="p-6 space-y-4">
            <h2 className="font-bold text-lg flex items-center gap-2">
              <Package className="h-5 w-5" /> Sipariş Özeti
            </h2>
            <div className="space-y-2 max-h-60 overflow-y-auto">
              {items.map(item => (
                <div key={`${item.productId}-${item.listingId}`} className="flex justify-between text-sm">
                  <span className="text-muted-foreground line-clamp-1 flex-1">{item.productName} × {item.quantity}</span>
                  <span className="ml-2 font-medium">{formatPrice(item.unitPrice * item.quantity)}</span>
                </div>
              ))}
            </div>
            <Separator />
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Kargo</span>
              <span className="text-green-600">Ücretsiz</span>
            </div>
            <div className="flex justify-between font-bold text-lg">
              <span>Toplam</span>
              <span className="text-teal-700">{formatPrice(total)}</span>
            </div>
            <Button
              form="checkout-form"
              type="submit"
              size="lg"
              className="w-full"
              disabled={createOrder.isPending}
            >
              {createOrder.isPending ? 'Sipariş oluşturuluyor...' : 'Ödemeye Geç →'}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function AddressFormFields({ register, errors }: { register: any; errors: any }) {
  return (
    <div className="grid grid-cols-2 gap-4 mt-3">
      <div className="col-span-2 space-y-2">
        <Label>Ad Soyad</Label>
        <Input placeholder="Ali Yılmaz" {...register('fullName')} />
        {errors.fullName && <p className="text-xs text-red-500">{errors.fullName.message}</p>}
      </div>
      <div className="space-y-2">
        <Label>Telefon</Label>
        <Input placeholder="05XX XXX XX XX" {...register('phone')} />
        {errors.phone && <p className="text-xs text-red-500">{errors.phone.message}</p>}
      </div>
      <div className="space-y-2">
        <Label>Şehir</Label>
        <Input placeholder="İstanbul" {...register('city')} />
        {errors.city && <p className="text-xs text-red-500">{errors.city.message}</p>}
      </div>
      <div className="col-span-2 space-y-2">
        <Label>İlçe</Label>
        <Input placeholder="Kadıköy" {...register('district')} />
        {errors.district && <p className="text-xs text-red-500">{errors.district.message}</p>}
      </div>
      <div className="col-span-2 space-y-2">
        <Label>Açık Adres</Label>
        <Input placeholder="Mahalle, cadde, sokak, no, daire..." {...register('fullAddress')} />
        {errors.fullAddress && <p className="text-xs text-red-500">{errors.fullAddress.message}</p>}
      </div>
    </div>
  )
}
