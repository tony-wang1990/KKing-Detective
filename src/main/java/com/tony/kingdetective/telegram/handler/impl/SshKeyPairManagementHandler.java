package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.ApiKey;
import com.oracle.bmc.identity.model.CreateApiKeyDetails;
import com.oracle.bmc.identity.requests.DeleteApiKeyRequest;
import com.oracle.bmc.identity.requests.ListApiKeysRequest;
import com.oracle.bmc.identity.requests.UploadApiKeyRequest;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
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

/**
 *  SSH  Handler
 *  OCI  API Key
 *
 * OCI  API Key  SSH/RSA 5
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class SshKeyPairManagementHandler extends AbstractCallbackHandler {

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.equals("ssh_keypair_management") ||
            callbackData.startsWith("ssh_keypair_list:") ||
            callbackData.startsWith("ssh_keypair_add:") ||
            callbackData.startsWith("ssh_keypair_del_confirm:") ||
            callbackData.startsWith("ssh_keypair_del:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.equals("ssh_keypair_management")) {
            return showAccountList(callbackQuery);
        } else if (data.startsWith("ssh_keypair_list:")) {
            return showKeyList(callbackQuery, data.substring("ssh_keypair_list:".length()));
        } else if (data.startsWith("ssh_keypair_add:")) {
            return startAddKey(callbackQuery, data.substring("ssh_keypair_add:".length()));
        } else if (data.startsWith("ssh_keypair_del_confirm:")) {
            return confirmDeleteKey(callbackQuery, data.substring("ssh_keypair_del_confirm:".length()));
        } else if (data.startsWith("ssh_keypair_del:")) {
            return deleteKey(callbackQuery, data.substring("ssh_keypair_del:".length()));
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
                KeyboardBuilder.button("? " + user.getUsername(), "ssh_keypair_list:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery,
            "? *SSH ?????*\n\n???????????\n\n" +
            "? OCI API Key ?? SDK ???????? RSA ????????5?",
            new InlineKeyboardMarkup(rows)
        );
    }

    private BotApiMethod<? extends Serializable> showKeyList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "? ?????");

            SysUserDTO dto = buildDto(user);
            List<InlineKeyboardRow> rows = new ArrayList<>();

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
                IdentityClient identityClient = fetcher.getIdentityClient();
                List<ApiKey> keys = identityClient.listApiKeys(
                    ListApiKeysRequest.builder()
                        .userId(user.getOciUserId())
                        .build()
                ).getItems();

                StringBuilder sb = new StringBuilder("? *????*????" + user.getUsername() + "?\n\n");
                if (keys.isEmpty()) {
                    sb.append("????");
                } else {
                    int i = 1;
                    for (ApiKey key : keys) {
                        String fp = key.getFingerprint();
                        String state = key.getLifecycleState() != null ? key.getLifecycleState().getValue() : "UNKNOWN";
                        sb.append(i++).append(". `").append(fp).append("`\n")
                          .append("   ??: ").append("ACTIVE".equals(state) ? "? ??" : "?? " + state).append("\n");
                        rows.add(new InlineKeyboardRow(
                            KeyboardBuilder.button("?? ?? " + fp.substring(0, Math.min(fp.length(), 20)) + "...",
                                "ssh_keypair_del_confirm:" + userId + ":" + fp)
                        ));
                    }
                    sb.append("\n? ").append(keys.size()).append("/5 ???");
                }

                rows.add(0, new InlineKeyboardRow(
                    KeyboardBuilder.button("? ?????", "ssh_keypair_add:" + userId)
                ));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list API keys", e);
            return buildEditMessage(callbackQuery, "? ?????????" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> startAddKey(CallbackQuery callbackQuery, String userId) {
        long chatId = callbackQuery.getMessage().getChatId();
        //  Session 
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        var data = new HashMap<String, Object>();
        data.put("userId", userId);
        data.put("action", "ssh_keypair_add");
        storage.startCustomSession(chatId, ConfigSessionStorage.SessionType.SSH_PUBKEY_INPUT, data);

        return buildEditMessage(callbackQuery,
            "? *????*\n\n" +
            "????? RSA/ECDSA ?????.pub ?????\n\n" +
            "?????\n" +
            "`ssh-rsa AAAA... user@host`\n\n" +
            "?? /cancel ???"
        );
    }

    private BotApiMethod<? extends Serializable> confirmDeleteKey(CallbackQuery callbackQuery, String params) {
        String[] parts = params.split(":", 2);
        String userId = parts[0];
        String fingerprint = parts[1];
        return buildEditMessage(callbackQuery,
            "?? *???????*\n\n" +
            "???`" + fingerprint + "`\n\n" +
            "????????? SDK ??????",
            KeyboardBuilder.buildConfirmationKeyboard(
                "ssh_keypair_del:" + params,
                "ssh_keypair_list:" + userId
            )
        );
    }

    private BotApiMethod<? extends Serializable> deleteKey(CallbackQuery callbackQuery, String params) {
        String[] parts = params.split(":", 2);
        String userId = parts[0];
        String fingerprint = parts[1];
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "? ?????");

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                fetcher.getIdentityClient().deleteApiKey(
                    DeleteApiKeyRequest.builder()
                        .userId(user.getOciUserId())
                        .fingerprint(fingerprint)
                        .build()
                );
            }

            return buildEditMessage(callbackQuery,
                "? *?????*\n\n???`" + fingerprint + "`",
                KeyboardBuilder.fromRows(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "ssh_keypair_list:" + userId)),
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                ))
            );
        } catch (Exception e) {
            log.error("Failed to delete API key: {}", fingerprint, e);
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
}
