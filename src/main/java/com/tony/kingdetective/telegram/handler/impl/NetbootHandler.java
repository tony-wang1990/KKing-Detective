package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.model.UpdateInstanceDetails;
import com.oracle.bmc.core.requests.InstanceActionRequest;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🚑 netboot.xyz 救砖 Handler
 * 功能：将实例的 iPXE 启动脚本替换为 netboot.xyz，然后重启实例进入网络引导界面
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class NetbootHandler extends AbstractCallbackHandler {

    private static final String IPXE_SCRIPT = "#!ipxe\ndhcp\nchain --autofree https://boot.netboot.xyz";

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.startsWith("netboot_xyz_confirm:") ||
            callbackData.startsWith("netboot_xyz_execute:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.startsWith("netboot_xyz_confirm:")) {
            return showConfirm(callbackQuery, data.substring("netboot_xyz_confirm:".length()));
        } else if (data.startsWith("netboot_xyz_execute:")) {
            return executeNetboot(callbackQuery, data.substring("netboot_xyz_execute:".length()), telegramClient);
        }
        return buildEditMessage(callbackQuery, "❌ 未知操作");
    }

    private BotApiMethod<? extends Serializable> showConfirm(CallbackQuery callbackQuery, String params) {
        return buildEditMessage(callbackQuery,
            "🚑 *netboot.xyz 救砖模式*\n\n" +
            "⚠️ 注意：\n" +
            "1. 实例的 iPXE 脚本将被修改为 `netboot.xyz` 引导代码。\n" +
            "2. 实例将**立刻重启**并进入网络引导模式。\n" +
            "3. 重启后需要使用 VNC 控制台直接操作！\n\n" +
            "确认执行吗？",
            KeyboardBuilder.buildConfirmationKeyboard(
                "netboot_xyz_execute:" + params,
                "cancel"
            )
        );
    }

    private BotApiMethod<? extends Serializable> executeNetboot(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        if (parts.length < 2) return buildEditMessage(callbackQuery, "❌ 参数错误");
        
        String instanceId = parts[0];
        String userId = parts[1];
        long chatId = callbackQuery.getMessage().getChatId();

        buildEditMessage(callbackQuery, "⏳ 正在设置 iPXE 脚本并重启实例，请稍候...", null);

        CompletableFuture.runAsync(() -> doNetboot(chatId, instanceId, userId, telegramClient));
        return null;
    }

    private void doNetboot(long chatId, String instanceId, String userId, TelegramClient telegramClient) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sendMarkdownMessage(chatId, "❌ 账户不存在", telegramClient);
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
                
                // 1. 设置 iPXE 脚本
                fetcher.getComputeClient().updateInstance(
                    UpdateInstanceRequest.builder()
                        .instanceId(instanceId)
                        .updateInstanceDetails(
                            UpdateInstanceDetails.builder()
                                .ipxeScript(IPXE_SCRIPT)
                                .build()
                        )
                        .build()
                );

                // 2. 软重启实例
                fetcher.getComputeClient().instanceAction(
                    InstanceActionRequest.builder()
                        .instanceId(instanceId)
                        .action("SOFTRESET")
                        .build()
                );

                String msg = "✅ *netboot.xyz 救砖模式已启动*\n\n" +
                             "iPXE 引导脚本设置成功，实例正在重启。\n\n" +
                             "📺 **下一步操作：**\n" +
                             "请使用在【主菜单】->【VNC配置】中配置的 VNC 工具，或前往 OCI 控制台，连接实例的 **Console Connection (VNC)** 进行后续系统重装操作。";
                
                sendMarkdownMessage(chatId, msg, telegramClient);
            }
        } catch (Exception e) {
            log.error("Failed to execute netboot", e);
            sendMarkdownMessage(chatId, "❌ 开启 netboot.xyz 失败：" + e.getMessage(), telegramClient);
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
            log.error("Failed to send msg", e);
        }
    }
}
