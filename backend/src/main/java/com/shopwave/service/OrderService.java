package com.shopwave.service;

import com.shopwave.domain.*;
import com.shopwave.domain.Order.OrderStatus;
import com.shopwave.dto.OrderDto;
import com.shopwave.dto.OrderDto.OrderItemDto;
import com.shopwave.dto.PlaceOrderRequest;
import com.shopwave.exception.InvalidOrderStateException;
import com.shopwave.exception.NotFoundException;
import com.shopwave.repository.CustomerRepository;
import com.shopwave.repository.OrderRepository;
import com.shopwave.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OrderService — sipariş iş akışının kalbi.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Monolith'te placeOrder() tek bir @Transactional içinde:        │
 * │    1. Müşteri doğrulanır                                        │
 * │    2. Her ürün için stok rezerve edilir  (InventoryService)     │
 * │    3. Order + OrderItem'lar kaydedilir                          │
 * │    4. Audit log yazılır                                         │
 * │  → Herhangi bir adım başarısız olursa HEPSİ rollback olur.     │
 * │  → Bu "free atomicity" dağıtık mimaride KAYBOLUR.              │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * LAB NOTU (Servis Ayrımı):
 *   InventoryService ayrı process'e taşındığında adım 2 HTTP çağrısı olur.
 *   HTTP başarılı ama DB commit başarısız olursa stok rezerve ama sipariş yok.
 *   HTTP başarısız ama timeout belirsiz olursa stok rezerve mi değil mi bilinmez.
 *   Çözüm: Saga Pattern (choreography veya orchestration).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository    orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository  productRepository;
    private final InventoryService   inventoryService;
    private final AuditService       auditService;

    // ─── Queries ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getByRef(String ref) {
        Order order = orderRepository.findByOrderRef(ref)
                .orElseThrow(() -> new NotFoundException("Order not found: " + ref));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toDto).toList();
    }

    // ─── Commands ─────────────────────────────────────────────

    /**
     * Sipariş ver.
     *
     * Tüm işlemler tek @Transactional boundary içinde.
     * Stok rezervasyonu ve sipariş kaydı atomik — ya hepsi, ya hiçbiri.
     */
    @Transactional
    public OrderDto placeOrder(PlaceOrderRequest req) {
        // TODO LAB-5: X-Idempotency-Key kontrolü
        // TODO LAB-4: Timeout deadline — bu metot X ms'den uzun sürerse TimeoutException fırlat
        // TODO LAB-2: Chaos delay — yapay gecikme enjekte et

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + req.getCustomerId()));

        Order order = Order.builder()
                .orderRef(generateOrderRef())
                .customer(customer)
                .status(OrderStatus.PENDING)
                .shippingAddress(req.getShippingAddress())
                .items(new ArrayList<>())
                .build();

        // Her sipariş kalemi için: ürünü bul, stok rezerve et, item ekle
        for (PlaceOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findByIdWithLock(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + itemReq.getProductId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is not active: " + product.getSku());
            }

            // InventoryService.reserve() bu transaction'a katılır.
            // Dağıtık mimaride bu satır HTTP çağrısına dönüşecek → atomiklik bozulacak.
            inventoryService.reserve(product.getId(), itemReq.getQuantity());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            order.getItems().add(item);
        }

        order.recalculateTotal();
        orderRepository.save(order);

        auditService.log("ORDER_PLACED", "Order", order.getId(),
                "ref=" + order.getOrderRef() + " total=" + order.getTotalAmount()
                + " items=" + order.getItems().size());

        log.info("Order placed ref={} customerId={} total={}",
                order.getOrderRef(), customer.getId(), order.getTotalAmount());

        return toDto(order);
    }

    /** Siparişi onayla (ödeme alındı). */
    @Transactional
    public OrderDto confirm(Long id) {
        Order order = getOrderForUpdate(id);
        requireStatus(order, OrderStatus.PENDING);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        auditService.log("ORDER_CONFIRMED", "Order", id, "ref=" + order.getOrderRef());
        return toDto(order);
    }

    /** Siparişi kargoya ver. */
    @Transactional
    public OrderDto ship(Long id) {
        Order order = getOrderForUpdate(id);
        requireStatus(order, OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
        auditService.log("ORDER_SHIPPED", "Order", id, null);
        return toDto(order);
    }

    /** Siparişi teslim edildi olarak işaretle — fiziksel stok düşülür. */
    @Transactional
    public OrderDto deliver(Long id) {
        Order order = getOrderForUpdate(id);
        requireStatus(order, OrderStatus.SHIPPED);
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        // Fiziksel stoktan düş
        for (OrderItem item : order.getItems()) {
            inventoryService.deduct(item.getProduct().getId(), item.getQuantity());
        }

        auditService.log("ORDER_DELIVERED", "Order", id, null);
        return toDto(order);
    }

    /** Siparişi iptal et — rezerve edilen stok serbest bırakılır. */
    @Transactional
    public OrderDto cancel(Long id) {
        Order order = getOrderForUpdate(id);
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Rezervasyonu geri bırak
        for (OrderItem item : order.getItems()) {
            inventoryService.release(item.getProduct().getId(), item.getQuantity());
        }

        auditService.log("ORDER_CANCELLED", "Order", id, null);
        log.info("Order cancelled ref={}", order.getOrderRef());
        return toDto(order);
    }

    // ─── Helpers ──────────────────────────────────────────────

    private Order getOrderForUpdate(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStateException(
                "Expected status %s but was %s".formatted(expected, order.getStatus()));
        }
    }

    private String generateOrderRef() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems() == null ? List.of() :
            o.getItems().stream().map(i -> OrderItemDto.builder()
                .productId(i.getProduct().getId())
                .sku(i.getProduct().getSku())
                .productName(i.getProduct().getName())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.lineTotal())
                .build()).toList();

        return OrderDto.builder()
                .id(o.getId())
                .orderRef(o.getOrderRef())
                .customerId(o.getCustomer().getId())
                .customerName(o.getCustomer().getFullName())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .shippingAddress(o.getShippingAddress())
                .items(items)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
