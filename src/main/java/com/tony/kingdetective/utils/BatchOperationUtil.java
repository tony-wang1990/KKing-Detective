package com.tony.kingdetective.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.service.IOciKvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 
 * 
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class BatchOperationUtil {
    
    /**
     * 
     *
     * @param ids ID
     * @param queryFunction 
     * @param <T> 
     * @return ID
     */
    public static <T> Map<String, T> batchQuery(
            List<String> ids,
            Function<List<String>, List<T>> queryFunction,
            Function<T, String> idExtractor) {
        
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        
        long startTime = System.currentTimeMillis();
        
        // 
        List<T> results = queryFunction.apply(ids);
        
        //  Map
        Map<String, T> resultMap = results.stream()
                .collect(Collectors.toMap(idExtractor, Function.identity()));
        
        long duration = System.currentTimeMillis() - startTime;
        log.debug("批量查询完成: {} 条记录, 耗时: {}ms", results.size(), duration);
        
        return resultMap;
    }
    
    /**
     * 
     *
     * @param items 
     * @param batchSize 
     * @param updateFunction 
     * @param <T> 
     * @return 
     */
    public static <T> int batchUpdate(
            List<T> items,
            int batchSize,
            Function<List<T>, Integer> updateFunction) {
        
        if (items == null || items.isEmpty()) {
            return 0;
        }
        
        long startTime = System.currentTimeMillis();
        int totalUpdated = 0;
        
        // 
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            List<T> batch = items.subList(i, endIndex);
            
            int updated = updateFunction.apply(batch);
            totalUpdated += updated;
            
            log.debug("批次 {}/{} 完成: 更新 {} 条记录",
                    (i / batchSize) + 1,
                    ((items.size() + batchSize - 1) / batchSize),
                    updated);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("批量更新完成: {} 条记录, 耗时: {}ms", totalUpdated, duration);
        
        return totalUpdated;
    }
    
    /**
     * 
     *
     * @param ids ID
     * @param batchSize 
     * @param deleteFunction 
     * @return 
     */
    public static int batchDelete(
            List<String> ids,
            int batchSize,
            Function<List<String>, Integer> deleteFunction) {
        
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        long startTime = System.currentTimeMillis();
        int totalDeleted = 0;
        
        // 
        for (int i = 0; i < ids.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, ids.size());
            List<String> batch = ids.subList(i, endIndex);
            
            int deleted = deleteFunction.apply(batch);
            totalDeleted += deleted;
            
            log.debug("批次 {}/{} 完成: 删除 {} 条记录",
                    (i / batchSize) + 1,
                    ((ids.size() + batchSize - 1) / batchSize),
                    deleted);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("批量删除完成: {} 条记录, 耗时: {}ms", totalDeleted, duration);
        
        return totalDeleted;
    }
}
