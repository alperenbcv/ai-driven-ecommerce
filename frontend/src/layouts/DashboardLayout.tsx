import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { performLogout } from '@/lib/logout'
import { Button } from '@/components/ui/button'
import { LogOut, Home, Store } from 'lucide-react'

interface SidebarItem { label: string; to: string; icon?: React.ReactNode }

interface DashboardLayoutProps {
  title: string
  items: SidebarItem[]
}

export function DashboardLayout({ title, items }: DashboardLayoutProps) {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-slate-100">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-72 border-r border-slate-200 bg-slate-950 text-white lg:flex lg:flex-col">
        <div className="border-b border-white/10 p-6">
          <div className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-500 text-slate-950">
              <Store className="h-5 w-5" />
            </span>
            <div>
              <p className="text-lg font-black">{title}</p>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-300">Control Center</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 space-y-1 p-4">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-colors',
                  isActive
                    ? 'bg-white text-slate-950 shadow-lg shadow-black/10'
                    : 'text-slate-300 hover:bg-white/10 hover:text-white',
                )
              }
            >
              {item.icon}
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="space-y-2 border-t border-white/10 p-4">
          <Button variant="ghost" size="sm" className="w-full justify-start gap-2 text-slate-200 hover:bg-white/10 hover:text-white" asChild>
            <NavLink to="/">
              <Home className="h-4 w-4" /> Mağazaya Dön
            </NavLink>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="w-full justify-start gap-2 text-rose-200 hover:bg-rose-500/10 hover:text-rose-100"
            onClick={() => { performLogout(); navigate('/login') }}
          >
            <LogOut className="h-4 w-4" /> Çıkış Yap
          </Button>
        </div>
      </aside>

      <main className="min-h-screen p-4 sm:p-6 lg:ml-72 lg:p-8">
        <div className="mx-auto max-w-7xl">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
