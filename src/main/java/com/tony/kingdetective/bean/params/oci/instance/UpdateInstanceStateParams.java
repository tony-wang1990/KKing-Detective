package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: UpdateInstanceStateParams
 * @author: Tony Wang
 * @date: 2024/11/28 21:28
 */
@Data
public class UpdateInstanceStateParams {

    @NotBlank(message = "??id????")
    private String ociCfgId;

    @NotBlank(message = "??id????")
    private String instanceId;

    @NotBlank(message = "????????")
    private String action;

}
