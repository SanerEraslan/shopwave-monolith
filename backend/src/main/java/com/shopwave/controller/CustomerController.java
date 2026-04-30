package com.shopwave.controller;

import com.shopwave.domain.Customer;
import com.shopwave.exception.NotFoundException;
import com.shopwave.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;

    @GetMapping
    public List<Customer> list() {
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Customer getById(@PathVariable Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }
}
