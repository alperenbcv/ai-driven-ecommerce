import { Link } from 'react-router-dom'
import { Package, Star } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { formatPrice } from '@/lib/utils'
import type { Product } from '@/types'

export function ProductCard({ product }: { product: Product }) {
  const image = product.images?.[0]?.url

  return (
    <Link
      to={`/products/${product.id}`}
      className="group block h-full overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm shadow-slate-200/70 transition-all duration-300 hover:-translate-y-1 hover:border-teal-200 hover:shadow-xl hover:shadow-teal-900/10"
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-slate-100">
        {image ? (
          <img
            src={image}
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-[radial-gradient(circle_at_30%_20%,#ccfbf1,transparent_32%),linear-gradient(135deg,#f8fafc,#e2e8f0)]">
            <Package className="h-12 w-12 text-slate-400" />
          </div>
        )}
        {product.category?.name && (
          <Badge className="absolute left-3 top-3 border border-white/70 bg-white/90 text-slate-700 shadow-sm hover:bg-white">
            {product.category.name}
          </Badge>
        )}
      </div>

      <div className="space-y-3 p-4">
        <div className="min-h-[64px]">
          <p className="text-xs font-medium text-slate-500">{product.brand?.name ?? 'n12 seçkisi'}</p>
          <h3 className="mt-1 line-clamp-2 text-sm font-semibold leading-snug text-slate-950">
            {product.name}
          </h3>
        </div>

        <div className="flex items-end justify-between gap-3">
          <div>
            <p className="text-[11px] font-medium uppercase tracking-wide text-slate-400">Fiyat</p>
            <p className="text-lg font-bold text-teal-700">{formatPrice(product.price)}</p>
          </div>
          {product.averageRating > 0 && (
            <div className="flex items-center gap-1 rounded-full bg-amber-50 px-2 py-1 text-xs font-semibold text-amber-700">
              <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
              {product.averageRating.toFixed(1)}
            </div>
          )}
        </div>
      </div>
    </Link>
  )
}
