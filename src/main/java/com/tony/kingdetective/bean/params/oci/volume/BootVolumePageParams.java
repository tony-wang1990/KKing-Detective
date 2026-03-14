package com.tony.kingdetective.bean.params.oci.volume;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * BootVolumePageParams
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/31 15:32
 */
@Data
public class BootVolumePageParams {

    @NotBlank(message = "??id????")
    private String ociCfgId;
    private String keyword;
    private int currentPage;
    private int pageSize;
    private boolean cleanReLaunch;
}
