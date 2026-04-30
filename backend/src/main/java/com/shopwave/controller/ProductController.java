package com.shopwave.controller;

import com.shopwave.dto.ProductDto;
import com.shopwave.service.ProductService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDto> list(
            @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) return productService.listByCategory(categoryId);
        return productService.listAll();
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/sku/{sku}")
    public ProductDto getBySku(@PathVariable String sku) {
        return productService.getBySku(sku);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@RequestBody Map<String, Object> body) {
        return productService.create(
                (String)  body.get("sku"),
                (String)  body.get("name"),
                (String)  body.get("description"),
                new BigDecimal(body.get("price").toString()),
                body.get("categoryId") != null ? Long.valueOf(body.get("categoryId").toString()) : null,
                body.get("initialStock") != null ? Integer.parseInt(body.get("initialStock").toString()) : 0
        );
    }

    @PatchMapping("/{id}/price")
    public ProductDto updatePrice(@PathVariable Long id,
                                  @RequestBody Map<String, Object> body) {
        return productService.updatePrice(id, new BigDecimal(body.get("price").toString()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        productService.deactivate(id);
    }
}
