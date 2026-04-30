package com.shopwave.controller;

import com.shopwave.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SystemController {

    private final AuditService auditService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "shopwave-monolith",
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/api/v1/audit")
    public Object recentAuditLogs() {
        return auditService.getRecent();
    }

    @GetMapping("/api/v1/audit/{aggregate}/{id}")
    public Object auditByAggregate(@PathVariable String aggregate,
                                    @PathVariable Long id) {
        return auditService.getByAggregate(aggregate, id);
    }
}
