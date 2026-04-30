package com.shopwave.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * AuditLog — tüm kritik işlemler burada kayıt altına alınır.
 *
 * Monolith'te audit kaydı, asıl işlemle AYNI @Transactional içinde yazılır.
 * Bu "free consistency" sağlar: ya ikisi birlikte commit olur, ya ikisi birden rollback.
 *
 * LAB NOTU (Outbox Pattern):
 *   Servisler ayrıldığında audit servisi ayrı bir process'te çalışacak.
 *   Asıl işlem başarılı olup audit yazımı başarısız olabilir → veri kaybı.
 *   Çözüm: Outbox Pattern — audit event önce aynı DB'ye yazılır,
 *   daha sonra bir poller tarafından hedefe iletilir.
 */
@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "aggregate", length = 30)
    private String aggregate;

    @Column(name = "aggregate_id")
    private Long aggregateId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = OffsetDateTime.now(); }
}
