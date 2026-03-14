package com.tony.kingdetective.bean.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 
 * 
 * 
 * @author Tony Wang
 */
@Data
@TableName("audit_log")
public class AuditLog {
    
    /** ID */
    @TableId
    private String id;
    
    /** ID */
    @TableField("user_id")
    private String userId;
    
    /**  */
    @TableField("username")
    private String username;
    
    /**  */
    @TableField("operation")
    private String operation;
    
    /** IDID */
    @TableField("target")
    private String target;
    
    /** JSON */
    @TableField("details")
    private String details;
    
    /**  */
    @TableField("success")
    private Boolean success;
    
    /**  */
    @TableField("error_message")
    private String errorMessage;
    
    /** IP */
    @TableField("ip_address")
    private String ipAddress;
    
    /**  */
    @TableField("user_agent")
    private String userAgent;
    
    /**  */
    @TableField("create_time")
    private LocalDateTime createTime;
}
