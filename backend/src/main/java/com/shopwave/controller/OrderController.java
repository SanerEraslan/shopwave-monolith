package com.shopwave.controller;

import com.shopwave.dto.OrderDto;
import com.shopwave.dto.PlaceOrderRequest;
import com.shopwave.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public OrderDto getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping("/ref/{ref}")
    public OrderDto getByRef(@PathVariable String ref) {
        return orderService.getByRef(ref);
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderDto> getByCustomer(@PathVariable Long customerId) {
        return orderService.getByCustomer(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto place(@Valid @RequestBody PlaceOrderRequest req) {
        // TODO LAB-5: X-Idempotency-Key header'ını oku ve OrderService'e ilet
        return orderService.placeOrder(req);
    }

    @PostMapping("/{id}/confirm")
    public OrderDto confirm(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @PostMapping("/{id}/ship")
    public OrderDto ship(@PathVariable Long id) {
        return orderService.ship(id);
    }

    @PostMapping("/{id}/deliver")
    public OrderDto deliver(@PathVariable Long id) {
        return orderService.deliver(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
