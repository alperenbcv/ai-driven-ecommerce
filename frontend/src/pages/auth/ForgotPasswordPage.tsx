import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react'
import { authApi } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

const schema = z.object({
  email: z.email('Geçerli bir e-posta adresi girin'),
})
type FormData = z.infer<typeof schema>

export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false)
  const [sentEmail, setSentEmail] = useState('')

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: (data: FormData) => authApi.forgotPassword(data.email),
    onSuccess: (_, vars) => {
      setSentEmail(vars.email)
      setSent(true)
    },
    onError: () => toast.error('Bir hata oluştu, lütfen tekrar deneyin'),
  })

  if (sent) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-violet-50 to-indigo-50 p-4">
        <Card className="w-full max-w-md shadow-lg">
          <CardContent className="pt-10 pb-8 text-center space-y-4">
            <div className="flex justify-center">
              <div className="h-16 w-16 rounded-full bg-green-100 flex items-center justify-center">
                <CheckCircle className="h-8 w-8 text-green-600" />
              </div>
            </div>
            <h2 className="text-xl font-bold text-gray-900">Mail Gönderildi!</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Eğer <span className="font-medium text-gray-700">{sentEmail}</span> adresi sistemimizde kayıtlıysa,
              şifre sıfırlama linki gönderildi. Lütfen gelen kutunuzu (ve spam klasörünü) kontrol edin.
            </p>
            <p className="text-xs text-muted-foreground">Link 1 saat geçerlidir.</p>
            <div className="pt-2 space-y-2">
              <Button
                variant="outline"
                className="w-full"
                onClick={() => { setSent(false); setSentEmail('') }}
              >
                Tekrar Gönder
              </Button>
              <Link to="/login">
                <Button variant="ghost" className="w-full gap-2">
                  <ArrowLeft className="h-4 w-4" /> Girişe Dön
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-violet-50 to-indigo-50 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="text-center pb-2">
          <div className="flex justify-center mb-4">
            <div className="h-12 w-12 rounded-full bg-violet-100 flex items-center justify-center">
              <Mail className="h-6 w-6 text-violet-600" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold">Şifremi Unuttum</CardTitle>
          <CardDescription className="text-sm leading-relaxed">
            E-posta adresinizi girin, şifre sıfırlama linki gönderelim.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={handleSubmit(data => mutation.mutate(data))} className="space-y-4">
            <div className="space-y-1.5">
              <Label>E-posta Adresi</Label>
              <Input
                type="email"
                placeholder="ornek@email.com"
                autoFocus
                {...register('email')}
              />
              {errors.email && (
                <p className="text-xs text-red-500">{errors.email.message}</p>
              )}
            </div>
            <Button
              type="submit"
              className="w-full bg-violet-600 hover:bg-violet-700"
              disabled={mutation.isPending}
            >
              {mutation.isPending ? 'Gönderiliyor...' : 'Sıfırlama Linki Gönder'}
            </Button>
          </form>
          <div className="text-center">
            <Link to="/login" className="text-sm text-violet-600 hover:underline inline-flex items-center gap-1">
              <ArrowLeft className="h-3.5 w-3.5" /> Girişe Dön
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
