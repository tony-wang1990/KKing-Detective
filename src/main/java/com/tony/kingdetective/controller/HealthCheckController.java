package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.vo.HealthStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 
 * 
 * 
 * @author Tony Wang
 */
@Slf4j
@RestController
@RequestMapping("/actuator")
public class HealthCheckController {
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 
     *
     * @return 
     */
    @GetMapping("/health")
    public HealthStatus health() {
        log.debug("执行健康检查");
        
        boolean databaseOk = checkDatabase();
        boolean memoryOk = checkMemory();
        
        String status = (databaseOk && memoryOk) ? "UP" : "DOWN";
        
        return HealthStatus.builder()
                .status(status)
                .databaseConnectivity(databaseOk)
                .memoryStatus(memoryOk)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 
     */
    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5
        } catch (Exception e) {
            log.error("数据库连接检查失败", e);
            return false;
        }
    }
    
    /**
     * 
     */
    private boolean checkMemory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double usagePercent = (double) usedMemory / maxMemory * 100;
            
            log.debug("内存使用率: {:.2f}%", usagePercent);
            
            // 90%
            return usagePercent < 90;
        } catch (Exception e) {
            log.error("内存检查失败", e);
            return false;
        }
    }
}
