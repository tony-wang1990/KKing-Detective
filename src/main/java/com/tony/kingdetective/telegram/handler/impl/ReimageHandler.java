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
 *   (Re-image) Handler
 * 
 *  Telegram  " Boot Volume "
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
        return buildEditMessage(callbackQuery, "? ????");
    }

    private BotApiMethod<? extends Serializable> showAccountList(CallbackQuery callbackQuery) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciUser> users = userService.getEnabledOciUserList();
        if (users == null || users.isEmpty()) {
            return buildEditMessage(callbackQuery, "? ??????");
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (OciUser user : users) {
            rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("? " + user.getUsername(), "reimage_list:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery, "? *???? (Re-image)*\n\n??????????", new InlineKeyboardMarkup(rows));
    }

    private BotApiMethod<? extends Serializable> showInstanceList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "? ?????");

            List<InlineKeyboardRow> rows = new ArrayList<>();
            SysUserDTO dto = buildDto(user);

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
                var instances = fetcher.getComputeClient().listInstances(
                    com.oracle.bmc.core.requests.ListInstancesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .build()
                ).getItems();

                if (instances.isEmpty()) {
                    return buildEditMessage(callbackQuery, "? ????????");
                }

                StringBuilder sb = new StringBuilder("? *????????*\n\n");
                for (var instance : instances) {
                    if ("TERMINATED".equals(instance.getLifecycleState().getValue()) || 
                        "TERMINATING".equals(instance.getLifecycleState().getValue())) {
                        continue;
                    }
                    rows.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("? " + instance.getDisplayName(), "reimage_confirm:" + userId + ":" + instance.getId())
                    ));
                }
                rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "reimage_select")));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list instances for reimage", e);
            return buildEditMessage(callbackQuery, "? ?????????" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> showConfirm(CallbackQuery callbackQuery, String params) {
        String[] parts = params.split(":");
        String userId = parts[0];
        String instanceId = parts[1];

        return buildEditMessage(callbackQuery,
            "?? *???????!*\n\n" +
            "?????\n" +
            "1. ???????**?????**?\n" +
            "2. ?????????????????????\n\n" +
            "??**??**??????????? IP ??????????\n" +
            "\n??????",
            KeyboardBuilder.buildConfirmationKeyboard(
                "reimage_execute:" + params,
                "reimage_list:" + userId
            )
        );
    }

    private BotApiMethod<? extends Serializable> executeReimage(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        if (parts.length < 2) return buildEditMessage(callbackQuery, "? ????");
        
        String userId = parts[0];
        String instanceId = parts[1];
        long chatId = callbackQuery.getMessage().getChatId();

        try { telegramClient.execute(buildEditMessage(callbackQuery, "? ?????????????????...", null)); } catch (Exception ignore) {}

        CompletableFuture.runAsync(() -> doReimage(chatId, userId, instanceId, telegramClient));
        return null;
    }

    private void doReimage(long chatId, String userId, String instanceId, TelegramClient telegramClient) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sendMarkdownMessage(chatId, "? ?????", telegramClient);
                return;
            }

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                
                // 1. 
                Instance oldInstance = fetcher.getComputeClient().getInstance(
                    GetInstanceRequest.builder().instanceId(instanceId).build()
                ).getInstance();

                //  VNIC 
                var vnicAttachments = fetcher.getComputeClient().listVnicAttachments(
                    com.oracle.bmc.core.requests.ListVnicAttachmentsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instanceId)
                        .build()
                ).getItems();
                String subnetId = vnicAttachments.isEmpty() ? null : vnicAttachments.get(0).getSubnetId();

                if (subnetId == null) {
                    sendMarkdownMessage(chatId, "? ?????????", telegramClient);
                    return;
                }

                // 1.5  BootVolume 
                var bootVolumeAttachments = fetcher.getComputeClient().listBootVolumeAttachments(
                    com.oracle.bmc.core.requests.ListBootVolumeAttachmentsRequest.builder()
                        .availabilityDomain(oldInstance.getAvailabilityDomain())
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instanceId)
                        .build()
                ).getItems();
                String bootVolumeId = bootVolumeAttachments.isEmpty() ? null : bootVolumeAttachments.get(0).getBootVolumeId();

                if (bootVolumeId == null) {
                    sendMarkdownMessage(chatId, "? ????????????", telegramClient);
                    return;
                }

                // 2.  Boot Volume
                fetcher.getComputeClient().terminateInstance(
                    TerminateInstanceRequest.builder()
                        .instanceId(instanceId)
                        .preserveBootVolume(true)
                        .build()
                );

                sendMarkdownMessage(chatId, "? ?? `" + oldInstance.getDisplayName() + "` ????????????... (???? 1-3 ??)", telegramClient);

                //  ()
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
                        //  404 
                        terminated = true;
                        break;
                    }
                }

                if (!terminated) {
                    sendMarkdownMessage(chatId, "? ????????????????", telegramClient);
                    return;
                }

                // 3.  BootVolume 
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

                // 
                fetcher.getComputeClient().launchInstance(
                    LaunchInstanceRequest.builder()
                        .launchInstanceDetails(launchDetails)
                        .build()
                );
                
                sendMarkdownMessage(chatId, "? ??????????????????????????", telegramClient);
            }
        } catch (Exception e) {
            log.error("Failed to reimage instance", e);
            sendMarkdownMessage(chatId, "? ???????" + e.getMessage(), telegramClient);
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
