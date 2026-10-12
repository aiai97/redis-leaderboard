package com.example.leaderboard.service;

import com.example.leaderboard.dto.ProductResponse;
import com.example.leaderboard.exception.ProductNotFoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class ProductCatalog {
    private final ConcurrentMap<String, ProductResponse> products = new ConcurrentHashMap<>();

    public ProductCatalog() {
        products.put("42", new ProductResponse("42", "Keyboard", 29900));
        products.put("43", new ProductResponse("43", "Mouse", 9900));
    }

    public ProductResponse get(String id) {
        ProductResponse product = products.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public ProductResponse update(String id, String name, long priceInCents) {
        ProductResponse product = new ProductResponse(id, name, priceInCents);
        products.put(id, product);
        return product;
    }
}
