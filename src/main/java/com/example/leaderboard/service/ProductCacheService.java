package com.example.leaderboard.service;

import com.example.leaderboard.dto.ProductResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

@Service
public class ProductCacheService {
    private final JedisPooled redis;
    private final ProductCatalog catalog;
    private final ObjectMapper objectMapper;
    private final long ttlSeconds;

    public ProductCacheService(
            JedisPooled redis,
            ProductCatalog catalog,
            ObjectMapper objectMapper,
            @Value("${redis-use-cases.cache.ttl-seconds}") long ttlSeconds) {
        this.redis = redis;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("Product cache TTL must be positive");
        }
    }

    public ProductResponse get(String id) {
        String cacheKey = cacheKey(id);
        String cached = redis.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ProductResponse.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Could not deserialize cached product " + id, exception);
            }
        }
        ProductResponse product = catalog.get(id);
        try {
            redis.set(cacheKey, objectMapper.writeValueAsString(product), SetParams.setParams().ex(ttlSeconds));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize product " + id, exception);
        }
        return product;
    }

    public ProductResponse update(String id, String name, long priceInCents) {
        ProductResponse updated = catalog.update(id, name, priceInCents);
        redis.del(cacheKey(id));
        return updated;
    }

    private String cacheKey(String id) {
        return "cache:product:" + id;
    }
}
