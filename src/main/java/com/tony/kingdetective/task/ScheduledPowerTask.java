package com.tony.kingdetective.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oracle.bmc.core.requests.InstanceActionRequest;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.service.IOciUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class ScheduledPowerTask {

    private static final String KV_KEY_PREFIX = "scheduled_power:";
    private static final Pattern CONFIG_PATTERN = Pattern.compile("STOP=(.*?):00,START=(.*?):00,USER=(.*)");

    /**
     * 
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void executeSchedule() {
        log.info("? ???????????...");
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);

        //  UTC+8
        //  Server 
        String currentHourStr = DateUtil.format(DateUtil.date(), "HH");

        List<OciKv> configs = kvService.list(new LambdaQueryWrapper<OciKv>().likeRight(OciKv::getCode, KV_KEY_PREFIX));
        if (configs == null || configs.isEmpty()) {
            return;
        }

        for (OciKv config : configs) {
            try {
                String instanceId = config.getCode().substring(KV_KEY_PREFIX.length());
                String value = config.getValue();
                
                Matcher matcher = CONFIG_PATTERN.matcher(value);
                if (!matcher.matches()) continue;

                String stopHour = matcher.group(1);
                String startHour = matcher.group(2);
                String userId = matcher.group(3);

                String action = null;
                if (currentHourStr.equals(stopHour)) {
                    action = "STOP";
                } else if (currentHourStr.equals(startHour)) {
                    action = "START";
                }

                if (action != null) {
                    log.info("? ???????: ??[{}], ????[{}]", instanceId, action);
                    executeInstanceAction(userId, instanceId, action, userService);
                }
            } catch (Exception e) {
                log.error("? ?????????????[{}]", config.getCode(), e);
            }
        }
        log.info("? ?????????.");
    }

    private void executeInstanceAction(String userId, String instanceId, String action, IOciUserService userService) {
        OciUser user = userService.getById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            log.warn("??????????????????");
            return;
        }

        SysUserDTO dto = SysUserDTO.builder()
            .ociCfg(SysUserDTO.OciCfg.builder()
                .userId(user.getOciUserId())
                .tenantId(user.getOciTenantId())
                .region(user.getOciRegion())
                .fingerprint(user.getOciFingerprint())
                .privateKeyPath(user.getOciKeyPath())
                .build())
            .username(user.getUsername())
            .build();

        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
            fetcher.getComputeClient().instanceAction(
                InstanceActionRequest.builder()
                    .instanceId(instanceId)
                    .action(action)
                    .build()
            );
            log.info("? ???????????[{}] -> [{}]", instanceId, action);
        } catch (Exception e) {
            log.error("? ?? OCI ?????????", e);
        }
    }
}
