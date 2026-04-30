package com.shopwave.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {

    @NotNull
    private Long customerId;

    private String shippingAddress;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {

        @NotNull
        private Long productId;

        @Positive
        private int quantity;
    }
}
