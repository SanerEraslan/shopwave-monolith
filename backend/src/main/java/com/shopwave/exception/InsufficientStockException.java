package com.shopwave.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int available, int requested) {
        super("Insufficient stock for product %d: available=%d, requested=%d"
                .formatted(productId, available, requested));
    }
}
