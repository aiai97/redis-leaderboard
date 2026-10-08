package com.example.leaderboard.config;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Bean(destroyMethod = "close")
    JedisPooled jedisPooled(
            @Value("${leaderboard.redis.host}") String host,
            @Value("${leaderboard.redis.port}") int port,
            @Value("${leaderboard.redis.timeout-millis}") int timeoutMillis) {
        var clientConfig = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(timeoutMillis)
                .socketTimeoutMillis(timeoutMillis)
                .build();
        return new JedisPooled(new HostAndPort(host, port), clientConfig);
    }
}
