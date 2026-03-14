package com.tony.kingdetective.bean.params.cf;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @ClassName AddCfCfgParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-20 16:57
 **/
@Data
public class AddCfCfgParams {

    @NotBlank(message = "??????")
    private String domain;
    @NotBlank(message = "??ID????")
    private String zoneId;
    @NotBlank(message = "API??????")
    private String apiToken;
}
