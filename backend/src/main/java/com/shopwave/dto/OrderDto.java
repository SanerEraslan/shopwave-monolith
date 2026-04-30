package com.shopwave.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopwave.domain.Order.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDto {
    private Long id;
    private String orderRef;
    private Long customerId;
    private String customerName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemDto> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItemDto {
        private Long productId;
        private String sku;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
