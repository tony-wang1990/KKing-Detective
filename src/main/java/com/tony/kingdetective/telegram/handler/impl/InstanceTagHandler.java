package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.model.UpdateInstanceDetails;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   Handler
 *  UpdateInstance API  freeformTags 
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class InstanceTagHandler extends AbstractCallbackHandler {

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.equals("instance_tag_select") ||
            callbackData.startsWith("instance_tag_list:") ||
            callbackData.startsWith("instance_tag_add:") ||
            callbackData.startsWith("instance_tag_del:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.equals("instance_tag_select")) {
            return showAccountList(callbackQuery);
        } else if (data.startsWith("instance_tag_list:")) {
            return showTagList(callbackQuery, data.substring("instance_tag_list:".length()));
        } else if (data.startsWith("instance_tag_add:")) {
            return startAddTag(callbackQuery, data.substring("instance_tag_add:".length()));
        } else if (data.startsWith("instance_tag_del:")) {
            return deleteTag(callbackQuery, data.substring("instance_tag_del:".length()), telegramClient);
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
                KeyboardBuilder.button("? " + user.getUsername(), "instance_tag_list:" + user.getId() + ":account")
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery, "? *??????*\n\n??????????", new InlineKeyboardMarkup(rows));
    }

    private BotApiMethod<? extends Serializable> showTagList(CallbackQuery callbackQuery, String params) {
        String[] parts = params.split(":");
        String userId = parts[0];
        
        //  instanceId 
        if (parts.length > 1 && parts[1].equals("account")) {
            return showInstanceList(callbackQuery, userId);
        }
        
        String instanceId = parts[1];
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                var instance = fetcher.getComputeClient().getInstance(
                    com.oracle.bmc.core.requests.GetInstanceRequest.builder()
                        .instanceId(instanceId)
                        .build()
                ).getInstance();

                Map<String, String> tags = instance.getFreeformTags();
                StringBuilder sb = new StringBuilder("? *????* (`" + instance.getDisplayName() + "`)\n\n");
                List<InlineKeyboardRow> rows = new ArrayList<>();

                if (tags == null || tags.isEmpty()) {
                    sb.append("????\n");
                } else {
                    for (Map.Entry<String, String> entry : tags.entrySet()) {
                        sb.append("?? `").append(entry.getKey()).append("` = `").append(entry.getValue()).append("`\n");
                        rows.add(new InlineKeyboardRow(
                            KeyboardBuilder.button("?? ?? " + entry.getKey(), 
                                "instance_tag_del:" + userId + ":" + instanceId + ":" + entry.getKey())
                        ));
                    }
                }

                rows.add(0, new InlineKeyboardRow(KeyboardBuilder.button("? ??/????", "instance_tag_add:" + userId + ":" + instanceId)));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "instance_tag_list:" + userId + ":account")));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list tags", e);
            return buildEditMessage(callbackQuery, "? ?????" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> showInstanceList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                var instances = fetcher.getComputeClient().listInstances(
                    com.oracle.bmc.core.requests.ListInstancesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .build()
                ).getItems();

                if (instances.isEmpty()) return buildEditMessage(callbackQuery, "? ???????");

                StringBuilder sb = new StringBuilder("? *?????????*\n\n");
                List<InlineKeyboardRow> rows = new ArrayList<>();
                for (var inst : instances) {
                    if ("TERMINATED".equals(inst.getLifecycleState().getValue()) || "TERMINATING".equals(inst.getLifecycleState().getValue())) continue;
                    
                    int tagCount = inst.getFreeformTags() != null ? inst.getFreeformTags().size() : 0;
                    rows.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("?? " + inst.getDisplayName() + " [" + tagCount + "???]", 
                            "instance_tag_list:" + userId + ":" + inst.getId())
                    ));
                }
                
                rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "instance_tag_select")));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to load instances for tag", e);
            return buildEditMessage(callbackQuery, "? ?????????" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> startAddTag(CallbackQuery callbackQuery, String params) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] parts = params.split(":");
        String userId = parts[0];
        String instanceId = parts[1];

        //  Session 
        com.tony.kingdetective.telegram.storage.ConfigSessionStorage storage = 
            com.tony.kingdetective.telegram.storage.ConfigSessionStorage.getInstance();
        
        var data = new java.util.HashMap<String, Object>();
        data.put("userId", userId);
        data.put("instanceId", instanceId);
        storage.startCustomSession(chatId, com.tony.kingdetective.telegram.storage.ConfigSessionStorage.SessionType.INSTANCE_TAG_INPUT, data);

        return buildEditMessage(callbackQuery,
            "? *??????*\n\n" +
            "????????????? `key=value`\n" +
            "???`Role=Web-Server` ? `Env=Prod`\n\n" +
            "? ?? /cancel ???"
        );
    }

    private BotApiMethod<? extends Serializable> deleteTag(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        String userId = parts[0];
        String instanceId = parts[1];
        String keyToRemove = parts[2];

        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                
                var instance = fetcher.getComputeClient().getInstance(
                    com.oracle.bmc.core.requests.GetInstanceRequest.builder()
                        .instanceId(instanceId)
                        .build()
                ).getInstance();

                Map<String, String> tags = new HashMap<>();
                if (instance.getFreeformTags() != null) {
                    tags.putAll(instance.getFreeformTags());
                }
                tags.remove(keyToRemove);

                fetcher.getComputeClient().updateInstance(
                    UpdateInstanceRequest.builder()
                        .instanceId(instanceId)
                        .updateInstanceDetails(
                            UpdateInstanceDetails.builder()
                                .freeformTags(tags)
                                .build()
                        )
                        .build()
                );

                return buildEditMessage(callbackQuery, 
                    "? *??  `" + keyToRemove + "` ???*",
                    KeyboardBuilder.fromRows(List.of(
                        new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "instance_tag_list:" + userId + ":" + instanceId)),
                        new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                    ))
                );
            }
        } catch (Exception e) {
            log.error("Failed to delete tag", e);
            return buildEditMessage(callbackQuery, "? ?????" + e.getMessage());
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

    @Override
    public String getCallbackPattern() {
        return "instancetag_";
    }
}