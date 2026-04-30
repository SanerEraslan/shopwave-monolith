import { useEffect, useState } from 'react'
import { productApi, customerApi, orderApi } from '../api'
import type { Product, Customer, Order } from '../types'
import { Spinner, ErrorBanner, StatusBadge } from '../components'

interface CartItem { product: Product; quantity: number }

export default function NewOrderPage() {
  const [products,   setProducts]   = useState<Product[]>([])
  const [customers,  setCustomers]  = useState<Customer[]>([])
  const [loading,    setLoading]    = useState(true)
  const [cart,       setCart]       = useState<CartItem[]>([])
  const [customerId, setCustomerId] = useState<number | ''>('')
  const [address,    setAddress]    = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [result,     setResult]     = useState<Order | null>(null)
  const [error,      setError]      = useState('')

  useEffect(() => {
    Promise.all([productApi.list(), customerApi.list()])
      .then(([prods, custs]) => { setProducts(prods); setCustomers(custs) })
      .catch(() => setError('Veriler yüklenemedi'))
      .finally(() => setLoading(false))
  }, [])

  const addToCart = (product: Product) => {
    setCart(prev => {
      const existing = prev.find(i => i.product.id === product.id)
      if (existing) return prev.map(i => i.product.id === product.id ? { ...i, quantity: i.quantity + 1 } : i)
      return [...prev, { product, quantity: 1 }]
    })
  }

  const updateQty = (productId: number, qty: number) => {
    if (qty <= 0) setCart(prev => prev.filter(i => i.product.id !== productId))
    else setCart(prev => prev.map(i => i.product.id === productId ? { ...i, quantity: qty } : i))
  }

  const total = cart.reduce((s, i) => s + i.product.price * i.quantity, 0)

  const handleSubmit = async () => {
    if (!customerId || cart.length === 0) return
    setSubmitting(true)
    setError('')
    try {
      const order = await orderApi.place({
        customerId: customerId as number,
        shippingAddress: address,
        items: cart.map(i => ({ productId: i.product.id, quantity: i.quantity }))
      })
      setResult(order)
      setCart([])
    } catch (e: any) {
      setError(e?.response?.data?.detail ?? 'Sipariş oluşturulamadı')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <Spinner />

  if (result) return (
    <div className="max-w-lg mx-auto text-center py-12">
      <div className="text-5xl mb-4">🎉</div>
      <h2 className="text-2xl font-bold text-gray-900 mb-2">Sipariş Oluşturuldu!</h2>
      <p className="text-gray-500 mb-1">Sipariş No: <span className="font-mono font-semibold text-indigo-600">{result.orderRef}</span></p>
      <p className="text-gray-500 mb-1">Durum: <StatusBadge status={result.status} /></p>
      <p className="text-gray-500 mb-6">Toplam: <strong>${result.totalAmount.toFixed(2)}</strong></p>
      <button
        onClick={() => { setResult(null) }}
        className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700"
      >
        Yeni Sipariş
      </button>
    </div>
  )

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Ürün seçimi */}
      <div className="lg:col-span-2">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Yeni Sipariş</h1>
        {error && <div className="mb-4"><ErrorBanner message={error} /></div>}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {products.filter(p => p.active && (p.availableStock ?? 0) > 0).map(p => (
            <div key={p.id}
              className="bg-white border border-gray-100 rounded-xl p-4 flex items-center justify-between shadow-sm hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => addToCart(p)}
            >
              <div>
                <p className="font-medium text-gray-900 text-sm">{p.name}</p>
                <p className="text-xs text-gray-400">{p.sku} · {p.availableStock} adet</p>
              </div>
              <div className="text-right">
                <p className="font-bold text-indigo-600">${p.price.toFixed(2)}</p>
                <p className="text-xs text-indigo-400 mt-0.5">+ Ekle</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Sepet */}
      <div className="bg-white border border-gray-100 rounded-xl shadow-sm p-5 h-fit sticky top-6">
        <h2 className="font-bold text-gray-900 text-lg mb-4">Sepet</h2>

        <div className="mb-4">
          <label className="text-sm text-gray-600 mb-1 block">Müşteri</label>
          <select
            className="w-full border rounded-lg px-3 py-2 text-sm"
            value={customerId}
            onChange={e => setCustomerId(Number(e.target.value))}
          >
            <option value="">-- Seç --</option>
            {customers.map(c => (
              <option key={c.id} value={c.id}>{c.fullName}</option>
            ))}
          </select>
        </div>

        <div className="mb-4">
          <label className="text-sm text-gray-600 mb-1 block">Teslimat Adresi</label>
          <textarea
            className="w-full border rounded-lg px-3 py-2 text-sm resize-none"
            rows={2}
            value={address}
            onChange={e => setAddress(e.target.value)}
            placeholder="Adres..."
          />
        </div>

        {cart.length === 0 ? (
          <p className="text-gray-400 text-sm text-center py-4">Sepet boş</p>
        ) : (
          <div className="space-y-2 mb-4">
            {cart.map(item => (
              <div key={item.product.id} className="flex items-center justify-between gap-2">
                <p className="text-sm text-gray-700 flex-1 truncate">{item.product.name}</p>
                <div className="flex items-center gap-1">
                  <button onClick={() => updateQty(item.product.id, item.quantity - 1)}
                    className="w-6 h-6 rounded bg-gray-100 hover:bg-gray-200 text-sm font-bold">−</button>
                  <span className="w-6 text-center text-sm">{item.quantity}</span>
                  <button onClick={() => updateQty(item.product.id, item.quantity + 1)}
                    className="w-6 h-6 rounded bg-gray-100 hover:bg-gray-200 text-sm font-bold">+</button>
                </div>
                <p className="text-sm font-semibold text-gray-900 w-16 text-right">
                  ${(item.product.price * item.quantity).toFixed(2)}
                </p>
              </div>
            ))}
          </div>
        )}

        <div className="border-t pt-3 flex items-center justify-between mb-4">
          <span className="font-semibold text-gray-700">Toplam</span>
          <span className="font-bold text-lg text-indigo-600">${total.toFixed(2)}</span>
        </div>

        <button
          onClick={handleSubmit}
          disabled={submitting || cart.length === 0 || !customerId}
          className="w-full bg-indigo-600 text-white py-2.5 rounded-lg font-semibold hover:bg-indigo-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          {submitting ? 'Gönderiliyor…' : 'Siparişi Onayla'}
        </button>
      </div>
    </div>
  )
}
