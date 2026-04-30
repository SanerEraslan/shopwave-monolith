import axios from 'axios'
import type { Product, Customer, Order, PlaceOrderRequest } from '../types'

const BASE = (import.meta as any).env?.VITE_API_BASE_URL ?? ''

const api = axios.create({ baseURL: BASE })

// TODO LAB-1: Her isteğe otomatik X-Correlation-ID header ekle
// api.interceptors.request.use(config => {
//   config.headers['X-Correlation-ID'] = crypto.randomUUID()
//   return config
// })

// TODO LAB-5: Her isteğe X-Idempotency-Key header ekle (POST için)
// api.interceptors.request.use(config => {
//   if (config.method === 'post') {
//     config.headers['X-Idempotency-Key'] = crypto.randomUUID()
//   }
//   return config
// })

export const productApi = {
  list: ()                              => api.get<Product[]>('/api/v1/products').then(r => r.data),
  get:  (id: number)                   => api.get<Product>(`/api/v1/products/${id}`).then(r => r.data),
  create: (body: object)               => api.post<Product>('/api/v1/products', body).then(r => r.data),
  updatePrice: (id: number, price: number) =>
    api.patch<Product>(`/api/v1/products/${id}/price`, { price }).then(r => r.data),
  deactivate: (id: number)             => api.delete(`/api/v1/products/${id}`),
}

export const customerApi = {
  list: ()           => api.get<Customer[]>('/api/v1/customers').then(r => r.data),
  get:  (id: number) => api.get<Customer>(`/api/v1/customers/${id}`).then(r => r.data),
}

export const orderApi = {
  get:           (id: number)          => api.get<Order>(`/api/v1/orders/${id}`).then(r => r.data),
  getByCustomer: (cid: number)         => api.get<Order[]>(`/api/v1/orders/customer/${cid}`).then(r => r.data),
  place:         (req: PlaceOrderRequest) => api.post<Order>('/api/v1/orders', req).then(r => r.data),
  confirm:       (id: number)          => api.post<Order>(`/api/v1/orders/${id}/confirm`).then(r => r.data),
  ship:          (id: number)          => api.post<Order>(`/api/v1/orders/${id}/ship`).then(r => r.data),
  deliver:       (id: number)          => api.post<Order>(`/api/v1/orders/${id}/deliver`).then(r => r.data),
  cancel:        (id: number)          => api.post<Order>(`/api/v1/orders/${id}/cancel`).then(r => r.data),
}

export const inventoryApi = {
  lowStock: (threshold = 10) => api.get(`/api/v1/inventory/low-stock?threshold=${threshold}`).then(r => r.data),
  addStock: (productId: number, quantity: number) =>
    api.post(`/api/v1/inventory/products/${productId}/add`, { quantity }).then(r => r.data),
}

export const auditApi = {
  recent: () => api.get('/api/v1/audit').then(r => r.data),
}
