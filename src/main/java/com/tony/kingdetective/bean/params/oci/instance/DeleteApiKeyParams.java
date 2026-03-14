package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteApiKeyParams {
    @NotBlank(message = "ociCfgId????")
    private String ociCfgId;

    @NotBlank(message = "fingerprint????")
    private String fingerprint;
}
