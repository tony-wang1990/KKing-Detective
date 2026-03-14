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
                KeyboardBuilder.button("👤 " + user.getUsername(), "ssh_keypair_list:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery,
            "🔑 *SSH 密钥对管理*\n\n选择要管理密钥的账户：\n\n" +
            "💡 OCI API Key 用于 SDK 认证，即你上传的 RSA 公钥，每账户最多5个",
            new InlineKeyboardMarkup(rows)
        );
    }

    private BotApiMethod<? extends Serializable> showKeyList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "❌ 账户不存在");

            SysUserDTO dto = buildDto(user);
            List<InlineKeyboardRow> rows = new ArrayList<>();

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
                IdentityClient identityClient = fetcher.getIdentityClient();
                List<ApiKey> keys = identityClient.listApiKeys(
                    ListApiKeysRequest.builder()
                        .userId(user.getOciUserId())
                        .build()
                ).getItems();

                StringBuilder sb = new StringBuilder("🔑 *密钥列表*（账户：" + user.getUsername() + "）\n\n");
                if (keys.isEmpty()) {
                    sb.append("暂无密钥");
                } else {
                    int i = 1;
                    for (ApiKey key : keys) {
                        String fp = key.getFingerprint();
                        String state = key.getLifecycleState() != null ? key.getLifecycleState().getValue() : "UNKNOWN";
                        sb.append(i++).append(". `").append(fp).append("`\n")
                          .append("   状态: ").append("ACTIVE".equals(state) ? "✅ 活跃" : "⚠️ " + state).append("\n");
                        rows.add(new InlineKeyboardRow(
                            KeyboardBuilder.button("🗑️ 删除 " + fp.substring(0, Math.min(fp.length(), 20)) + "...",
                                "ssh_keypair_del_confirm:" + userId + ":" + fp)
                        ));
                    }
                    sb.append("\n共 ").append(keys.size()).append("/5 个密钥");
                }

                rows.add(0, new InlineKeyboardRow(
                    KeyboardBuilder.button("➕ 上传新公钥", "ssh_keypair_add:" + userId)
                ));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list API keys", e);
            return buildEditMessage(callbackQuery, "❌ 获取密钥列表失败：" + e.getMessage());
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
            "🔑 *上传公钥*\n\n" +
            "请直接发送 RSA/ECDSA 公钥内容（.pub 文件内容）\n\n" +
            "格式示例：\n" +
            "`ssh-rsa AAAA... user@host`\n\n" +
            "发送 /cancel 可取消"
        );
    }

    private BotApiMethod<? extends Serializable> confirmDeleteKey(CallbackQuery callbackQuery, String params) {
        String[] parts = params.split(":", 2);
        String userId = parts[0];
        String fingerprint = parts[1];
        return buildEditMessage(callbackQuery,
            "⚠️ *确认删除密钥？*\n\n" +
            "指纹：`" + fingerprint + "`\n\n" +
            "删除后使用该密钥的 SDK 连接将失效！",
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
            if (user == null) return buildEditMessage(callbackQuery, "❌ 账户不存在");

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                fetcher.getIdentityClient().deleteApiKey(
                    DeleteApiKeyRequest.builder()
                        .userId(user.getOciUserId())
                        .fingerprint(fingerprint)
                        .build()
                );
            }

            return buildEditMessage(callbackQuery,
                "✅ *密钥已删除*\n\n指纹：`" + fingerprint + "`",
                KeyboardBuilder.fromRows(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.button("← 返回密钥列表", "ssh_keypair_list:" + userId)),
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                ))
            );
        } catch (Exception e) {
            log.error("Failed to delete API key: {}", fingerprint, e);
            return buildEditMessage(callbackQuery, "❌ 删除失败：" + e.getMessage());
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
