package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * UpdateInstanceCfgParams
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/18 18:13
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateInstanceCfgParams extends GetInstanceCfgInfoParams {

    @NotBlank(message = "cpu????")
    private String ocpus;
    @NotBlank(message = "??????")
    private String memory;
}
