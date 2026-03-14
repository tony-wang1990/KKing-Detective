package com.tony.kingdetective.bean.params.oci.volume;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * <p>
 * TerminateBootVolumeParams
 * </p >
 *
 * @author yuhui.fan
 * @since 2025/1/3 19:04
 */
@Data
public class TerminateBootVolumeParams {

    @NotBlank(message = "??id????")
    private String ociCfgId;
    @NotEmpty(message = "???id????")
    private List<String> bootVolumeIds;
}
