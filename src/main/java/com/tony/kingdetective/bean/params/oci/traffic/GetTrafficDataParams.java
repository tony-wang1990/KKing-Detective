package com.tony.kingdetective.bean.params.oci.traffic;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params.oci.traffic
 * @className: GetTrafficDataParams
 * @author: Tony Wang
 * @date: 2025/3/7 20:37
 */
@Data
public class GetTrafficDataParams {

    @NotBlank(message = "??ID????")
    private String ociCfgId;
    @NotNull(message = "????????")
    private Date beginTime;
    @NotNull(message = "????????")
    private Date endTime;
    @NotBlank(message = "??????")
    private String region;
    @NotBlank(message = "inQuery????")
    private String inQuery;
    @NotBlank(message = "outQuery????")
    private String outQuery;
    @NotBlank(message = "namespace????")
    private String namespace;
}
