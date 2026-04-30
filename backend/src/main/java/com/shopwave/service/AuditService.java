package com.shopwave.service;

import com.shopwave.domain.AuditLog;
import com.shopwave.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AuditService — kritik işlemleri kayıt altına alır.
 *
 * Monolith'te REQUIRES_NEW propagation kullanılabilir ama gerek yok;
 * asıl transaction zaten bu kaydı da kapsar.
 *
 * LAB NOTU:
 *   Servisler ayrıldığında audit yazımı network üzerinden gidecek.
 *   Asıl işlem commit → audit çağrısı başarısız → kayıp event.
 *   Çözüm: Outbox Pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String eventType, String aggregate, Long aggregateId, String payload) {
        // TODO LAB-1: MDC'deki correlation-id'yi payload'a ekle
        AuditLog entry = AuditLog.builder()
                .eventType(eventType)
                .aggregate(aggregate)
                .aggregateId(aggregateId)
                .payload(payload)
                .build();
        auditLogRepository.save(entry);
        log.info("AUDIT event={} aggregate={} id={}", eventType, aggregate, aggregateId);
    }

    public List<AuditLog> getByAggregate(String aggregate, Long id) {
        return auditLogRepository.findByAggregateAndAggregateIdOrderByCreatedAtDesc(aggregate, id);
    }

    public List<AuditLog> getRecent() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
