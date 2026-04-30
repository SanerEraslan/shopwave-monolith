import type { OrderStatus } from '../types'

// ── StatusBadge ────────────────────────────────────────────────
const STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING:   'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  SHIPPED:   'bg-indigo-100 text-indigo-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
}

export function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${STATUS_COLORS[status]}`}>
      {status}
    </span>
  )
}

// ── Spinner ────────────────────────────────────────────────────
export function Spinner() {
  return (
    <div className="flex justify-center items-center py-12">
      <div className="w-8 h-8 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )
}

// ── EmptyState ─────────────────────────────────────────────────
export function EmptyState({ message }: { message: string }) {
  return (
    <div className="text-center py-16 text-gray-400">
      <p className="text-lg">{message}</p>
    </div>
  )
}

// ── ErrorBanner ────────────────────────────────────────────────
export function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">
      {message}
    </div>
  )
}

// ── Card ───────────────────────────────────────────────────────
export function Card({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`bg-white rounded-xl shadow-sm border border-gray-100 ${className}`}>
      {children}
    </div>
  )
}
