import { Outlet } from 'react-router-dom'
import { Navbar } from './Navbar'

export function CustomerLayout() {
  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#f1f5f9_42%,#ecfeff_100%)]">
      <Navbar />
      <main className="page-shell py-6 sm:py-8">
        <Outlet />
      </main>
      <footer className="mt-16 border-t border-slate-200/80 bg-white/70">
        <div className="page-shell flex flex-col gap-3 py-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p className="font-medium text-slate-700">n12 · AI destekli e-ticaret deneyimi</p>
          <p>Güvenli ödeme, akıllı öneriler ve takip edilebilir sipariş akışı.</p>
        </div>
      </footer>
    </div>
  )
}
