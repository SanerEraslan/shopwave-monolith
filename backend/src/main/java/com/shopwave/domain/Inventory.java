package com.shopwave.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Inventory — stok tablosu.
 *
 * Monolith'te bu entity, OrderService ile aynı @Transactional boundary içinde
 * güncellenir. Sipariş verildiğinde:
 *   1. reserved artar  (stok ayrılır)
 *   2. order kaydedilir
 *   3. Her ikisi de tek commit ile kalıcı hale gelir.
 *
 * LAB NOTU (Servis Ayrımı):
 *   Bu tablo ileride ayrı bir "inventory-service"e taşındığında,
 *   adım 1 ve 2 farklı process'lerde çalışacak.
 *   Transaction atomikliği kaybolacak → Saga Pattern gerekecek.
 */
@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reserved;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public int availableQuantity() {
        return quantity - reserved;
    }

    public boolean canReserve(int amount) {
        return availableQuantity() >= amount;
    }

    /** Stok ayır — sipariş onaylanana kadar rezerve tutulur. */
    public void reserve(int amount) {
        if (!canReserve(amount)) {
            throw new IllegalStateException(
                "Insufficient stock: available=%d, requested=%d"
                    .formatted(availableQuantity(), amount));
        }
        this.reserved += amount;
    }

    /** Rezervasyonu iptal et (sipariş iptalinde). */
    public void release(int amount) {
        this.reserved = Math.max(0, this.reserved - amount);
    }

    /** Sipariş teslim edildiğinde fiziksel stoktan düş. */
    public void deduct(int amount) {
        this.reserved  = Math.max(0, this.reserved  - amount);
        this.quantity  = Math.max(0, this.quantity  - amount);
    }
}
