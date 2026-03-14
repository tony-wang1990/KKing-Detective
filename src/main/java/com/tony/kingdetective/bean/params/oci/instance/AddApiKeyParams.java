package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddApiKeyParams {
    @NotBlank(message = "ociCfgId不能为空")
    private String ociCfgId;

    @NotBlank(message = "publicKeyContent不能为空")
    private String publicKeyContent;
}
