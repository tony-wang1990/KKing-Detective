package com.tony.kingdetective.telegram.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * 
 * @author Tony Wang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstancePlan {
    
    /**
     * CPU ?
     */
    private Integer ocpus;
    
    /**
     *  (GB)
     */
    private Integer memory;
    
    /**
     *  (GB)
     */
    private Integer disk;
    
    /**
     *  (VM.Standard.E2.1.Micro, VM.Standard.A1.Flex)
     */
    private String architecture;
    
    /**
     * 
     */
    private String operationSystem;
    
    /**
     *  (?
     */
    private Integer interval;
    
    /**
     * 
     */
    private Integer createNumbers;
    
    /**
     * Root ?
     */
    private String rootPassword;
    
    /**
     * ?TG ?
     */
    @Builder.Default
    private boolean joinChannelBroadcast = true;
}
