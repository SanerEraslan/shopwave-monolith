package com.shopwave.service;

import com.shopwave.domain.Category;
import com.shopwave.domain.Inventory;
import com.shopwave.domain.Product;
import com.shopwave.dto.ProductDto;
import com.shopwave.exception.NotFoundException;
import com.shopwave.repository.CategoryRepository;
import com.shopwave.repository.InventoryRepository;
import com.shopwave.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository     productRepository;
    private final CategoryRepository    categoryRepository;
    private final InventoryRepository   inventoryRepository;
    private final AuditService          auditService;

    // ─── Queries ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductDto> listAll() {
        return productRepository.findAllActiveWithInventory()
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public ProductDto getBySku(String sku) {
        Product p = productRepository.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Product not found: " + sku));
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId)
                .stream().map(this::toDto).toList();
    }

    // ─── Commands ─────────────────────────────────────────────

    @Transactional
    public ProductDto create(String sku, String name, String description,
                             BigDecimal price, Long categoryId, int initialStock) {
        // TODO LAB-5: X-Idempotency-Key kontrolü — aynı SKU çift gelirse ikinci isteği ignore et

        if (productRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + sku);
        }

        Category category = categoryId != null
                ? categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId))
                : null;

        Product product = Product.builder()
                .sku(sku).name(name).description(description)
                .price(price).category(category).active(true)
                .build();
        productRepository.save(product);

        Inventory inv = Inventory.builder()
                .product(product).quantity(initialStock).reserved(0)
                .build();
        inventoryRepository.save(inv);

        // Monolith: audit aynı transaction içinde yazılır — atomik
        auditService.log("PRODUCT_CREATED", "Product", product.getId(),
                "sku=" + sku + " stock=" + initialStock);

        log.info("Product created sku={} id={}", sku, product.getId());
        return toDto(product);
    }

    @Transactional
    public ProductDto updatePrice(Long id, BigDecimal newPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        BigDecimal oldPrice = product.getPrice();
        product.setPrice(newPrice);
        productRepository.save(product);

        auditService.log("PRODUCT_PRICE_UPDATED", "Product", id,
                "old=" + oldPrice + " new=" + newPrice);
        return toDto(product);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        product.setActive(false);
        productRepository.save(product);
        auditService.log("PRODUCT_DEACTIVATED", "Product", id, null);
    }

    // ─── Mapper ───────────────────────────────────────────────

    ProductDto toDto(Product p) {
        Integer available = null;
        if (p.getInventory() != null) {
            available = p.getInventory().availableQuantity();
        }
        return ProductDto.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .active(p.isActive())
                .availableStock(available)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
