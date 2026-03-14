package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddApiKeyParams {
    @NotBlank(message = "ociCfgId????")
    private String ociCfgId;

    @NotBlank(message = "publicKeyContent????")
    private String publicKeyContent;
}
