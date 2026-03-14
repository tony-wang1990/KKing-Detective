package com.tony.kingdetective.bean.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 
 * 
 * @author Tony Wang
 */
@Data
@Builder
public class HealthStatus {
    /** UP / DOWN */
    private String status;
    
    /**  */
    private Boolean databaseConnectivity;
    
    /**  */
    private Boolean memoryStatus;
    
    /**  */
    private Long timestamp;
}
