package com.tony.kingdetective.controller;

import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.params.sys.*;
import com.tony.kingdetective.bean.response.sys.GetGlanceRsp;
import com.tony.kingdetective.bean.response.sys.GetSysCfgRsp;
import com.tony.kingdetective.bean.response.sys.LoginRsp;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.IIpBlacklistService;
import com.tony.kingdetective.service.ILoginAttemptService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.controller
 * @className: SysCfgController
 * @author: Tony Wang
 * @date: 2024/11/30 17:07
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/sys")
public class SysCfgController {

    @Resource
    private ISysService sysService;
    
    @Resource
    private ILoginAttemptService loginAttemptService;
    
    @Resource
    private IIpBlacklistService blacklistService;
    
    @Resource
    private HttpServletRequest request;

    @PostMapping(path = "/login")
    public ResponseData<LoginRsp> addCfg(@Validated @RequestBody LoginParams params) {
        String clientIp = getClientIp(request);
        
        try {
            // Clean expired login attempts
            loginAttemptService.cleanExpiredAttempts();
            
            LoginRsp result = sysService.login(params);
            
            // Login success - clear attempts
            loginAttemptService.clearAttempts(clientIp);
            
            return ResponseData.successData(result, "????");
        } catch (OciException e) {
            // Login failed - record attempt
            loginAttemptService.recordFailure(clientIp);
            int attemptCount = loginAttemptService.getAttemptCount(clientIp);
            
            // Auto-ban after 5 failures (no notification)
            if (attemptCount >= 5) {
                blacklistService.addToBlacklist(clientIp, "Login failed 5 times", "AUTO");
                log.warn("IP {} automatically blacklisted after 5 failed login attempts", clientIp);
                // Do NOT send notification
            }
            
            throw e;
        }
    }
    
    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping(path = "/updateVersion")
    public ResponseData<Void> updateVersion() {
        sysService.updateVersion();
        return ResponseData.successData("????????????????????~");
    }

    @PostMapping(path = "/getEnableMfa")
    public ResponseData<Boolean> getEnableMfa() {
        return ResponseData.successData(sysService.getEnableMfa(), "????????MFA??");
    }

    @PostMapping(path = "/getSysCfg")
    public ResponseData<GetSysCfgRsp> getSysCfg() {
        return ResponseData.successData(sysService.getSysCfg(), "????????");
    }

    @PostMapping(path = "/updateSysCfg")
    public ResponseData<Void> updateSysCfg(@Validated @RequestBody UpdateSysCfgParams params) {
        sysService.updateSysCfg(params);
        return ResponseData.successData("????????");
    }

    @PostMapping(path = "/sendMsg")
    public ResponseData<Void> sendMsg(@Validated @RequestBody SendMsgParams params) {
        sysService.sendMessage(params.getMessage());
        return ResponseData.successData("??????");
    }

    @PostMapping(path = "/checkMfaCode")
    public ResponseData<Void> checkMfaCode(@Validated @RequestBody CheckMfaCodeParams params) {
        sysService.checkMfaCode(params.getMfaCode());
        return ResponseData.successData("MFA????");
    }

    @PostMapping(path = "/backup")
    public void backup(@Validated @RequestBody BackupParams params) {
        sysService.backup(params);
    }

    @PostMapping(path = "/recover")
    public ResponseData<Void> recover(@Validated RecoverParams params) {
        sysService.recover(params);
        return ResponseData.successData("??????");
    }

    @GetMapping(path = "/glance")
    public ResponseData<GetGlanceRsp> glance() {
        return ResponseData.successData(sysService.glance(), "?????????");
    }

    @PostMapping(path = "/googleLogin")
    public ResponseData<LoginRsp> googleLogin(@Validated @RequestBody GoogleLoginParams params) {
        return ResponseData.successData(sysService.googleLogin(params), "Google????");
    }

    @PostMapping(path = "/getGoogleClientId")
    public ResponseData<String> getGoogleClientId() {
        return ResponseData.successData(sysService.getGoogleClientId(), "??Google Client ID??");
    }

    @GetMapping(path = "/getAlertEmail")
    public ResponseData<String> getAlertEmail() {
        return ResponseData.successData(sysService.getAlertEmail(), "????????");
    }

    @PostMapping(path = "/updateAlertEmail")
    public ResponseData<Void> updateAlertEmail(@RequestParam("email") String email) {
        sysService.updateAlertEmail(email);
        return ResponseData.successData("????????");
    }
}
