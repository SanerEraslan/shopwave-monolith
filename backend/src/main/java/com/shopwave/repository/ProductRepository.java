package com.shopwave.repository;

import com.shopwave.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p JOIN FETCH p.inventory WHERE p.active = true")
    List<Product> findAllActiveWithInventory();

    // Pessimistic write lock — stok güncellemesi sırasında race condition önlenir.
    // NOT: Bu lock tek bir JVM/DB transaction içinde çalışır.
    // Dağıtık sistemde birden fazla instance varsa bu lock YETERSİZ kalır.
    // → Distributed lock (Redis, Zookeeper) veya optimistic locking gerekir (Lab konusu).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}
