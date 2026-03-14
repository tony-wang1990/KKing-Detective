package com.tony.kingdetective.bean.params.oci.securityrule;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * @ClassName RemoveSecurityRuleParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-05 16:06
 **/
@Data
public class RemoveSecurityRuleParams {

    @NotBlank(message = "api??id????")
    private String ociCfgId;
    @NotBlank(message = "vcnId????")
    private String vcnId;
    private Integer type;
    private List<String> ruleIds;
}
