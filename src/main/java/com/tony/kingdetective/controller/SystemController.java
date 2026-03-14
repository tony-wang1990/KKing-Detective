package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.controller
 * @className: SystemController
 * @author: Tony Wang
 * @date: 2026/01/04
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    /**
     * 
     * update_version_trigger.flagwatcher
     */
    @PostMapping("/trigger-update")
    public ResponseData<String> triggerUpdate() {
        try {
            File flagFile = new File("/app/king-detective/update_version_trigger.flag");
            String timestamp = String.valueOf(System.currentTimeMillis());
            Files.write(flagFile.toPath(), timestamp.getBytes());
            log.info("???????timestamp: {}", timestamp);
            return ResponseData.successData("??????????????????????");
        } catch (Exception e) {
            log.error("??????", e);
            return ResponseData.errorData("??????: " + e.getMessage());
        }
    }

    /**
     * 
     */
    @PostMapping("/version")
    public ResponseData<String> getVersion() {
        // 
        String version = System.getenv("APP_VERSION");
        if (version == null || version.isEmpty()) {
            version = "v2.0.0";
        }
        return ResponseData.successData(version);
    }
}
