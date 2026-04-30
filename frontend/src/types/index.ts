export interface Product {
  id: number
  sku: string
  name: string
  description?: string
  price: number
  categoryName?: string
  active: boolean
  availableStock?: number
  createdAt: string
}

export interface Customer {
  id: number
  email: string
  fullName: string
  phone?: string
}

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export interface OrderItem {
  productId: number
  sku: string
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface Order {
  id: number
  orderRef: string
  customerId: number
  customerName: string
  status: OrderStatus
  totalAmount: number
  shippingAddress?: string
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export interface PlaceOrderRequest {
  customerId: number
  shippingAddress: string
  items: { productId: number; quantity: number }[]
}
