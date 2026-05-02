import { useState, useRef } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ArrowLeft, Store, CheckCircle, XCircle, Loader2, Sparkles } from 'lucide-react'
import { authApi } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent } from '@/components/ui/card'

const schema = z.object({
  firstName: z.string().min(2, 'Ad en az 2 karakter'),
  lastName: z.string().min(2, 'Soyad en az 2 karakter'),
  email: z.string().email('Geçerli bir e-posta girin'),
  password: z.string().min(8, 'Şifre en az 8 karakter'),
  confirmPassword: z.string(),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Şifreler eşleşmiyor',
  path: ['confirmPassword'],
})

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

type FormData = z.infer<typeof schema>
type EmailStatus = 'idle' | 'checking' | 'available' | 'taken'

export function RegisterPage() {
  const navigate = useNavigate()
  const [emailStatus, setEmailStatus] = useState<EmailStatus>('idle')

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: () => {
      toast.success('Hesabınız oluşturuldu! Lütfen e-postanızı doğrulayın.', { duration: 6000 })
      navigate('/login')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message ?? 'Kayıt sırasında bir hata oluştu'
      toast.error(msg)
    },
  })

  /**
   * Akış:
   *   "a"           → format geçersiz → istek yok, idle
   *   "ali@"        → format geçersiz → istek yok, idle
   *   "ali@gmail"   → format geçersiz → istek yok, idle
   *   "ali@gmail.com" → format geçerli → 500ms bekle → backend'e sor → checking / available / taken
   */
  const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const email = e.target.value.trim()

    // Önceki zamanlayıcıyı iptal et
    if (timerRef.current) clearTimeout(timerRef.current)

    // Format geçersizse temizle ve çık ve backend'e gitme
    if (!email || !EMAIL_REGEX.test(email)) {
      setEmailStatus('idle')
      return
    }

    // Format geçerli ise spinner göster, 500ms bekle
    setEmailStatus('checking')

    timerRef.current = setTimeout(async () => {
      try {
        const result = await authApi.checkEmail(email)
        setEmailStatus(result.exists ? 'taken' : 'available')
      } catch {
        // Ağ hatası vb. — sessizce idle'a dön, formu engelleme
        setEmailStatus('idle')
      }
    }, 500)
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_80%_10%,#fed7aa,transparent_25%),linear-gradient(135deg,#f8fafc,#ccfbf1)] p-4">
      <div className="grid w-full max-w-5xl overflow-hidden rounded-[2rem] border border-white/70 bg-white/80 shadow-2xl shadow-slate-300/70 backdrop-blur lg:grid-cols-[1.05fr_.95fr]">
        <div className="p-6 sm:p-10">
        <div className="mb-4">
          <Link to="/" className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-500 transition-colors hover:text-teal-700">
            <ArrowLeft className="h-4 w-4" />
            Anasayfaya Dön
          </Link>
        </div>
        <div className="mb-8">
          <p className="eyebrow">Kayıt</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight text-slate-950">n12 hesabını oluştur</h1>
          <p className="mt-2 text-sm text-slate-500">Kişisel öneriler ve sipariş takibi için hesap aç.</p>
        </div>
        <Card className="border-0 bg-transparent shadow-none">
          <CardContent>
            <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Ad</Label>
                  <Input placeholder="Ali" {...register('firstName')} />
                  {errors.firstName && <p className="text-xs text-red-500">{errors.firstName.message}</p>}
                </div>
                <div className="space-y-2">
                  <Label>Soyad</Label>
                  <Input placeholder="Yılmaz" {...register('lastName')} />
                  {errors.lastName && <p className="text-xs text-red-500">{errors.lastName.message}</p>}
                </div>
              </div>

              {/* E-posta — format-aware Bloom Filter kontrolü */}
              <div className="space-y-1.5">
                <Label>E-posta</Label>
                <div className="relative">
                  <Input
                    type="email"
                    placeholder="ornek@email.com"
                    className={
                      emailStatus === 'taken'     ? 'border-red-400 pr-9'   :
                      emailStatus === 'available' ? 'border-green-400 pr-9' :
                      emailStatus === 'checking'  ? 'border-blue-300 pr-9'  : ''
                    }
                    {...register('email')}
                    onChange={(e) => {
                      register('email').onChange(e)
                      handleEmailChange(e)
                    }}
                  />
                  <div className="absolute inset-y-0 right-2.5 flex items-center pointer-events-none">
                    {emailStatus === 'checking'  && <Loader2    className="h-4 w-4 text-blue-400 animate-spin" />}
                    {emailStatus === 'available' && <CheckCircle className="h-4 w-4 text-green-500" />}
                    {emailStatus === 'taken'     && <XCircle     className="h-4 w-4 text-red-500" />}
                  </div>
                </div>

                {/* Zod validasyon hatası */}
                {errors.email && <p className="text-xs text-red-500">{errors.email.message}</p>}

                {/* Bloom Filter sonuç mesajları — sadece format geçerliyse göster */}
                {emailStatus === 'checking' && (
                  <p className="text-xs text-blue-500">Kontrol ediliyor...</p>
                )}
                {emailStatus === 'available' && !errors.email && (
                  <p className="text-xs text-green-600 flex items-center gap-1">
                    <CheckCircle className="h-3 w-3" /> Bu e-posta kullanılabilir
                  </p>
                )}
                {emailStatus === 'taken' && !errors.email && (
                  <p className="text-xs text-red-500">
                    Bu e-posta zaten kayıtlı.{' '}
                    <Link to="/login" className="underline font-medium">Giriş yapmak ister misiniz?</Link>
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label>Şifre</Label>
                <Input type="password" placeholder="En az 8 karakter" {...register('password')} />
                {errors.password && <p className="text-xs text-red-500">{errors.password.message}</p>}
              </div>
              <div className="space-y-2">
                <Label>Şifre Tekrar</Label>
                <Input type="password" placeholder="Şifrenizi tekrar girin" {...register('confirmPassword')} />
                {errors.confirmPassword && <p className="text-xs text-red-500">{errors.confirmPassword.message}</p>}
              </div>

              <Button
                type="submit"
                className="h-12 w-full rounded-2xl"
                disabled={mutation.isPending || emailStatus === 'taken'}
              >
                {mutation.isPending ? 'Hesap oluşturuluyor...' : 'Üye Ol'}
              </Button>
            </form>

            {/* Kayıt sonrası bilgi notu */}
            <div className="mt-4 rounded-2xl border border-teal-100 bg-teal-50 p-3">
              <p className="text-xs leading-relaxed text-teal-800">
                Kayıt sonrası e-posta adresinize doğrulama linki gönderilecektir.
              </p>
            </div>

            <p className="text-center text-sm text-muted-foreground mt-4">
              Zaten hesabınız var mı?{' '}
              <Link to="/login" className="font-semibold text-teal-700 hover:underline">Giriş yap</Link>
            </p>
          </CardContent>
        </Card>
        </div>
        <div className="hidden bg-slate-950 p-10 text-white lg:flex lg:flex-col">
          <div className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-500 text-slate-950">
              <Store className="h-5 w-5" />
            </span>
            <div>
              <p className="text-lg font-black">n12</p>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-300">AI Commerce</p>
            </div>
          </div>
          <div className="mt-auto space-y-5">
            <Sparkles className="h-9 w-9 text-teal-300" />
            <h2 className="text-4xl font-black leading-tight tracking-tight">Kişisel alışveriş deneyimin burada başlar.</h2>
            <p className="text-sm leading-6 text-slate-300">
              Sipariş geçmişinden öneri, semantik ürün arama ve gerçek zamanlı asistan deneyimi için hesabını aç.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
