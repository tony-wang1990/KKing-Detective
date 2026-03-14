package com.tony.kingdetective.utils;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.google.common.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * <p>
 * CustomExpiryGuavaCache
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/31 13:50
 */
@Slf4j
@Component
public class CustomExpiryGuavaCache<K, V> {

    /**
     * 
     *
     * @param <V>
     */
    private static class CacheValue<V> {
        private final V value;
        private final long expiryTime;

        public CacheValue(V value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        public V getValue() {
            return value;
        }

        public boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }
    }

    private final Cache<K, CacheValue<V>> cache;
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1,
            ThreadFactoryBuilder.create().setDaemon(true).setNamePrefix("clean-cache-task-").build());

    public CustomExpiryGuavaCache() {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS) // 
                .removalListener((RemovalListener<K, CacheValue<V>>) notification -> {
                    if (notification.wasEvicted()) {
                        log.info("cache key: [{}] was evicted.", notification.getKey());
                    }
                })
                .build();

        // 
        SCHEDULER.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            cache.asMap().entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 
     *
     * @param key
     * @param value
     * @param ttlMillis
     */
    public void put(K key, V value, long ttlMillis) {
        long expiryTime = System.currentTimeMillis() + ttlMillis;
        cache.put(key, new CacheValue<>(value, expiryTime));
    }

    /**
     * 
     *
     * @param key
     * @return
     */
    public V get(K key) {
        CacheValue<V> cacheValue = cache.getIfPresent(key);
        if (cacheValue == null || cacheValue.isExpired(System.currentTimeMillis())) {
            cache.invalidate(key); // 
            return null;
        }
        return cacheValue.getValue();
    }

    /**
     * 
     *
     * @param key
     */
    public void remove(K key) {
        cache.invalidate(key);
    }

    /**
     * 
     */
    public void cleanUp() {
        cache.cleanUp();
    }

}