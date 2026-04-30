import { useEffect, useState } from 'react'
import { customerApi, orderApi } from '../api'
import type { Customer, Order } from '../types'
import { Spinner, EmptyState, ErrorBanner, StatusBadge, Card } from '../components'

export default function OrdersPage() {
  const [customers,   setCustomers]   = useState<Customer[]>([])
  const [customerId,  setCustomerId]  = useState<number | ''>('')
  const [orders,      setOrders]      = useState<Order[]>([])
  const [loading,     setLoading]     = useState(false)
  const [initLoading, setInitLoading] = useState(true)
  const [error,       setError]       = useState('')
  const [actionId,    setActionId]    = useState<number | null>(null)

  useEffect(() => {
    customerApi.list()
      .then(setCustomers)
      .finally(() => setInitLoading(false))
  }, [])

  const loadOrders = (cid: number) => {
    setLoading(true)
    setError('')
    orderApi.getByCustomer(cid)
      .then(setOrders)
      .catch(() => setError('Siparişler yüklenemedi'))
      .finally(() => setLoading(false))
  }

  const handleCustomer = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const cid = Number(e.target.value)
    setCustomerId(cid)
    if (cid) loadOrders(cid)
    else setOrders([])
  }

  const action = async (fn: () => Promise<Order>, orderId: number) => {
    setActionId(orderId)
    try {
      const updated = await fn()
      setOrders(prev => prev.map(o => o.id === updated.id ? updated : o))
    } catch (e: any) {
      setError(e?.response?.data?.detail ?? 'İşlem başarısız')
    } finally {
      setActionId(null)
    }
  }

  if (initLoading) return <Spinner />

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Siparişler</h1>

      <div className="mb-6 max-w-xs">
        <label className="text-sm text-gray-600 mb-1 block">Müşteri Seç</label>
        <select
          className="w-full border rounded-lg px-3 py-2 text-sm"
          value={customerId}
          onChange={handleCustomer}
        >
          <option value="">-- Müşteri seçin --</option>
          {customers.map(c => (
            <option key={c.id} value={c.id}>{c.fullName}</option>
          ))}
        </select>
      </div>

      {error && <div className="mb-4"><ErrorBanner message={error} /></div>}
      {loading && <Spinner />}

      {!loading && customerId && orders.length === 0 && (
        <EmptyState message="Bu müşteriye ait sipariş bulunamadı" />
      )}

      <div className="space-y-4">
        {orders.map(order => (
          <Card key={order.id} className="p-5">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
              <div>
                <span className="font-mono font-semibold text-indigo-600 text-sm">{order.orderRef}</span>
                <span className="ml-3 text-xs text-gray-400">{new Date(order.createdAt).toLocaleString('tr-TR')}</span>
              </div>
              <div className="flex items-center gap-3">
                <StatusBadge status={order.status} />
                <span className="font-bold text-gray-900">${order.totalAmount.toFixed(2)}</span>
              </div>
            </div>

            {/* Kalemler */}
            <div className="text-sm text-gray-600 space-y-1 mb-4">
              {order.items?.map((item, i) => (
                <div key={i} className="flex justify-between">
                  <span>{item.productName} × {item.quantity}</span>
                  <span className="text-gray-900 font-medium">${item.lineTotal.toFixed(2)}</span>
                </div>
              ))}
            </div>

            {/* Aksiyon butonları */}
            <div className="flex flex-wrap gap-2">
              {order.status === 'PENDING' && (
                <>
                  <ActionBtn label="Onayla" color="blue"
                    loading={actionId === order.id}
                    onClick={() => action(() => orderApi.confirm(order.id), order.id)} />
                  <ActionBtn label="İptal Et" color="red"
                    loading={actionId === order.id}
                    onClick={() => action(() => orderApi.cancel(order.id), order.id)} />
                </>
              )}
              {order.status === 'CONFIRMED' && (
                <ActionBtn label="Kargoya Ver" color="indigo"
                  loading={actionId === order.id}
                  onClick={() => action(() => orderApi.ship(order.id), order.id)} />
              )}
              {order.status === 'SHIPPED' && (
                <ActionBtn label="Teslim Edildi" color="green"
                  loading={actionId === order.id}
                  onClick={() => action(() => orderApi.deliver(order.id), order.id)} />
              )}
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}

function ActionBtn({ label, color, loading, onClick }: {
  label: string; color: string; loading: boolean; onClick: () => void
}) {
  const colors: Record<string, string> = {
    blue:   'bg-blue-100 text-blue-700 hover:bg-blue-200',
    red:    'bg-red-100 text-red-700 hover:bg-red-200',
    indigo: 'bg-indigo-100 text-indigo-700 hover:bg-indigo-200',
    green:  'bg-green-100 text-green-700 hover:bg-green-200',
  }
  return (
    <button
      onClick={onClick}
      disabled={loading}
      className={`text-xs font-semibold px-3 py-1.5 rounded-lg ${colors[color]} disabled:opacity-40`}
    >
      {label}
    </button>
  )
}
