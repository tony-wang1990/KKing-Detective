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
 *  netboot.xyz  Handler
 *  iPXE  netboot.xyz
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
        return buildEditMessage(callbackQuery, "? ????");
    }

    private BotApiMethod<? extends Serializable> showConfirm(CallbackQuery callbackQuery, String params) {
        return buildEditMessage(callbackQuery,
            "? *netboot.xyz ????*\n\n" +
            "?? ???\n" +
            "1. ??? iPXE ??????? `netboot.xyz` ?????\n" +
            "2. ???**????**??????????\n" +
            "3. ??????? VNC ????????\n\n" +
            "??????",
            KeyboardBuilder.buildConfirmationKeyboard(
                "netboot_xyz_execute:" + params,
                "cancel"
            )
        );
    }

    private BotApiMethod<? extends Serializable> executeNetboot(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        if (parts.length < 2) return buildEditMessage(callbackQuery, "? ????");
        
        String instanceId = parts[0];
        String userId = parts[1];
        long chatId = callbackQuery.getMessage().getChatId();

        try { telegramClient.execute(buildEditMessage(callbackQuery, "? ???? iPXE ???????????...", null)); } catch (Exception ignore) {}

        CompletableFuture.runAsync(() -> doNetboot(chatId, instanceId, userId, telegramClient));
        return null;
    }

    private void doNetboot(long chatId, String instanceId, String userId, TelegramClient telegramClient) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sendMarkdownMessage(chatId, "? ?????", telegramClient);
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
                
                // 1.  iPXE 
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

                // 2. 
                fetcher.getComputeClient().instanceAction(
                    InstanceActionRequest.builder()
                        .instanceId(instanceId)
                        .action("SOFTRESET")
                        .build()
                );

                String msg = "? *netboot.xyz ???????*\n\n" +
                             "iPXE ????????????????\n\n" +
                             "? **??????**\n" +
                             "?????????->?VNC??????? VNC ?????? OCI ????????? **Console Connection (VNC)** ???????????";
                
                sendMarkdownMessage(chatId, msg, telegramClient);
            }
        } catch (Exception e) {
            log.error("Failed to execute netboot", e);
            sendMarkdownMessage(chatId, "? ?? netboot.xyz ???" + e.getMessage(), telegramClient);
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
