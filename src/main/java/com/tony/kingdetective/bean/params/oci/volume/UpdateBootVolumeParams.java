package com.tony.kingdetective.bean.params.oci.volume;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.oci
 * @className: UpdateBootVolumeParams
 * @author: Tony Wang
 * @date: 2025/1/4 19:27
 */
@Data
public class UpdateBootVolumeParams {

    @NotBlank(message = "??id????")
    private String ociCfgId;
    @NotBlank(message = "???id????")
    private String bootVolumeId;
    @NotBlank(message = "?????????")
    private String bootVolumeSize;
    @NotBlank(message = "???VPU????")
    private String bootVolumeVpu;
}
