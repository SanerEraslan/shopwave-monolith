package com.shopwave.repository;

import com.shopwave.domain.Order;
import com.shopwave.domain.Order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderRef(String orderRef);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
