package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.model.UpdateInstanceDetails;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.model.CreateVnicDetails;
import com.oracle.bmc.core.model.LaunchInstanceDetails;
import com.oracle.bmc.core.requests.LaunchInstanceRequest;
import com.oracle.bmc.core.requests.TerminateInstanceRequest;
import com.oracle.bmc.core.requests.GetInstanceRequest;
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
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 重建实例 (Re-image) Handler
 * 功能：删除当前实例（保留启动卷可选），并用相同的配置（基于原启动卷或新镜像）原位拉起新实例
 * 注：受限于 Telegram 交互的复杂性，此处实现最常用的 "带 Boot Volume 终止并原地重建"
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class ReimageHandler extends AbstractCallbackHandler {

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.equals("reimage_select") ||
            callbackData.startsWith("reimage_list:") ||
            callbackData.startsWith("reimage_confirm:") ||
            callbackData.startsWith("reimage_execute:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.equals("reimage_select")) {
            return showAccountList(callbackQuery);
        } else if (data.startsWith("reimage_list:")) {
            return showInstanceList(callbackQuery, data.substring("reimage_list:".length()));
        } else if (data.startsWith("reimage_confirm:")) {
            return showConfirm(callbackQuery, data.substring("reimage_confirm:".length()));
        } else if (data.startsWith("reimage_execute:")) {
            return executeReimage(callbackQuery, data.substring("reimage_execute:".length()), telegramClient);
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
                KeyboardBuilder.button("👤 " + user.getUsername(), "reimage_list:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery, "🔄 *重建实例 (Re-image)*\n\n请选择要操作的账户：", new InlineKeyboardMarkup(rows));
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

                StringBuilder sb = new StringBuilder("🔄 *选择要重建的实例*\n\n");
                for (var instance : instances) {
                    if ("TERMINATED".equals(instance.getLifecycleState().getValue()) || 
                        "TERMINATING".equals(instance.getLifecycleState().getValue())) {
                        continue;
                    }
                    rows.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("🔄 " + instance.getDisplayName(), "reimage_confirm:" + userId + ":" + instance.getId())
                    ));
                }
                rows.add(new InlineKeyboardRow(KeyboardBuilder.button("← 返回账户列表", "reimage_select")));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list instances for reimage", e);
            return buildEditMessage(callbackQuery, "❌ 获取实例列表失败：" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> showConfirm(CallbackQuery callbackQuery, String params) {
        String[] parts = params.split(":");
        String userId = parts[0];
        String instanceId = parts[1];

        return buildEditMessage(callbackQuery,
            "⚠️ *即将重建该实例!*\n\n" +
            "操作流程：\n" +
            "1. 终止当前实例（**保留启动卷**）\n" +
            "2. 使用原启动卷、原网络配置重新拉起一个新实例\n\n" +
            "❗️**注意**：此操作会导致实例公网 IP 变更，短暂中断服务。\n" +
            "\n确认继续吗？",
            KeyboardBuilder.buildConfirmationKeyboard(
                "reimage_execute:" + params,
                "reimage_list:" + userId
            )
        );
    }

    private BotApiMethod<? extends Serializable> executeReimage(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        if (parts.length < 2) return buildEditMessage(callbackQuery, "❌ 参数错误");
        
        String userId = parts[0];
        String instanceId = parts[1];
        long chatId = callbackQuery.getMessage().getChatId();

        try { telegramClient.execute(buildEditMessage(callbackQuery, "⏳ 正在提交重建任务，时间较长，请稍候...", null)); } catch (Exception ignore) {}

        CompletableFuture.runAsync(() -> doReimage(chatId, userId, instanceId, telegramClient));
        return null;
    }

    private void doReimage(long chatId, String userId, String instanceId, TelegramClient telegramClient) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sendMarkdownMessage(chatId, "❌ 账户不存在", telegramClient);
                return;
            }

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                
                // 1. 获取原实例详细信息
                Instance oldInstance = fetcher.getComputeClient().getInstance(
                    GetInstanceRequest.builder().instanceId(instanceId).build()
                ).getInstance();

                // 获取 VNIC 信息以备份子网
                var vnicAttachments = fetcher.getComputeClient().listVnicAttachments(
                    com.oracle.bmc.core.requests.ListVnicAttachmentsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instanceId)
                        .build()
                ).getItems();
                String subnetId = vnicAttachments.isEmpty() ? null : vnicAttachments.get(0).getSubnetId();

                if (subnetId == null) {
                    sendMarkdownMessage(chatId, "❌ 无法获取原子网信息", telegramClient);
                    return;
                }

                // 1.5 获取 BootVolume 信息
                var bootVolumeAttachments = fetcher.getComputeClient().listBootVolumeAttachments(
                    com.oracle.bmc.core.requests.ListBootVolumeAttachmentsRequest.builder()
                        .availabilityDomain(oldInstance.getAvailabilityDomain())
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instanceId)
                        .build()
                ).getItems();
                String bootVolumeId = bootVolumeAttachments.isEmpty() ? null : bootVolumeAttachments.get(0).getBootVolumeId();

                if (bootVolumeId == null) {
                    sendMarkdownMessage(chatId, "❌ 无法获取原实例启动卷信息", telegramClient);
                    return;
                }

                // 2. 终止实例，但保留 Boot Volume
                fetcher.getComputeClient().terminateInstance(
                    TerminateInstanceRequest.builder()
                        .instanceId(instanceId)
                        .preserveBootVolume(true)
                        .build()
                );

                sendMarkdownMessage(chatId, "⏳ 实例 `" + oldInstance.getDisplayName() + "` 正在终止中，保留了启动卷... (大概需要 1-3 分钟)", telegramClient);

                // 等待实例完全终止 (简易轮询)
                boolean terminated = false;
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(10000);
                    try {
                        Instance state = fetcher.getComputeClient().getInstance(
                            GetInstanceRequest.builder().instanceId(instanceId).build()
                        ).getInstance();
                        if ("TERMINATED".equals(state.getLifecycleState().getValue())) {
                            terminated = true;
                            break;
                        }
                    } catch (Exception e) {
                        // 如果抛出 404 等，通常也表示被彻底删除了
                        terminated = true;
                        break;
                    }
                }

                if (!terminated) {
                    sendMarkdownMessage(chatId, "❌ 终止实例超时，请稍后手动重新拉起", telegramClient);
                    return;
                }

                // 3. 用旧配置和旧 BootVolume 拉起新实例
                LaunchInstanceDetails launchDetails = LaunchInstanceDetails.builder()
                    .compartmentId(fetcher.getCompartmentId())
                    .availabilityDomain(oldInstance.getAvailabilityDomain())
                    .shape(oldInstance.getShape())
                    .shapeConfig(oldInstance.getShapeConfig())
                    .displayName(oldInstance.getDisplayName() + "-Reimaged")
                    .sourceDetails(
                        com.oracle.bmc.core.model.InstanceSourceViaBootVolumeDetails.builder()
                            .bootVolumeId(bootVolumeId)
                            .build()
                    )
                    .createVnicDetails(
                        CreateVnicDetails.builder()
                            .subnetId(subnetId)
                            .assignPublicIp(true)
                            .build()
                    )
                    .build();

                // 拉起新实例
                fetcher.getComputeClient().launchInstance(
                    LaunchInstanceRequest.builder()
                        .launchInstanceDetails(launchDetails)
                        .build()
                );
                
                sendMarkdownMessage(chatId, "✅ 原实例终止完毕，已提交同配置同硬盘的新实例重建请求！", telegramClient);
            }
        } catch (Exception e) {
            log.error("Failed to reimage instance", e);
            sendMarkdownMessage(chatId, "❌ 重建操作异常：" + e.getMessage(), telegramClient);
        }
    }

    private void sendMarkdownMessage(long chatId, String text, TelegramClient telegramClient) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendMessage message = 
                org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(new InlineKeyboardMarkup(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                )))
                .build();
            telegramClient.execute(message);
        } catch (Exception e) {
            log.error("Failed to send message", e);
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
