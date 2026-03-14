package com.tony.kingdetective.service;

import com.tony.kingdetective.bean.entity.AuditLog;

/**
 * 
 * 
 * @author Tony Wang
 */
public interface IAuditLogService {
    
    /**
     * 
     *
     * @param log 
     */
    void log(AuditLog log);
    
    /**
     * 
     *
     * @param userId ID
     * @param operation 
     * @param target 
     * @param details 
     */
    void logSuccess(String userId, String operation, String target, String details);
    
    /**
     * 
     *
     * @param userId ID
     * @param operation 
     * @param target 
     * @param error 
     */
    void logFailure(String userId, String operation, String target, String error);
}
