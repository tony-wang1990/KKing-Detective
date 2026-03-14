package com.tony.kingdetective.bean.params.oci.instance;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * CreateInstanceParams
 * </p >
 *
 * @author Tony Wang
 * @since 2024/11/13 19:26
 */
@Data
public class CreateInstanceParams {

    @NotBlank(message = "??id????")
    private String userId;
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

    private boolean joinChannelBroadcast = true;

}
