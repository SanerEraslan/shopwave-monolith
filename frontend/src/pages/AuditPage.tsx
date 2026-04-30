import { useEffect, useState } from 'react'
import { auditApi } from '../api'
import { Spinner, ErrorBanner } from '../components'

interface AuditEntry {
  id: number
  eventType: string
  aggregate: string
  aggregateId: number
  payload?: string
  createdAt: string
}

const EVENT_COLORS: Record<string, string> = {
  ORDER_PLACED:          'bg-indigo-100 text-indigo-700',
  ORDER_CONFIRMED:       'bg-blue-100 text-blue-700',
  ORDER_SHIPPED:         'bg-purple-100 text-purple-700',
  ORDER_DELIVERED:       'bg-green-100 text-green-700',
  ORDER_CANCELLED:       'bg-red-100 text-red-700',
  STOCK_RESERVED:        'bg-yellow-100 text-yellow-700',
  STOCK_RELEASED:        'bg-orange-100 text-orange-700',
  STOCK_DEDUCTED:        'bg-rose-100 text-rose-700',
  STOCK_ADDED:           'bg-teal-100 text-teal-700',
  PRODUCT_CREATED:       'bg-sky-100 text-sky-700',
  PRODUCT_PRICE_UPDATED: 'bg-cyan-100 text-cyan-700',
  PRODUCT_DEACTIVATED:   'bg-gray-100 text-gray-600',
}

export default function AuditPage() {
  const [logs,    setLogs]    = useState<AuditEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState('')

  useEffect(() => {
    auditApi.recent()
      .then(setLogs)
      .catch(() => setError('Audit logları yüklenemedi'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Spinner />
  if (error)   return <ErrorBanner message={error} />

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Audit Log</h1>
          <p className="text-sm text-gray-400 mt-0.5">
            Monolith: tüm eventler aynı transaction içinde yazılır — atomik ve tutarlı.
          </p>
        </div>
        <span className="text-sm text-gray-500">{logs.length} kayıt</span>
      </div>

      <div className="bg-white border border-gray-100 rounded-xl shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-100">
            <tr>
              {['Zaman', 'Event', 'Aggregate', 'ID', 'Payload'].map(h => (
                <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {logs.map(log => (
              <tr key={log.id} className="hover:bg-gray-50 transition-colors">
                <td className="px-4 py-3 text-gray-400 whitespace-nowrap font-mono text-xs">
                  {new Date(log.createdAt).toLocaleTimeString('tr-TR')}
                </td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${EVENT_COLORS[log.eventType] ?? 'bg-gray-100 text-gray-600'}`}>
                    {log.eventType}
                  </span>
                </td>
                <td className="px-4 py-3 text-gray-500 capitalize">{log.aggregate}</td>
                <td className="px-4 py-3 text-gray-500 font-mono">{log.aggregateId}</td>
                <td className="px-4 py-3 text-gray-400 font-mono text-xs max-w-xs truncate">
                  {log.payload ?? '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {logs.length === 0 && (
          <p className="text-center text-gray-400 py-8">Henüz kayıt yok</p>
        )}
      </div>
    </div>
  )
}
