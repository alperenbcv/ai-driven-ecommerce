import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ArrowLeft, Store, MailWarning, RefreshCw, CheckCircle, Sparkles } from 'lucide-react'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent } from '@/components/ui/card'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

const schema = z.object({
  email: z.email('Geçerli bir e-posta girin'),
  password: z.string().min(6, 'Şifre en az 6 karakter olmalı'),
})

type FormData = z.infer<typeof schema>

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuthStore()
  const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null)
  const [resendSent, setResendSent] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })
  const emailField = register('email')

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      login(data.user, data.accessToken, data.refreshToken)
      toast.success('Giriş başarılı!')
      navigate('/')
    },
    onError: (err: any) => {
      const code: string = err?.response?.data?.errorCode ?? ''
      const msg: string = err?.response?.data?.message ?? 'E-posta veya şifre hatalı'

      if (code === 'EMAIL_NOT_VERIFIED') {
        // Form'dan e-posta değerini al ve doğrulama uyarısını göster
        const emailVal = (document.getElementById('email') as HTMLInputElement)?.value ?? ''
        setUnverifiedEmail(emailVal || msg)
        setResendSent(false)
      } else {
        setUnverifiedEmail(null)
        toast.error(msg)
      }
    },
  })

  const resendMutation = useMutation({
    mutationFn: (email: string) => authApi.resendVerification(email),
    onSuccess: () => {
      setResendSent(true)
      toast.success('Doğrulama maili gönderildi!')
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message ?? 'Mail gönderilemedi')
    },
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_20%_10%,#ccfbf1,transparent_28%),linear-gradient(135deg,#f8fafc,#e2e8f0)] p-4">
      <div className="grid w-full max-w-5xl overflow-hidden rounded-[2rem] border border-white/70 bg-white/80 shadow-2xl shadow-slate-300/70 backdrop-blur lg:grid-cols-[.95fr_1.05fr]">
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
            <h1 className="text-4xl font-black leading-tight tracking-tight">Akıllı alışveriş hesabına dön.</h1>
            <p className="text-sm leading-6 text-slate-300">
              Sipariş takibi, kişisel öneriler ve AI destekli ürün keşfi tek oturumda birleşir.
            </p>
          </div>
        </div>

        <div className="p-6 sm:p-10">
        <div className="mb-4">
          <Link to="/" className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-500 transition-colors hover:text-teal-700">
            <ArrowLeft className="h-4 w-4" />
            Anasayfaya Dön
          </Link>
        </div>
        <div className="mb-8">
          <p className="eyebrow">Giriş</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight text-slate-950">Tekrar hoş geldiniz</h1>
          <p className="mt-2 text-sm text-slate-500">Hesabınıza erişmek için bilgilerinizi girin.</p>
        </div>

        <Card className="border-0 bg-transparent shadow-none">
          <CardContent className="space-y-4">
            {/* E-posta doğrulama uyarısı */}
            {unverifiedEmail && (
              <Alert className="border-amber-300 bg-amber-50">
                <MailWarning className="h-4 w-4 text-amber-600" />
                <AlertTitle className="text-amber-800">E-posta doğrulanmamış</AlertTitle>
                <AlertDescription className="text-amber-700 space-y-3">
                  <p className="text-sm">
                    Giriş yapabilmek için e-posta adresinizi doğrulamanız gerekiyor.
                  </p>
                  {resendSent ? (
                    <div className="flex items-center gap-2 text-sm font-medium text-green-700">
                      <CheckCircle className="h-4 w-4" />
                      Doğrulama maili gönderildi — lütfen gelen kutunuzu kontrol edin.
                    </div>
                  ) : (
                    <Button
                      size="sm"
                      variant="outline"
                    className="border-amber-300 text-amber-800 hover:bg-amber-100"
                      disabled={resendMutation.isPending}
                      onClick={() => resendMutation.mutate(unverifiedEmail)}
                    >
                      <RefreshCw className={`h-3.5 w-3.5 mr-1.5 ${resendMutation.isPending ? 'animate-spin' : ''}`} />
                      {resendMutation.isPending ? 'Gönderiliyor...' : 'Doğrulama Maili Gönder'}
                    </Button>
                  )}
                </AlertDescription>
              </Alert>
            )}

            <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">E-posta</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="ornek@email.com"
                  {...emailField}
                  onChange={(e) => {
                    emailField.onChange(e)
                    if (unverifiedEmail) setUnverifiedEmail(null)
                  }}
                />
                {errors.email && <p className="text-xs text-red-500">{errors.email.message}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Şifre</Label>
                <Input id="password" type="password" placeholder="••••••••" {...register('password')} />
                {errors.password && <p className="text-xs text-red-500">{errors.password.message}</p>}
              </div>
              <div className="flex justify-end">
                <Link to="/forgot-password" className="text-xs font-semibold text-teal-700 hover:underline">
                  Şifremi Unuttum
                </Link>
              </div>
              <Button type="submit" className="h-12 w-full rounded-2xl" disabled={mutation.isPending}>
                {mutation.isPending ? 'Giriş yapılıyor...' : 'Giriş Yap'}
              </Button>
            </form>

            <p className="text-center text-sm text-muted-foreground">
              Hesabınız yok mu?{' '}
              <Link to="/register" className="font-semibold text-teal-700 hover:underline">
                Üye ol
              </Link>
            </p>
          </CardContent>
        </Card>
        </div>
      </div>
    </div>
  )
}
