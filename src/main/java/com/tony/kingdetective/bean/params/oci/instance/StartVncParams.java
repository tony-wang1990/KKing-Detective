package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @ClassName StartVncParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-06-04 11:02
 **/
@Data
public class StartVncParams {

    @NotBlank(message = "??ID????")
    private String ociCfgId;
    private String compartmentId;
    @NotBlank(message = "??ID????")
    private String instanceId;
}
