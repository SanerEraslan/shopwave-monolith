package com.shopwave.controller;

import com.shopwave.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/low-stock")
    public Object lowStock(@RequestParam(defaultValue = "10") int threshold) {
        return inventoryService.getLowStock(threshold);
    }

    @PostMapping("/products/{productId}/add")
    public Object addStock(@PathVariable Long productId,
                           @RequestBody Map<String, Integer> body) {
        inventoryService.addStock(productId, body.get("quantity"));
        return Map.of("message", "Stock added");
    }
}
