import { useEffect, useState } from 'react'
import { productApi, inventoryApi } from '../api'
import type { Product } from '../types'
import { Spinner, EmptyState, ErrorBanner, Card } from '../components'

export default function ProductsPage() {
  const [products, setProducts]   = useState<Product[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')
  const [addingStock, setAdding]  = useState<number | null>(null)
  const [stockQty, setStockQty]   = useState<Record<number, string>>({})

  const load = () => {
    setLoading(true)
    productApi.list()
      .then(setProducts)
      .catch(() => setError('Ürünler yüklenemedi'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleAddStock = async (id: number) => {
    const qty = parseInt(stockQty[id] ?? '0')
    if (!qty || qty <= 0) return
    await inventoryApi.addStock(id, qty)
    setAdding(null)
    setStockQty(prev => ({ ...prev, [id]: '' }))
    load()
  }

  if (loading) return <Spinner />
  if (error)   return <ErrorBanner message={error} />

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Ürün Kataloğu</h1>
        <span className="text-sm text-gray-500">{products.length} ürün</span>
      </div>

      {products.length === 0 && <EmptyState message="Hiç ürün bulunamadı" />}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {products.map(p => (
          <Card key={p.id} className="p-4 flex flex-col gap-2">
            <div className="flex items-start justify-between">
              <div>
                <p className="font-semibold text-gray-900">{p.name}</p>
                <p className="text-xs text-gray-400 font-mono">{p.sku}</p>
              </div>
              <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${
                p.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
              }`}>
                {p.active ? 'Aktif' : 'Pasif'}
              </span>
            </div>

            {p.categoryName && (
              <p className="text-xs text-indigo-600 font-medium">{p.categoryName}</p>
            )}

            {p.description && (
              <p className="text-sm text-gray-500 line-clamp-2">{p.description}</p>
            )}

            <div className="flex items-center justify-between mt-1">
              <span className="text-lg font-bold text-gray-900">
                ${p.price.toFixed(2)}
              </span>
              <StockPill available={p.availableStock ?? 0} />
            </div>

            {/* Stok ekle inline */}
            {addingStock === p.id ? (
              <div className="flex gap-2 mt-1">
                <input
                  type="number" min="1"
                  className="border rounded px-2 py-1 text-sm w-24"
                  placeholder="Adet"
                  value={stockQty[p.id] ?? ''}
                  onChange={e => setStockQty(prev => ({ ...prev, [p.id]: e.target.value }))}
                />
                <button
                  onClick={() => handleAddStock(p.id)}
                  className="bg-indigo-600 text-white text-sm px-3 rounded hover:bg-indigo-700"
                >
                  Ekle
                </button>
                <button
                  onClick={() => setAdding(null)}
                  className="text-sm text-gray-500 hover:text-gray-700"
                >
                  İptal
                </button>
              </div>
            ) : (
              <button
                onClick={() => setAdding(p.id)}
                className="text-xs text-indigo-600 hover:underline text-left mt-1"
              >
                + Stok ekle
              </button>
            )}
          </Card>
        ))}
      </div>
    </div>
  )
}

function StockPill({ available }: { available: number }) {
  const color = available === 0
    ? 'bg-red-100 text-red-700'
    : available < 10
      ? 'bg-yellow-100 text-yellow-700'
      : 'bg-green-100 text-green-700'
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${color}`}>
      {available === 0 ? 'Stok yok' : `${available} adet`}
    </span>
  )
}
