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
        log.info("================== ?????? ==================");
        
        // 1. 
        if (virtualExecutor != null) {
            shutdownExecutor(virtualExecutor, "VirtualExecutor");
        }
        
        // 2. 
        log.info("??????????...");
        
        // 3. Spring 
        log.info("???????...");
        
        // 4. 
        log.info("????...");
        
        log.info("================== ?????? ==================");
    }
    
    /**
     * 
     *
     * @param executor 
     * @param name 
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        log.info("???????: {}", name);
        
        // 
        executor.shutdown();
        
        try {
            // 60
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("??? {} ?60??????????????", name);
                
                // 
                executor.shutdownNow();
                
                // 30
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("??? {} ??????", name);
                }
            } else {
                log.info("??? {} ?????", name);
            }
        } catch (InterruptedException e) {
            log.error("???????????: {}", name, e);
            
            // 
            executor.shutdownNow();
            
            // 
            Thread.currentThread().interrupt();
        }
    }
}
