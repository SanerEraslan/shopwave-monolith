package com.shopwave.repository;

import com.shopwave.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAggregateAndAggregateIdOrderByCreatedAtDesc(String aggregate, Long aggregateId);
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
