package com.tony.kingdetective.bean.params.cf;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @ClassName OciRemoveCfDnsRecordsParams
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-03-21 14:53
 **/
@Data
public class OciRemoveCfDnsRecordsParams {

    private String cfCfgId;
    @NotEmpty(message = "??ID????")
    private List<String> recordIds;
}
