package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.Valid;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: CreateInstanceBatchParams
 * @author: Tony Wang
 * @date: 2024/11/16 0:04
 */
@Data
public class CreateInstanceBatchParams {

    @NotEmpty(message = "????id??????")
    private List<String> userIds;
    @Valid
    private InstanceInfo instanceInfo;

    @Data
    public static class InstanceInfo {
        @NotBlank(message = "CPU????")
        private String ocpus;
        @NotBlank(message = "??????")
        private String memory;
        @NotNull(message = "????????")
        private Integer disk;
        @NotBlank(message = "????????")
        private String architecture;
        @NotNull(message = "????????")
        private Integer interval;
        @NotNull(message = "????????")
        private Integer createNumbers;
        @NotBlank(message = "????????")
        private String operationSystem;
        @NotBlank(message = "root??????")
        private String rootPassword;
    }
}
