package com.shopwave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ShopWave Monolith — E-Commerce sistemi.
 *
 * Mimari Notlar:
 * ─────────────
 * Bu uygulama kasıtlı olarak monolitik tasarlanmıştır.
 * Tüm domain'ler (Catalog, Inventory, Order, Audit) tek process içinde,
 * tek veritabanına bağlı, senkron metot çağrıları ile birbirine bağlıdır.
 *
 * İlerideki lab adımlarında:
 *   1. Inventory ayrı bir servis haline getirilecek
 *   2. HTTP çağrısı ile iletişim kurulacak
 *   3. Ağ hataları, timeout ve tutarsızlık sorunları gözlemlenecek
 *   4. Resilience mekanizmaları (circuit breaker, retry, saga) eklenecek
 */
@SpringBootApplication
public class ShopWaveApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopWaveApplication.class, args);
    }
}
