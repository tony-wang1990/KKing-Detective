package com.tony.kingdetective.bean.params.oci.cfg;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * AddCfgParams
 * </p >
 *
 * @author Tony Wang
 * @since 2024/11/13 14:30
 */
@Data
public class AddCfgParams {

    @NotBlank(message = "????????")
    private String username;

    @NotBlank(message = "??????")
    private String ociCfgStr;

    @NotNull(message = "??????")
    private MultipartFile file;
}
