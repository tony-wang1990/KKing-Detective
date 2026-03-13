package com.tony.kingdetective.telegram.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实例创建方案模型
 * 
 * @author Tony Wang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstancePlan {
    
    /**
     * CPU 核心�?
     */
    private Integer ocpus;
    
    /**
     * 内存大小 (GB)
     */
    private Integer memory;
    
    /**
     * 磁盘大小 (GB)
     */
    private Integer disk;
    
    /**
     * 架构类型 (例如：VM.Standard.E2.1.Micro, VM.Standard.A1.Flex)
     */
    private String architecture;
    
    /**
     * 操作系统
     */
    private String operationSystem;
    
    /**
     * 间隔时间 (�?
     */
    private Integer interval;
    
    /**
     * 创建实例数量
     */
    private Integer createNumbers;
    
    /**
     * Root 密码（可选，如果未提供将自动生成�?
     */
    private String rootPassword;
    
    /**
     * 是否�?TG 频道推送开机成功信�?
     */
    @Builder.Default
    private boolean joinChannelBroadcast = true;
}
