import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { User, MapPin, Lock, Plus, Trash2 } from 'lucide-react'
import { authApi, type Address, type AddressRequest } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'

const profileSchema = z.object({
  firstName: z.string().min(2),
  lastName: z.string().min(2),
  phone: z.string().optional(),
})

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Mevcut şifre zorunlu'),
  newPassword: z.string().min(8, 'En az 8 karakter'),
  confirmNewPassword: z.string(),
}).refine(d => d.newPassword === d.confirmNewPassword, { message: 'Şifreler eşleşmiyor', path: ['confirmNewPassword'] })

const addressSchema = z.object({
  title: z.string().min(2, 'Başlık zorunlu'),
  firstName: z.string().min(2, 'Ad zorunlu'),
  lastName: z.string().min(2, 'Soyad zorunlu'),
  phone: z.string().min(10, 'Geçerli telefon girin'),
  city: z.string().min(2, 'Şehir zorunlu'),
  district: z.string().min(2, 'İlçe zorunlu'),
  fullAddress: z.string().min(5, 'Adres zorunlu'),
})

export function ProfilePage() {
  const qc = useQueryClient()
  const { user, updateUser } = useAuthStore()
  const [addrOpen, setAddrOpen] = useState(false)

  const { data: addresses } = useQuery({
    queryKey: ['addresses'],
    queryFn: authApi.getAddresses,
  })

  const profileForm = useForm({ resolver: zodResolver(profileSchema), defaultValues: { firstName: user?.firstName, lastName: user?.lastName, phone: user?.phone ?? '' } })
  const pwForm = useForm({ resolver: zodResolver(passwordSchema) })
  const addrForm = useForm<AddressRequest>({ resolver: zodResolver(addressSchema) })

  const updateProfile = useMutation({
    mutationFn: (d: z.infer<typeof profileSchema>) => authApi.updateProfile(d),
    onSuccess: (u) => { updateUser(u); toast.success('Profil güncellendi') },
    onError: () => toast.error('Güncelleme başarısız'),
  })

  const changePassword = useMutation({
    mutationFn: (d: z.infer<typeof passwordSchema>) => authApi.changePassword(d),
    onSuccess: () => { toast.success('Şifre değiştirildi'); pwForm.reset() },
    onError: () => toast.error('Mevcut şifre hatalı olabilir'),
  })

  const addAddress = useMutation({
    mutationFn: (d: AddressRequest) => authApi.addAddress(d),
    onSuccess: () => { toast.success('Adres eklendi'); setAddrOpen(false); addrForm.reset(); qc.invalidateQueries({ queryKey: ['addresses'] }) },
    onError: () => toast.error('Adres eklenemedi'),
  })

  const deleteAddress = useMutation({
    mutationFn: (id: number) => authApi.deleteAddress(id),
    onSuccess: () => { toast.success('Adres silindi'); qc.invalidateQueries({ queryKey: ['addresses'] }) },
  })

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold">Hesabım</h1>

      <Tabs defaultValue="profile">
        <TabsList className="grid grid-cols-3 w-full">
          <TabsTrigger value="profile" className="gap-2"><User className="h-4 w-4" /> Profil</TabsTrigger>
          <TabsTrigger value="addresses" className="gap-2"><MapPin className="h-4 w-4" /> Adreslerim</TabsTrigger>
          <TabsTrigger value="password" className="gap-2"><Lock className="h-4 w-4" /> Şifre</TabsTrigger>
        </TabsList>

        {/* Profil */}
        <TabsContent value="profile">
          <Card>
            <CardHeader><CardTitle>Profil Bilgileri</CardTitle></CardHeader>
            <CardContent>
              <form onSubmit={profileForm.handleSubmit(d => updateProfile.mutate(d))} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Ad</Label>
                    <Input {...profileForm.register('firstName')} />
                    {profileForm.formState.errors.firstName && <p className="text-xs text-red-500">En az 2 karakter</p>}
                  </div>
                  <div className="space-y-2">
                    <Label>Soyad</Label>
                    <Input {...profileForm.register('lastName')} />
                    {profileForm.formState.errors.lastName && <p className="text-xs text-red-500">En az 2 karakter</p>}
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Telefon</Label>
                  <Input placeholder="05XX XXX XX XX" {...profileForm.register('phone')} />
                </div>
                <div className="space-y-2">
                  <Label>E-posta</Label>
                  <Input value={user?.email} disabled className="bg-muted" />
                  <p className="text-xs text-muted-foreground">E-posta değiştirilemez</p>
                </div>
                <Button type="submit" disabled={updateProfile.isPending}>
                  {updateProfile.isPending ? 'Kaydediliyor...' : 'Kaydet'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Adresler */}
        <TabsContent value="addresses">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Adreslerim</CardTitle>
              <Dialog open={addrOpen} onOpenChange={setAddrOpen}>
                <DialogTrigger asChild>
                  <Button size="sm"><Plus className="h-4 w-4 mr-2" /> Yeni Adres</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader><DialogTitle>Adres Ekle</DialogTitle></DialogHeader>
                  <form onSubmit={addrForm.handleSubmit(d => addAddress.mutate(d))} className="space-y-3">
                    <div className="space-y-2">
                      <Label>Adres Başlığı</Label>
                      <Input placeholder="Ev, İş..." {...addrForm.register('title')} />
                      {addrForm.formState.errors.title && <p className="text-xs text-red-500">{addrForm.formState.errors.title.message}</p>}
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="space-y-2">
                        <Label>Ad</Label>
                        <Input placeholder="Ali" {...addrForm.register('firstName')} />
                        {addrForm.formState.errors.firstName && <p className="text-xs text-red-500">{addrForm.formState.errors.firstName.message}</p>}
                      </div>
                      <div className="space-y-2">
                        <Label>Soyad</Label>
                        <Input placeholder="Yılmaz" {...addrForm.register('lastName')} />
                        {addrForm.formState.errors.lastName && <p className="text-xs text-red-500">{addrForm.formState.errors.lastName.message}</p>}
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label>Telefon</Label>
                      <Input placeholder="05XX XXX XX XX" {...addrForm.register('phone')} />
                      {addrForm.formState.errors.phone && <p className="text-xs text-red-500">{addrForm.formState.errors.phone.message}</p>}
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="space-y-2">
                        <Label>Şehir</Label>
                        <Input {...addrForm.register('city')} />
                      </div>
                      <div className="space-y-2">
                        <Label>İlçe</Label>
                        <Input {...addrForm.register('district')} />
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label>Açık Adres</Label>
                      <Input {...addrForm.register('fullAddress')} />
                    </div>
                    <Button type="submit" className="w-full" disabled={addAddress.isPending}>
                      {addAddress.isPending ? 'Ekleniyor...' : 'Kaydet'}
                    </Button>
                  </form>
                </DialogContent>
              </Dialog>
            </CardHeader>
            <CardContent className="space-y-3">
              {!addresses || addresses.length === 0 ? (
                <p className="text-muted-foreground text-sm py-4 text-center">Kayıtlı adresiniz bulunmuyor</p>
              ) : (
                addresses.map((addr: Address) => (
                  <div key={addr.id} className="p-4 rounded-lg border">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="font-medium">{addr.title}</p>
                        <p className="text-sm text-muted-foreground">{addr.firstName} {addr.lastName} · {addr.phone}</p>
                        <p className="text-sm text-muted-foreground">{addr.district}, {addr.city}</p>
                        <p className="text-sm text-muted-foreground">{addr.fullAddress}</p>
                      </div>
                      <Button
                        variant="ghost" size="icon"
                        className="text-red-500 hover:text-red-600 hover:bg-red-50"
                        onClick={() => deleteAddress.mutate(addr.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Şifre */}
        <TabsContent value="password">
          <Card>
            <CardHeader><CardTitle>Şifre Değiştir</CardTitle></CardHeader>
            <CardContent>
              <form onSubmit={pwForm.handleSubmit(d => changePassword.mutate(d))} className="space-y-4">
                <div className="space-y-2">
                  <Label>Mevcut Şifre</Label>
                  <Input type="password" {...pwForm.register('currentPassword')} />
                </div>
                <Separator />
                <div className="space-y-2">
                  <Label>Yeni Şifre</Label>
                  <Input type="password" {...pwForm.register('newPassword')} />
                  {pwForm.formState.errors.newPassword && <p className="text-xs text-red-500">En az 8 karakter</p>}
                </div>
                <div className="space-y-2">
                  <Label>Yeni Şifre Tekrar</Label>
                  <Input type="password" {...pwForm.register('confirmNewPassword')} />
                  {pwForm.formState.errors.confirmNewPassword && <p className="text-xs text-red-500">{pwForm.formState.errors.confirmNewPassword.message}</p>}
                </div>
                <Button type="submit" disabled={changePassword.isPending}>
                  {changePassword.isPending ? 'Değiştiriliyor...' : 'Şifreyi Değiştir'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
