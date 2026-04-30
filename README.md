# ShopWave — Monolith

Dağıtık Sistemler lab dersleri için hazırlanmış **başlangıç noktası**.  
E-ticaret temalı, tam çalışan bir monolitik uygulama.

## Stack

| Katman    | Teknoloji |
|-----------|-----------|
| Backend   | Spring Boot 3.2, Java 21, JPA, Flyway |
| Veritabanı| PostgreSQL 15 |
| Frontend  | React 18, TypeScript, Vite, Tailwind CSS |
| Infra     | Docker Compose |

## Başlatmak

```bash
docker compose up --build
```

| Servis        | URL                                     |
|---------------|-----------------------------------------|
| Frontend      | http://localhost:3000                   |
| Backend API   | http://localhost:8080/api/v1/products   |
| Health        | http://localhost:8080/health            |
| Actuator      | http://localhost:8080/actuator/health   |
| DB            | localhost:5432 / shopwave               |

## API Özeti

```bash
# Ürünler
GET  /api/v1/products
GET  /api/v1/products/{id}
POST /api/v1/products
PATCH /api/v1/products/{id}/price

# Siparişler
POST /api/v1/orders              # sipariş ver
POST /api/v1/orders/{id}/confirm
POST /api/v1/orders/{id}/ship
POST /api/v1/orders/{id}/deliver
POST /api/v1/orders/{id}/cancel
GET  /api/v1/orders/customer/{customerId}

# Stok
POST /api/v1/inventory/products/{id}/add
GET  /api/v1/inventory/low-stock

# Audit
GET  /api/v1/audit

# Müşteriler
GET  /api/v1/customers
```

---

## Mimari Notlar (Lab Başlangıcı)

Bu sistemde şu anda:

- **Tek process** çalışır (backend)
- **Tek veritabanı** kullanılır (shopwave)
- Sipariş + stok rezervasyonu **tek transaction** içinde gerçekleşir
- **Ağ belirsizliği yoktur**
- Tüm audit logları asıl işlemle **atomik** yazılır

Bu özellikler sistem dağıtıklaştıkça **birer birer ortadan kalkacak**.

---

## Lab Yol Haritası

### LAB-1 — Monolith Sağlamlaştırma
- [ ] Structured logging (JSON formatı)
- [ ] Her isteğe `X-Correlation-ID` eklenmesi
- [ ] MDC ile tüm log satırlarında correlation-id görüntülenmesi
- [ ] p95 / p99 gecikme ölçümü (Micrometer)

### LAB-2 — Kaos & Gecikme Enjeksiyonu
- [ ] `OrderService.placeOrder()` içine yapay gecikme eklenmesi
- [ ] Jitter ile gecikme varyasyonu
- [ ] Race condition gözlemlenmesi (paralel istek atılması)

### LAB-3 — Kod İçi Sınırların Netleştirilmesi
- [ ] Inventory, Order, Catalog, Audit modüllerinin paket bazında ayrılması
- [ ] Veri sahipliğinin belgelenmesi
- [ ] Servis sınırlarının çizilmesi

### LAB-4 — Timeout & Deadline
- [ ] `InventoryService.reserve()` için maksimum süre sınırı
- [ ] `OrderService.placeOrder()` için toplam deadline
- [ ] Timeout aşımında anlamlı hata yanıtı

### LAB-5 — Idempotency
- [ ] `POST /api/v1/orders` için `X-Idempotency-Key` desteği
- [ ] Aynı key ile ikinci isteğin ilk sonucu dönmesi
- [ ] Key TTL yönetimi

### LAB-6 — Servis Ayrımı (Gerçek Dağıtıklık)
- [ ] `InventoryService` → ayrı Spring Boot uygulaması
- [ ] Ayrı PostgreSQL DB
- [ ] HTTP client entegrasyonu (`RestClient` veya `WebClient`)
- [ ] Ağ hatası ve timeout senaryolarının gözlemlenmesi
- [ ] Partial failure: sipariş kaydedildi ama stok rezerve edilemedi

### LAB-7 — Resilience
- [ ] Retry (exponential backoff + jitter)
- [ ] Circuit Breaker (Resilience4j)
- [ ] Fallback stratejisi
- [ ] Bulkhead izolasyonu

### LAB-8 — Saga Pattern
- [ ] Choreography-based saga (event'lerle)
- [ ] Compensation: `ORDER_CANCELLED` → stok geri bırakılır
- [ ] Outbox Pattern: audit event'lerin güvenli iletimi

### LAB-9 — Gözlemlenebilirlik
- [ ] Distributed tracing (Micrometer Tracing + Zipkin)
- [ ] Prometheus metrikleri
- [ ] Grafana dashboard
- [ ] Golden signals: latency, traffic, errors, saturation
