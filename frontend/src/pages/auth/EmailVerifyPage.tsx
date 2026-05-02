import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'

export function EmailVerifyPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [message, setMessage] = useState('')

  const verifiedRef = useRef(false)

  useEffect(() => {
    if (!token) {
      setStatus('error')
      setMessage('Geçersiz doğrulama linki.')
      return
    }
    if (verifiedRef.current) return
    verifiedRef.current = true

    authApi.verifyEmail(token)
      .then(() => {
        setStatus('success')
        setMessage('E-posta adresiniz başarıyla doğrulandı!')
      })
      .catch((err: any) => {
        const serverMsg: string = err?.response?.data?.message ?? ''
        if (serverMsg.includes('daha önce kullanılmış')) {
          setStatus('success')
          setMessage('E-posta adresiniz zaten doğrulanmış.')
        } else {
          setStatus('error')
          setMessage(serverMsg || 'Doğrulama başarısız. Link geçersiz veya süresi dolmuş olabilir.')
        }
      })
  }, [token])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-violet-50 to-indigo-50 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardContent className="pt-12 pb-10 text-center space-y-6">
          {status === 'loading' && (
            <>
              <div className="flex justify-center">
                <Loader2 className="h-14 w-14 text-violet-500 animate-spin" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900">Doğrulanıyor...</h2>
                <p className="text-sm text-muted-foreground mt-2">Lütfen bekleyin.</p>
              </div>
            </>
          )}

          {status === 'success' && (
            <>
              <div className="flex justify-center">
                <div className="h-16 w-16 rounded-full bg-green-100 flex items-center justify-center">
                  <CheckCircle className="h-9 w-9 text-green-600" />
                </div>
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900">Doğrulama Başarılı! 🎉</h2>
                <p className="text-sm text-muted-foreground mt-2 leading-relaxed">{message}</p>
              </div>
              <Link to="/login">
                <Button className="w-full bg-violet-600 hover:bg-violet-700">
                  Giriş Yap
                </Button>
              </Link>
            </>
          )}

          {status === 'error' && (
            <>
              <div className="flex justify-center">
                <div className="h-16 w-16 rounded-full bg-red-100 flex items-center justify-center">
                  <XCircle className="h-9 w-9 text-red-600" />
                </div>
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900">Doğrulama Başarısız</h2>
                <p className="text-sm text-muted-foreground mt-2 leading-relaxed">{message}</p>
              </div>
              <div className="space-y-2">
                <Link to="/login">
                  <Button className="w-full" variant="outline">Girişe Dön</Button>
                </Link>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
