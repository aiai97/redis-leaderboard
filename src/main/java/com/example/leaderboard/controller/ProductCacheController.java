package com.example.leaderboard.controller;

import com.example.leaderboard.dto.ProductRequest;
import com.example.leaderboard.dto.ProductResponse;
import com.example.leaderboard.service.ProductCacheService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/demo/products")
public class ProductCacheController {
    private final ProductCacheService service;

    public ProductCacheController(ProductCacheService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String id,
            @Valid @RequestBody ProductRequest request) {
        return service.update(id, request.name(), request.priceInCents());
    }
}
