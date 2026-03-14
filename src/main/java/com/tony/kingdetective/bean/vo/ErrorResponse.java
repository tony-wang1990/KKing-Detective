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
public class ErrorResponse {
    /**  */
    private Integer code;
    
    /**  */
    private String message;
    
    /** () */
    private String details;
    
    /**  */
    private Long timestamp;
}
