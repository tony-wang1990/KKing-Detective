package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName UpdateShapeParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-09-15 09:54
 **/
@Data
public class UpdateShapeParams {

    @NotBlank(message = "ociCfgId????")
    private String ociCfgId;
    @NotBlank(message = "instanceId????")
    private String instanceId;
    @NotBlank(message = "shape???????VM.Standard.A1.Flex")
    private String shape;
}
