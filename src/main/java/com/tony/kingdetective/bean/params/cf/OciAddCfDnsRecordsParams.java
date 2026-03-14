package com.tony.kingdetective.bean.params.cf;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @ClassName OciAddCfDnsRecordsParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-21 14:25
 **/
@Data
public class OciAddCfDnsRecordsParams {

    @NotBlank(message = "??ID????")
    private String cfCfgId;
//    @NotBlank(message = "")
    private String prefix;
    @NotBlank(message = "??????")
    private String type;
    @NotBlank(message = "ip??????")
    private String ipAddress;
//    @NotNull(message = "")
    private boolean proxied;
//    @NotNull(message = "ttl")
//    @Min(value = 60)
    private Integer ttl;
    private String comment;
}
