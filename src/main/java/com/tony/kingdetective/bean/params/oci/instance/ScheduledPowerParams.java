package com.tony.kingdetective.bean.params.oci.instance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScheduledPowerParams {
    @NotBlank(message = "ociCfgId????")
    private String ociCfgId;

    @NotBlank(message = "instanceId????")
    private String instanceId;

    private String stopTime; // Optional (e.g., "01:00"). If blank, clears schedule
    private String startTime; // Optional (e.g., "08:00")
}
