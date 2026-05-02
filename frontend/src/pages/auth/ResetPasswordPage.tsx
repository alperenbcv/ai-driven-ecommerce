import { useEffect } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { KeyRound, AlertCircle } from 'lucide-react'
import { authApi } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

const schema = z.object({
  newPassword: z.string().min(6, 'Şifre en az 6 karakter olmalı'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Şifreler eşleşmiyor',
  path: ['confirmPassword'],
})
type FormData = z.infer<typeof schema>

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  useEffect(() => {
    if (!token) {
      toast.error('Geçersiz veya eksik şifre sıfırlama linki')
    }
  }, [token])

  const mutation = useMutation({
    mutationFn: (data: FormData) => authApi.resetPassword(token!, data.newPassword),
    onSuccess: () => {
      toast.success('Şifreniz başarıyla güncellendi! Lütfen yeni şifrenizle giriş yapın.')
      navigate('/login')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message ?? 'Link geçersiz veya süresi dolmuş'
      toast.error(msg)
    },
  })

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 to-orange-50 p-4">
        <Card className="w-full max-w-md shadow-lg">
          <CardContent className="pt-10 pb-8 text-center space-y-4">
            <div className="flex justify-center">
              <div className="h-16 w-16 rounded-full bg-red-100 flex items-center justify-center">
                <AlertCircle className="h-8 w-8 text-red-600" />
              </div>
            </div>
            <h2 className="text-xl font-bold text-gray-900">Geçersiz Link</h2>
            <p className="text-sm text-muted-foreground">Bu şifre sıfırlama linki geçersiz veya eksik.</p>
            <Link to="/forgot-password">
              <Button className="w-full">Yeni Link Al</Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-amber-50 to-orange-50 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="text-center pb-2">
          <div className="flex justify-center mb-4">
            <div className="h-12 w-12 rounded-full bg-amber-100 flex items-center justify-center">
              <KeyRound className="h-6 w-6 text-amber-600" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold">Yeni Şifre Belirle</CardTitle>
          <CardDescription>En az 6 karakter kullanın.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(data => mutation.mutate(data))} className="space-y-4">
            <div className="space-y-1.5">
              <Label>Yeni Şifre</Label>
              <Input type="password" placeholder="••••••••" autoFocus {...register('newPassword')} />
              {errors.newPassword && <p className="text-xs text-red-500">{errors.newPassword.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label>Şifre Tekrar</Label>
              <Input type="password" placeholder="••••••••" {...register('confirmPassword')} />
              {errors.confirmPassword && <p className="text-xs text-red-500">{errors.confirmPassword.message}</p>}
            </div>
            <Button
              type="submit"
              className="w-full bg-amber-600 hover:bg-amber-700"
              disabled={mutation.isPending}
            >
              {mutation.isPending ? 'Güncelleniyor...' : 'Şifremi Güncelle'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
