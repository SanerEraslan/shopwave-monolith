import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import ProductsPage from './pages/ProductsPage'
import NewOrderPage from './pages/NewOrderPage'
import OrdersPage   from './pages/OrdersPage'
import AuditPage    from './pages/AuditPage'

const NAV = [
  { to: '/',        label: 'Ürünler',       icon: '📦' },
  { to: '/orders',  label: 'Siparişler',    icon: '🛍️'  },
  { to: '/new',     label: 'Yeni Sipariş',  icon: '➕' },
  { to: '/audit',   label: 'Audit Log',     icon: '📋' },
]

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50 flex flex-col">
        {/* Header */}
        <header className="bg-white border-b border-gray-100 shadow-sm sticky top-0 z-10">
          <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-xl">🛒</span>
              <span className="font-bold text-gray-900">ShopWave</span>
              <span className="text-xs bg-indigo-100 text-indigo-600 font-semibold px-2 py-0.5 rounded-full ml-2">
                monolith
              </span>
            </div>
            <nav className="flex items-center gap-1">
              {NAV.map(n => (
                <NavLink
                  key={n.to}
                  to={n.to}
                  end={n.to === '/'}
                  className={({ isActive }) =>
                    `flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ` +
                    (isActive
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100')
                  }
                >
                  <span>{n.icon}</span>
                  <span className="hidden sm:inline">{n.label}</span>
                </NavLink>
              ))}
            </nav>
          </div>
        </header>

        {/* Main */}
        <main className="flex-1 max-w-6xl mx-auto w-full px-4 py-8">
          <Routes>
            <Route path="/"       element={<ProductsPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/new"    element={<NewOrderPage />} />
            <Route path="/audit"  element={<AuditPage />} />
          </Routes>
        </main>

        {/* Footer */}
        <footer className="border-t border-gray-100 bg-white text-center py-3 text-xs text-gray-400">
          ShopWave Monolith — Dağıtık Sistemler Lab Başlangıç Noktası
        </footer>
      </div>
    </BrowserRouter>
  )
}
