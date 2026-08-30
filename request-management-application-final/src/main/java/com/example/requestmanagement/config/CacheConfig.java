package com.example.requestmanagement.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManager cacheManager(@Value("${app.cache.request.max-size:10000}") long maximumSize,
                              @Value("${app.cache.request.ttl-seconds:30}") long ttlSeconds) {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager("requests");
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(maximumSize).expireAfterWrite(Duration.ofSeconds(ttlSeconds)).recordStats());
        return cacheManager;
    }
}