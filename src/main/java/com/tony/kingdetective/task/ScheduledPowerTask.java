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
 * ⏰ 定时开关机后台任务
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class ScheduledPowerTask {

    private static final String KV_KEY_PREFIX = "scheduled_power:";
    private static final Pattern CONFIG_PATTERN = Pattern.compile("STOP=(.*?):00,START=(.*?):00,USER=(.*)");

    /**
     * 每小时执行一次，检查是否有实例需要开关机
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void executeSchedule() {
        log.info("⏰ 开始执行定时开关机检查...");
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);

        // 获取当前时间（小时），假设服务器运行在 UTC+8
        // 为确保时区一致，建议使用 Server 本地时区判断
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
                    log.info("🎯 匹配到定时任务: 实例[{}], 目标动作[{}]", instanceId, action);
                    executeInstanceAction(userId, instanceId, action, userService);
                }
            } catch (Exception e) {
                log.error("💥 执行实例的开关机任务失败：[{}]", config.getCode(), e);
            }
        }
        log.info("⏰ 定时开关机检查完毕.");
    }

    private void executeInstanceAction(String userId, String instanceId, String action, IOciUserService userService) {
        OciUser user = userService.getById(userId);
        if (user == null || user.getOciUserStatus() == 0) {
            log.warn("用户为空或状态已禁用，跳过开关机动作");
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
            log.info("✅ 成功发起实例定时动作：[{}] -> [{}]", instanceId, action);
        } catch (Exception e) {
            log.error("❌ 调用 OCI 实例开关机接口异常", e);
        }
    }
}
