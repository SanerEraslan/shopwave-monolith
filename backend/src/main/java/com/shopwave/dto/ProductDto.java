package com.shopwave.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private boolean active;
    private Integer availableStock;  // inventory.quantity - inventory.reserved
    private OffsetDateTime createdAt;
}
