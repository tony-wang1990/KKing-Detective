package com.tony.kingdetective.utils;

import com.google.common.util.concurrent.RateLimiter;
import com.tony.kingdetective.exception.OciException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * API 
 *  Token Bucket  API 
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class ApiRateLimiter {
    
    // API
    private static final double QUOTA_QUERY_RATE = 2.0;        // : 2/
    private static final double COST_QUERY_RATE = 1.0;         // : 1/
    private static final double INSTANCE_CREATE_RATE = 0.5;    // : 0.5/(21)
    private static final double INSTANCE_ACTION_RATE = 2.0;    // : 2/
    private static final double NETWORK_CONFIG_RATE = 1.0;     // : 1/
    private static final double DEFAULT_RATE = 5.0;            // : 5/
    
    // 
    private static final double USER_RATE_LIMIT = 10.0;        // 10/
    
    // 
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    
    // 
    private static final int TIMEOUT_SECONDS = 5;
    
    /**
     * 
     */
    public void acquireQuotaQuery(String userId) {
        acquire("quota_query", QUOTA_QUERY_RATE, userId);
    }
    
    /**
     * 
     */
    public void acquireCostQuery(String userId) {
        acquire("cost_query", COST_QUERY_RATE, userId);
    }
    
    /**
     * 
     */
    public void acquireInstanceCreate(String userId) {
        acquire("instance_create", INSTANCE_CREATE_RATE, userId);
    }
    
    /**
     * 
     */
    public void acquireInstanceAction(String userId) {
        acquire("instance_action", INSTANCE_ACTION_RATE, userId);
    }
    
    /**
     * 
     */
    public void acquireNetworkConfig(String userId) {
        acquire("network_config", NETWORK_CONFIG_RATE, userId);
    }
    
    /**
     * API
     */
    public void acquireGeneral(String apiName, String userId) {
        acquire(apiName, DEFAULT_RATE, userId);
    }
    
    /**
     * 
     *
     * @param apiName API
     * @param rate 
     * @param userId ID
     */
    private void acquire(String apiName, double rate, String userId) {
        // 1. 
        RateLimiter userLimiter = userLimiters.computeIfAbsent(
            userId,
            k -> RateLimiter.create(USER_RATE_LIMIT)
        );
        
        if (!userLimiter.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.warn("用户 {} 请求过于频繁，已触发限流", userId);
            throw new OciException(-429, "请求过于频繁，请稍后再试");
        }
        
        // 2. API
        RateLimiter apiLimiter = limiters.computeIfAbsent(
            apiName,
            k -> RateLimiter.create(rate)
        );
        
        if (!apiLimiter.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.warn("API {} 调用过于频繁，用户: {}", apiName, userId);
            throw new OciException(-429, String.format("【%s】调用过于频繁，请稍后再试", apiName));
        }
        
        log.debug("API {} 速率限制检查通过，用户: {}", apiName, userId);
    }
    
    /**
     * 
     *
     * @param userId ID
     */
    public void resetUserLimit(String userId) {
        userLimiters.remove(userId);
        log.info("已重置用户 {} 的速率限制", userId);
    }
    
    /**
     * API
     *
     * @param apiName API
     */
    public void resetApiLimit(String apiName) {
        limiters.remove(apiName);
        log.info("已重置 API {} 的速率限制", apiName);
    }
    
    /**
     * 
     */
    public void resetAll() {
        limiters.clear();
        userLimiters.clear();
        log.info("已清空所有速率限制");
    }
}
