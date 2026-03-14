package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName CreateNetworkLoadBalancerParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-08-21 09:41
 **/
@Data
public class CreateNetworkLoadBalancerParams {

    @NotBlank(message = "ociCfgId????")
    private String ociCfgId;
    @NotBlank(message = "instanceId????")
    private String instanceId;
    @NotNull(message = "sshPort????")
    private Integer sshPort;
}
