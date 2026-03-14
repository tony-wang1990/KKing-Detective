package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSnapshotParams {
    @NotBlank(message = "ociCfgId????")
    private String ociCfgId;

    @NotBlank(message = "instanceId????")
    private String instanceId;

    private String snapshotName; // Optional
}
