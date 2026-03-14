package com.tony.kingdetective.bean.params.oci.tenant;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;

/**
 * @ClassName UpdateUserInfoParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-14 18:07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateUserInfoParams extends UpdateUserBasicParams{

    @NotBlank(message = "??????")
    private String email;
    @NotBlank(message = "dbUserName????")
    private String dbUserName;
    private String description;
}
