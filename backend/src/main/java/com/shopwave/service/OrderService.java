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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    private final Random random = new Random();

    private final ConcurrentHashMap<String, OrderDto> idempotencyMap = new ConcurrentHashMap<>();

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

    @Transactional
    public OrderDto placeOrder(PlaceOrderRequest req) {

        if (req.getIdempotencyKey() != null && idempotencyMap.containsKey(req.getIdempotencyKey())) {
            log.info("LAB-5: Duplicate request detected for key: {}. Returning cached result.",
                    req.getIdempotencyKey());
            return idempotencyMap.get(req.getIdempotencyKey());
        }

        long startTime = System.currentTimeMillis();
        long deadlineMs = 400;

        try {
            int delay = 200 + random.nextInt(300);
            log.info("LAB-2: Injecting chaos delay: {}ms", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Chaos delay interrupted", e);
        }

        long timeElapsed = System.currentTimeMillis() - startTime;
        if (timeElapsed > deadlineMs) {
            log.error("LAB-4: Deadline exceeded! Elapsed: {}ms, Limit: {}ms", timeElapsed, deadlineMs);
            throw new RuntimeException(
                    "Sipariş işlemi zaman aşımına uğradı (Deadline Exceeded: " + timeElapsed + "ms)");
        }

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + req.getCustomerId()));

        Order order = Order.builder()
                .orderRef(generateOrderRef())
                .customer(customer)
                .status(OrderStatus.PENDING)
                .shippingAddress(req.getShippingAddress())
                .items(new ArrayList<>())
                .build();

        for (PlaceOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findByIdWithLock(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + itemReq.getProductId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is not active: " + product.getSku());
            }

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

        OrderDto responseDto = toDto(order);

        if (req.getIdempotencyKey() != null) {
            idempotencyMap.put(req.getIdempotencyKey(), responseDto);
        }

        return responseDto;
    }

    @Transactional
    public OrderDto confirm(Long id) {
        Order order = getOrderForUpdate(id);
        requireStatus(order, OrderStatus.PENDING);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        auditService.log("ORDER_CONFIRMED", "Order", id, "ref=" + order.getOrderRef());
        return toDto(order);
    }

    @Transactional
    public OrderDto ship(Long id) {
        Order order = getOrderForUpdate(id);
        requireStatus(order, OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
        auditService.log("ORDER_SHIPPED", "Order", id, null);
        return toDto(order);
    }

    @Transactional
    public OrderDto deliver(Long id) {
        Order order = getOrderForUpdate(id);
        requireStatus(order, OrderStatus.SHIPPED);
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            inventoryService.deduct(item.getProduct().getId(), item.getQuantity());
        }

        auditService.log("ORDER_DELIVERED", "Order", id, null);
        return toDto(order);
    }

    @Transactional
    public OrderDto cancel(Long id) {
        Order order = getOrderForUpdate(id);
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            inventoryService.release(item.getProduct().getId(), item.getQuantity());
        }

        auditService.log("ORDER_CANCELLED", "Order", id, null);
        log.info("Order cancelled ref={}", order.getOrderRef());
        return toDto(order);
    }

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
        List<OrderItemDto> items = o.getItems() == null ? List.of()
                : o.getItems().stream().map(i -> OrderItemDto.builder()
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