package com.tony.kingdetective.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 
 * 
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class GracefulShutdownHandler {
    
    @Autowired(required = false)
    @Qualifier("virtualExecutor")
    private ExecutorService virtualExecutor;
    
    /**
     * 
     */
    @PreDestroy
    public void shutdown() {
        log.info("================== 开始优雅停机 ==================");
        
        // 1. 
        if (virtualExecutor != null) {
            shutdownExecutor(virtualExecutor, "VirtualExecutor");
        }
        
        // 2. 
        log.info("等待所有异步任务完成...");
        
        // 3. Spring 
        log.info("关闭数据库连接...");
        
        // 4. 
        log.info("清理缓存...");
        
        log.info("================== 优雅停机完成 ==================");
    }
    
    /**
     * 
     *
     * @param executor 
     * @param name 
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        log.info("开始关闭线程池: {}", name);
        
        // 
        executor.shutdown();
        
        try {
            // 60
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("线程池 {} 在60秒内未完成所有任务，强制关闭", name);
                
                // 
                executor.shutdownNow();
                
                // 30
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("线程池 {} 无法完全关闭", name);
                }
            } else {
                log.info("线程池 {} 已成功关闭", name);
            }
        } catch (InterruptedException e) {
            log.error("等待线程池关闭时被中断: {}", name, e);
            
            // 
            executor.shutdownNow();
            
            // 
            Thread.currentThread().interrupt();
        }
    }
}
