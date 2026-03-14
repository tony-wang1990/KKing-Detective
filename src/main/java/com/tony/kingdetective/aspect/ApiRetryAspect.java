package com.tony.kingdetective.aspect;

import com.oracle.bmc.model.BmcException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * API 
 *  Oracle Cloud API 
 * 
 * @author Tony Wang
 */
@Slf4j
@Aspect
@Component
public class ApiRetryAspect {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;  // 1
    private static final double BACKOFF_MULTIPLIER = 2.0;  // 
    
    /**
     *  OCI API 
     */
    @Around("@annotation(com.tony.kingdetective.annotation.RetryableOciApi)")
    public Object retryOciApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;
        
        while (true) {
            attempt++;
            try {
                log.debug("执行 API 调用: {} (尝试 {}/{})", methodName, attempt, MAX_RETRY_ATTEMPTS);
                Object result = joinPoint.proceed();
                
                if (attempt > 1) {
                    log.info("API 调用成功: {} (重试 {} 次后成功)", methodName, attempt - 1);
                }
                
                return result;
                
            } catch (BmcException e) {
                boolean shouldRetry = shouldRetry(e, attempt);
                
                if (shouldRetry) {
                    log.warn("API 调用失败: {}, 错误码: {}, 将在 {}ms 后重试 ({}/{})",
                            methodName, e.getStatusCode(), backoffMs, attempt, MAX_RETRY_ATTEMPTS);
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                    
                    // 
                    backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
                    
                } else {
                    log.error("API 调用失败: {}, 不再重试 (尝试 {} 次)", methodName, attempt);
                    throw e;
                }
                
            } catch (Exception e) {
                //  BmcException 
                log.error("API 调用发生非预期异常: {}", methodName, e);
                throw e;
            }
        }
    }
    
    /**
     * 
     *
     * @param e BmcException
     * @param attempt 
     * @return true 
     */
    private boolean shouldRetry(BmcException e, int attempt) {
        // 
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            return false;
        }
        
        int statusCode = e.getStatusCode();
        
        // 
        // 429: Too Many Requests ()
        // 500: Internal Server Error ()
        // 502: Bad Gateway ()
        // 503: Service Unavailable ()
        // 504: Gateway Timeout ()
        return statusCode == 429 ||
               statusCode == 500 ||
               statusCode == 502 ||
               statusCode == 503 ||
               statusCode == 504;
    }
}
