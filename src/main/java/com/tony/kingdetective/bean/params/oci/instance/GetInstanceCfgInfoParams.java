package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * GetInstanceCfgInfoParams
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/18 18:02
 */
@Data
public class GetInstanceCfgInfoParams {

    @NotBlank(message = "??id????")
    private String ociCfgId;

    @NotBlank(message = "??id????")
    private String instanceId;
}
