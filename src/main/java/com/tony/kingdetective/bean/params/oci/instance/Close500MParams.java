package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName Close500MParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-08-25 10:08
 **/
@Data
public class Close500MParams {

    @NotBlank(message = "ociCfgId????")
    private String ociCfgId;
    @NotBlank(message = "instanceId????")
    private String instanceId;
    @NotNull(message = "???????????????")
    private Boolean retainBl;
    @NotNull(message = "????NAT??????")
    private Boolean retainNatGw;
}
