import { Link, NavLink, useNavigate } from 'react-router-dom'
import {
  Bot, LayoutDashboard, LogOut, Search, Shield, ShoppingBag,
  ShoppingCart, Sparkles, Store, User,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import { performLogout } from '@/lib/logout'
import { useCartStore } from '@/stores/cartStore'
import { useIsAuthenticated, useIsAdmin, useIsSeller, useUser } from '@/stores/authStore'
import { cn } from '@/lib/utils'
import { useState } from 'react'

const navItems = [
  { label: 'Ürünler', to: '/products', icon: ShoppingBag },
  { label: 'AI Asistan', to: '/chat', icon: Bot },
  { label: 'Siparişler', to: '/orders', icon: ShoppingCart },
]

export function Navbar() {
  const navigate = useNavigate()
  const isAuth = useIsAuthenticated()
  const isAdmin = useIsAdmin()
  const isSeller = useIsSeller()
  const user = useUser()
  const totalItems = useCartStore((s) => s.totalItems)
  const [searchQ, setSearchQ] = useState('')

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchQ.trim()) navigate(`/products?keyword=${encodeURIComponent(searchQ.trim())}`)
  }

  const handleLogout = () => {
    performLogout()
    navigate('/login')
  }

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/85 backdrop-blur-xl">
      <div className="page-shell flex h-18 min-h-[72px] items-center gap-4">
        <Link to="/" className="group flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-950 text-white shadow-lg shadow-slate-900/15">
            <Store className="h-5 w-5" />
          </span>
          <span className="leading-tight">
            <span className="block text-lg font-black tracking-tight text-slate-950">n12</span>
            <span className="hidden text-[11px] font-semibold uppercase tracking-[0.18em] text-teal-700 sm:block">
              AI Commerce
            </span>
          </span>
        </Link>

        <nav className="hidden items-center gap-1 lg:flex">
          {navItems.map(({ label, to, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold transition-colors',
                  isActive ? 'bg-teal-50 text-teal-800' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>

        <form onSubmit={handleSearch} className="hidden flex-1 xl:block">
          <div className="relative mx-auto max-w-xl">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Ürün, kategori veya marka ara"
              value={searchQ}
              onChange={(e) => setSearchQ(e.target.value)}
              className="focus-ring h-11 w-full rounded-2xl border border-slate-200 bg-slate-50 pl-11 pr-4 text-sm font-medium text-slate-900 placeholder:text-slate-400"
            />
          </div>
        </form>

        <div className="ml-auto flex items-center gap-2">
          <Button variant="outline" size="sm" asChild className="hidden border-teal-200 bg-teal-50 text-teal-800 hover:bg-teal-100 md:inline-flex">
            <Link to="/chat">
              <Sparkles className="h-4 w-4" />
              AI
            </Link>
          </Button>

          <Button variant="ghost" size="icon" asChild className="relative rounded-xl">
            <Link to="/cart" aria-label="Sepet">
              <ShoppingCart className="h-5 w-5" />
              {totalItems > 0 && (
                <Badge className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-rose-500 p-0 text-[11px] text-white">
                  {totalItems}
                </Badge>
              )}
            </Link>
          </Button>

          {isAuth && user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="h-11 gap-2 rounded-2xl px-2 pr-3">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className="bg-slate-900 text-xs font-bold text-white">
                      {user.firstName[0]}{user.lastName[0]}
                    </AvatarFallback>
                  </Avatar>
                  <span className="hidden text-sm font-semibold text-slate-700 md:block">{user.firstName}</span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56 rounded-xl">
                <DropdownMenuItem asChild>
                  <Link to="/profile" className="flex items-center gap-2">
                    <User className="h-4 w-4" /> Profilim
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link to="/orders" className="flex items-center gap-2">
                    <ShoppingCart className="h-4 w-4" /> Siparişlerim
                  </Link>
                </DropdownMenuItem>
                {isSeller && (
                  <>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem asChild>
                      <Link to="/seller" className="flex items-center gap-2">
                        <LayoutDashboard className="h-4 w-4" /> Satıcı Paneli
                      </Link>
                    </DropdownMenuItem>
                  </>
                )}
                {isAdmin && (
                  <>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem asChild>
                      <Link to="/admin" className="flex items-center gap-2">
                        <Shield className="h-4 w-4" /> Admin Paneli
                      </Link>
                    </DropdownMenuItem>
                  </>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout} className="flex items-center gap-2 text-red-600">
                  <LogOut className="h-4 w-4" /> Çıkış Yap
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <div className="flex gap-2">
              <Button variant="ghost" asChild size="sm">
                <Link to="/login">Giriş</Link>
              </Button>
              <Button asChild size="sm">
                <Link to="/register">Üye Ol</Link>
              </Button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
