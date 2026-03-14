package com.tony.kingdetective.bean.params.oci.securityrule;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * <p>
 * ReleaseSecurityRuleParams
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/18 17:59
 */
@Data
public class ReleaseSecurityRuleParams {

    @NotBlank(message = "??id????")
    private String ociCfgId;
}
