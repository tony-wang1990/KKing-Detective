package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IBootVolumeService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.InstanceSelectionStorage;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import lombok.extern.slf4j.Slf4j;
import com.oracle.bmc.core.BlockstorageClient;
import com.oracle.bmc.core.model.BootVolumeBackup;
import com.oracle.bmc.core.model.CreateBootVolumeBackupDetails;
import com.oracle.bmc.core.requests.CreateBootVolumeBackupRequest;
import com.oracle.bmc.core.requests.DeleteBootVolumeBackupRequest;
import com.oracle.bmc.core.requests.ListBootVolumeBackupsRequest;
import com.oracle.bmc.core.responses.ListBootVolumeBackupsResponse;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 📸 实例快照管理 Handler
 * 支持：查看快照列表、创建快照、删除快照
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class SnapshotManagementHandler extends AbstractCallbackHandler {

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.equals("snapshot_management") ||
            callbackData.startsWith("snapshot_list:") ||
            callbackData.startsWith("snapshot_create:") ||
            callbackData.startsWith("snapshot_delete_confirm:") ||
            callbackData.startsWith("snapshot_delete:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        if (data.equals("snapshot_management")) {
            return showAccountList(callbackQuery);
        } else if (data.startsWith("snapshot_list:")) {
            return showSnapshotList(callbackQuery, data.substring("snapshot_list:".length()));
        } else if (data.startsWith("snapshot_create:")) {
            return createSnapshot(callbackQuery, data.substring("snapshot_create:".length()), telegramClient);
        } else if (data.startsWith("snapshot_delete_confirm:")) {
            return confirmDelete(callbackQuery, data.substring("snapshot_delete_confirm:".length()));
        } else if (data.startsWith("snapshot_delete:")) {
            return deleteSnapshot(callbackQuery, data.substring("snapshot_delete:".length()), telegramClient);
        }
        return buildEditMessage(callbackQuery, "❌ 未知操作");
    }

    private BotApiMethod<? extends Serializable> showAccountList(CallbackQuery callbackQuery) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciUser> users = userService.getEnabledOciUserList();
        if (users == null || users.isEmpty()) {
            return buildEditMessage(callbackQuery, "❌ 暂无可用账户，请先添加 OCI 账户");
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (OciUser user : users) {
            rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("👤 " + user.getUsername() + " (" + user.getOciRegion() + ")",
                    "snapshot_list:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery,
            "📸 *实例快照管理*\n\n请选择要管理快照的账户：",
            new InlineKeyboardMarkup(rows)
        );
    }

    private BotApiMethod<? extends Serializable> showSnapshotList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "❌ 账户不存在");

            SysUserDTO sysUserDTO = buildSysUserDTO(user);

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                BlockstorageClient blockstorageClient = fetcher.getBlockstorageClient();
                String compartmentId = fetcher.getCompartmentId();

                ListBootVolumeBackupsResponse response = blockstorageClient.listBootVolumeBackups(
                    ListBootVolumeBackupsRequest.builder()
                        .compartmentId(compartmentId)
                        .limit(20)
                        .build()
                );

                List<BootVolumeBackup> backups = response.getItems();
                List<InlineKeyboardRow> rows = new ArrayList<>();

                if (backups.isEmpty()) {
                    rows.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("➕ 创建第一个快照", "snapshot_create:" + userId + ":auto")
                    ));
                } else {
                    StringBuilder sb = new StringBuilder("📸 *快照列表*（账户：" + user.getUsername() + "）\n\n");
                    int i = 1;
                    for (BootVolumeBackup backup : backups) {
                        sb.append(i++).append(". `").append(truncateName(backup.getDisplayName())).append("`\n")
                          .append("   状态：").append(stateEmoji(backup.getLifecycleState().getValue()))
                          .append(" | 大小：").append(backup.getSizeInGBs() != null ? backup.getSizeInGBs() + "GB" : "N/A").append("\n");

                        rows.add(new InlineKeyboardRow(
                            KeyboardBuilder.button("🗑️ 删除 " + truncateName(backup.getDisplayName()),
                                "snapshot_delete_confirm:" + backup.getId())
                        ));
                    }
                    rows.add(0, new InlineKeyboardRow(
                        KeyboardBuilder.button("➕ 创建新快照", "snapshot_create:" + userId + ":auto")
                    ));
                    rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

                    return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
                }

                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
                return buildEditMessage(callbackQuery, "📸 *快照列表*\n\n该账户暂无快照", new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list snapshots", e);
            return buildEditMessage(callbackQuery, "❌ 获取快照列表失败：" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> createSnapshot(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        String userId = parts[0];
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "❌ 账户不存在");

            SysUserDTO sysUserDTO = buildSysUserDTO(user);
            String snapshotName = "bot-snapshot-" + System.currentTimeMillis();

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                // 获取该账户下第一个 Boot Volume
                var bootVolumes = fetcher.getBlockstorageClient()
                    .listBootVolumes(
                        com.oracle.bmc.core.requests.ListBootVolumesRequest.builder()
                            .availabilityDomain(fetcher.getAvailabilityDomain())
                            .compartmentId(fetcher.getCompartmentId())
                            .build()
                    ).getItems();

                if (bootVolumes.isEmpty()) {
                    return buildEditMessage(callbackQuery, "❌ 未找到 Boot Volume，无法创建快照");
                }

                fetcher.getBlockstorageClient().createBootVolumeBackup(
                    CreateBootVolumeBackupRequest.builder()
                        .createBootVolumeBackupDetails(
                            CreateBootVolumeBackupDetails.builder()
                                .bootVolumeId(bootVolumes.get(0).getId())
                                .displayName(snapshotName)
                                .type(CreateBootVolumeBackupDetails.Type.Full)
                                .build()
                        )
                        .build()
                );
            }

            return buildEditMessage(callbackQuery,
                "✅ *快照创建任务已提交*\n\n" +
                "快照名称：`" + snapshotName + "`\n" +
                "账户：" + user.getUsername() + "\n\n" +
                "⏳ 快照创建需要几分钟，完成后可在快照列表中查看。",
                KeyboardBuilder.fromRows(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.button("🔄 刷新列表", "snapshot_list:" + userId)),
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                ))
            );
        } catch (Exception e) {
            log.error("Failed to create snapshot", e);
            return buildEditMessage(callbackQuery, "❌ 创建快照失败：" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> confirmDelete(CallbackQuery callbackQuery, String backupId) {
        return buildEditMessage(callbackQuery,
            "⚠️ *确认删除快照？*\n\n" +
            "快照 ID：`" + truncateName(backupId) + "`\n\n" +
            "此操作不可撤销！",
            KeyboardBuilder.buildConfirmationKeyboard(
                "snapshot_delete:" + backupId,
                "cancel"
            )
        );
    }

    private BotApiMethod<? extends Serializable> deleteSnapshot(CallbackQuery callbackQuery, String backupId, TelegramClient telegramClient) {
        // 需要找到该快照属于哪个账号，通过遍历账户来删除
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            List<OciUser> users = userService.getEnabledOciUserList();

            for (OciUser user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildSysUserDTO(user))) {
                    fetcher.getBlockstorageClient().deleteBootVolumeBackup(
                        DeleteBootVolumeBackupRequest.builder()
                            .bootVolumeBackupId(backupId)
                            .build()
                    );
                    return buildEditMessage(callbackQuery,
                        "✅ *快照已删除*",
                        KeyboardBuilder.fromRows(List.of(
                            new InlineKeyboardRow(KeyboardBuilder.button("← 返回列表", "snapshot_list:" + user.getId())),
                            new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                        ))
                    );
                } catch (Exception ignore) {}
            }
            return buildEditMessage(callbackQuery, "❌ 删除失败：找不到对应快照");
        } catch (Exception e) {
            log.error("Failed to delete snapshot: {}", backupId, e);
            return buildEditMessage(callbackQuery, "❌ 删除失败：" + e.getMessage());
        }
    }

    private SysUserDTO buildSysUserDTO(OciUser user) {
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

    private String truncateName(String name) {
        return name != null && name.length() > 30 ? name.substring(0, 30) + "..." : name;
    }

    private String stateEmoji(String state) {
        return switch (state) {
            case "AVAILABLE" -> "✅ 可用";
            case "CREATING" -> "⏳ 创建中";
            case "DELETING" -> "🗑️ 删除中";
            case "DELETED" -> "❌ 已删除";
            case "FAULTY" -> "⚠️ 异常";
            default -> state;
        };
    }
}
