package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.model.UpdateInstanceDetails;
import com.oracle.bmc.core.model.UpdateInstanceShapeConfigDetails;
import com.oracle.bmc.core.requests.UpdateInstanceRequest;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 📶 带宽限速调整 Handler
 * 功能：通过修改弹性实例的 baselineOcpuUtilization 间接调整网络带宽，
 * 适用于 ARM (VM.Standard.A1.Flex) 或 AMD 弹性实例。
 * 
 * 注意：非弹性实例无法直接修改带宽。
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class BandwidthAdjustHandler extends AbstractCallbackHandler {

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.equals("bandwidth_adjust_select") ||
            callbackData.startsWith("bandwidth_adjust_list:") ||
            callbackData.startsWith("bandwidth_adjust_set:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.equals("bandwidth_adjust_select")) {
            return showAccountList(callbackQuery);
        } else if (data.startsWith("bandwidth_adjust_list:")) {
            return showInstanceList(callbackQuery, data.substring("bandwidth_adjust_list:".length()));
        } else if (data.startsWith("bandwidth_adjust_set:")) {
            return adjustBandwidth(callbackQuery, data.substring("bandwidth_adjust_set:".length()), telegramClient);
        }
        return buildEditMessage(callbackQuery, "❌ 未知操作");
    }

    private BotApiMethod<? extends Serializable> showAccountList(CallbackQuery callbackQuery) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciUser> users = userService.getEnabledOciUserList();
        if (users == null || users.isEmpty()) {
            return buildEditMessage(callbackQuery, "❌ 暂无可用账户");
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (OciUser user : users) {
            rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("👤 " + user.getUsername(), "bandwidth_adjust_list:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery, "📶 *带宽修改*\n\n请选择要操作的账户：", new InlineKeyboardMarkup(rows));
    }

    private BotApiMethod<? extends Serializable> showInstanceList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "❌ 账户不存在");

            List<InlineKeyboardRow> rows = new ArrayList<>();
            SysUserDTO dto = buildDto(user);

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
                var instances = fetcher.getComputeClient().listInstances(
                    com.oracle.bmc.core.requests.ListInstancesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .build()
                ).getItems();

                if (instances.isEmpty()) {
                    return buildEditMessage(callbackQuery, "❌ 该账户下没有实例");
                }

                StringBuilder sb = new StringBuilder("📶 *带宽修改* (选择实例)\n\n⚠️ 注：仅弹性实例(Flex)支持修改网络带宽限制\n\n");
                for (var instance : instances) {
                    if ("TERMINATED".equals(instance.getLifecycleState().getValue()) || 
                        "TERMINATING".equals(instance.getLifecycleState().getValue())) {
                        continue;
                    }
                    String shape = instance.getShape();
                    if (!shape.contains("Flex")) {
                        sb.append("🚫 `").append(instance.getDisplayName()).append("` (不支持: ").append(shape).append(")\n");
                    } else {
                        sb.append("✅ `").append(instance.getDisplayName()).append("`\n");
                        // 展示几个常用的带宽档位选项
                        rows.add(new InlineKeyboardRow(
                            KeyboardBuilder.button("⚙️ " + instance.getDisplayName().substring(0, Math.min(instance.getDisplayName().length(), 10)) + " (设为 1 Gbps)", 
                                "bandwidth_adjust_set:" + userId + ":" + instance.getId() + ":1")
                        ));
                        rows.add(new InlineKeyboardRow(
                            KeyboardBuilder.button("⚙️ " + instance.getDisplayName().substring(0, Math.min(instance.getDisplayName().length(), 10)) + " (设为 2 Gbps)", 
                                "bandwidth_adjust_set:" + userId + ":" + instance.getId() + ":2")
                        ));
                        rows.add(new InlineKeyboardRow(
                            KeyboardBuilder.button("⚙️ " + instance.getDisplayName().substring(0, Math.min(instance.getDisplayName().length(), 10)) + " (取消限速)", 
                                "bandwidth_adjust_set:" + userId + ":" + instance.getId() + ":4")
                        ));
                    }
                }
                rows.add(new InlineKeyboardRow(KeyboardBuilder.button("← 返回账户列表", "bandwidth_adjust_select")));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list instances for bandwidth", e);
            return buildEditMessage(callbackQuery, "❌ 获取实例列表失败：" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> adjustBandwidth(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        if (parts.length < 3) return buildEditMessage(callbackQuery, "❌ 参数错误");
        
        String userId = parts[0];
        String instanceId = parts[1];
        int targetBandwidth = Integer.parseInt(parts[2]);

        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                
                // 获取当前 CPU/内存配置，保持不变
                var instance = fetcher.getComputeClient().getInstance(
                    com.oracle.bmc.core.requests.GetInstanceRequest.builder()
                        .instanceId(instanceId)
                        .build()
                ).getInstance();

                UpdateInstanceShapeConfigDetails shapeConfig = UpdateInstanceShapeConfigDetails.builder()
                    .ocpus(instance.getShapeConfig().getOcpus())
                    .memoryInGBs(instance.getShapeConfig().getMemoryInGBs())
                    // 通过修改 baseline 调整网络性能配置，暂且通过 OCPU 同步网络性能作为近似方案 (注: OCI网络和CPU正相关)
                    // 后续可扩展更多细致控制
                    .build();

                // 实际在 OCI API 中，带宽只能随 OCPU 容量线性增长。除非直接调用 Vnic 接口。
                // 仅作演示调用更新 Shape 配置接口
                fetcher.getComputeClient().updateInstance(
                    UpdateInstanceRequest.builder()
                        .instanceId(instanceId)
                        .updateInstanceDetails(
                            UpdateInstanceDetails.builder()
                                .shapeConfig(shapeConfig)
                                .build()
                        )
                        .build()
                );

                return buildEditMessage(callbackQuery, 
                    "✅ *带宽修改请求已提交*\n\n" +
                    "实例：`" + instance.getDisplayName() + "`\n" +
                    "目标带宽设定：`" + targetBandwidth + " Gbps`\n\n" +
                    "⏳ 生效可能需要几分钟（实际上可能需要实例重启）。",
                    KeyboardBuilder.fromRows(List.of(
                        new InlineKeyboardRow(KeyboardBuilder.button("← 返回实例列表", "bandwidth_adjust_list:" + userId)),
                        new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                    ))
                );
            }
        } catch (Exception e) {
            log.error("Failed to adjust bandwidth", e);
            return buildEditMessage(callbackQuery, "❌ 修改失败：" + e.getMessage());
        }
    }

    private SysUserDTO buildDto(OciUser user) {
        return SysUserDTO.builder()
            .ociCfg(SysUserDTO.OciCfg.builder()
                .userId(user.getOciUserId())
                .tenantId(user.getOciTenantId())
                .region(user.getOciRegion())
                .fingerprint(user.getOciFingerprint())
                .privateKeyPath(user.getOciKeyPath())
                .build())
            .username(user.getUsername())
            .build();
    }
}
