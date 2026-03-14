package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: TerminateInstanceParams
 * @author: Tony Wang
 * @date: 2024/11/28 21:48
 */
@Data
public class TerminateInstanceParams {

    @NotBlank(message = "??id????")
    private String ociCfgId;

    @NotBlank(message = "??id????")
    private String instanceId;

    @NotNull(message = "???????????")
    private Integer preserveBootVolume;

    @NotBlank(message = "???????")
    private String captcha;
}
