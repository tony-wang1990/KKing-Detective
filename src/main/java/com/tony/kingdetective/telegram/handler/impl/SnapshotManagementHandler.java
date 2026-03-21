package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IBootVolumeService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
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
 *   Handler
 *     //
 *  BootVolume
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
            callbackData.startsWith("snapshot_instances:") ||
            callbackData.startsWith("snapshot_create:") ||
            callbackData.startsWith("snapshot_delete_confirm:") ||
            callbackData.startsWith("snapshot_delete:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.equals("snapshot_management")) {
            return showAccountList(callbackQuery);
        } else if (data.startsWith("snapshot_instances:")) {
            //   
            return showInstanceList(callbackQuery, data.substring("snapshot_instances:".length()));
        } else if (data.startsWith("snapshot_list:")) {
            // snapshot_list:<userId>:<instanceId>:<bootVolumeId>
            return showSnapshotList(callbackQuery, data.substring("snapshot_list:".length()));
        } else if (data.startsWith("snapshot_create:")) {
            // snapshot_create:<userId>:<bootVolumeId>
            return createSnapshot(callbackQuery, data.substring("snapshot_create:".length()), telegramClient);
        } else if (data.startsWith("snapshot_delete_confirm:")) {
            return confirmDelete(callbackQuery, data.substring("snapshot_delete_confirm:".length()));
        } else if (data.startsWith("snapshot_delete:")) {
            return deleteSnapshot(callbackQuery, data.substring("snapshot_delete:".length()), telegramClient);
        }
        return buildEditMessage(callbackQuery, "? ????");
    }

    //  Step 1:  
    private BotApiMethod<? extends Serializable> showAccountList(CallbackQuery callbackQuery) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciUser> users = userService.getEnabledOciUserList();
        if (users == null || users.isEmpty()) {
            return buildEditMessage(callbackQuery, "? ??????????? OCI ??");
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (OciUser user : users) {
            rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("? " + user.getUsername() + " (" + user.getOciRegion() + ")",
                    "snapshot_instances:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

        return buildEditMessage(callbackQuery,
            "? *??????*\n\n????????????",
            new InlineKeyboardMarkup(rows)
        );
    }

    //  Step 2:  
    private BotApiMethod<? extends Serializable> showInstanceList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "? ?????");

            SysUserDTO dto = buildSysUserDTO(user);
            List<InlineKeyboardRow> rows = new ArrayList<>();
            StringBuilder sb = new StringBuilder("? *????*????" + user.getUsername() + "?\n\n");

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
                var instances = fetcher.getComputeClient().listInstances(
                    com.oracle.bmc.core.requests.ListInstancesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .build()
                ).getItems();

                if (instances.isEmpty()) {
                    return buildEditMessage(callbackQuery, "? ????????");
                }

                for (var inst : instances) {
                    String state = inst.getLifecycleState().getValue();
                    if ("TERMINATED".equals(state) || "TERMINATING".equals(state)) continue;

                    //  Boot Volume ID
                    var bvAttachments = fetcher.getComputeClient().listBootVolumeAttachments(
                        com.oracle.bmc.core.requests.ListBootVolumeAttachmentsRequest.builder()
                            .availabilityDomain(inst.getAvailabilityDomain())
                            .compartmentId(fetcher.getCompartmentId())
                            .instanceId(inst.getId())
                            .build()
                    ).getItems();

                    if (bvAttachments.isEmpty()) continue;
                    String bootVolumeId = bvAttachments.get(0).getBootVolumeId();

                    sb.append("?? `").append(inst.getDisplayName()).append("` ? ").append(state).append("\n");
                    // callback: snapshot_list:<userId>:<instanceId>:<bootVolumeId>
                    rows.add(new InlineKeyboardRow(
                        KeyboardBuilder.button(
                            "? " + truncateName(inst.getDisplayName()),
                            "snapshot_list:" + userId + ":" + inst.getId() + ":" + bootVolumeId
                        )
                    ));
                }
            }

            rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "snapshot_management")));
            rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
            return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
        } catch (Exception e) {
            log.error("Failed to list instances for snapshot", e);
            return buildEditMessage(callbackQuery, "? ?????????" + e.getMessage());
        }
    }

    //  Step 3:  Boot Volume 
    private BotApiMethod<? extends Serializable> showSnapshotList(CallbackQuery callbackQuery, String params) {
        // params = <userId>:<instanceId>:<bootVolumeId>
        String[] parts = params.split(":", 3);
        if (parts.length < 3) return buildEditMessage(callbackQuery, "? ??????");
        String userId = parts[0];
        String instanceId = parts[1];
        String bootVolumeId = parts[2];

        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "? ?????");

            SysUserDTO sysUserDTO = buildSysUserDTO(user);

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                BlockstorageClient blockstorageClient = fetcher.getBlockstorageClient();

                ListBootVolumeBackupsResponse response = blockstorageClient.listBootVolumeBackups(
                    ListBootVolumeBackupsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .bootVolumeId(bootVolumeId)   //  
                        .limit(20)
                        .build()
                );

                List<BootVolumeBackup> backups = response.getItems();
                List<InlineKeyboardRow> rows = new ArrayList<>();

                //  bootVolumeId
                rows.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? ?????", "snapshot_create:" + userId + ":" + bootVolumeId)
                ));

                if (backups.isEmpty()) {
                    rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "snapshot_instances:" + userId)));
                    rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
                    return buildEditMessage(callbackQuery, "? *????*\n\n???????", new InlineKeyboardMarkup(rows));
                }

                StringBuilder sb = new StringBuilder("? *????*\n\n");
                int i = 1;
                for (BootVolumeBackup backup : backups) {
                    sb.append(i++).append(". `").append(truncateName(backup.getDisplayName())).append("`\n")
                      .append("   ???").append(stateEmoji(backup.getLifecycleState().getValue()))
                      .append(" | ???").append(backup.getSizeInGBs() != null ? backup.getSizeInGBs() + "GB" : "N/A").append("\n");

                    rows.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("?? ?? " + truncateName(backup.getDisplayName()),
                            "snapshot_delete_confirm:" + backup.getId())
                    ));
                }
                rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "snapshot_instances:" + userId)));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));

                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list snapshots", e);
            return buildEditMessage(callbackQuery, "? ?????????" + e.getMessage());
        }
    }

    //  Step 4:  bootVolumeId 
    private BotApiMethod<? extends Serializable> createSnapshot(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        // params = <userId>:<bootVolumeId>
        String[] parts = params.split(":", 2);
        if (parts.length < 2) return buildEditMessage(callbackQuery, "? ????");
        String userId = parts[0];
        String bootVolumeId = parts[1];

        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "? ?????");

            String snapshotName = "bot-snapshot-" + System.currentTimeMillis();

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildSysUserDTO(user))) {
                fetcher.getBlockstorageClient().createBootVolumeBackup(
                    CreateBootVolumeBackupRequest.builder()
                        .createBootVolumeBackupDetails(
                            CreateBootVolumeBackupDetails.builder()
                                .bootVolumeId(bootVolumeId)   //  
                                .displayName(snapshotName)
                                .type(CreateBootVolumeBackupDetails.Type.Full)
                                .build()
                        )
                        .build()
                );
            }

            return buildEditMessage(callbackQuery,
                "? *?????????*\n\n" +
                "?????`" + snapshotName + "`\n" +
                "???" + user.getUsername() + "\n\n" +
                "? ???????????????????????",
                KeyboardBuilder.fromRows(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "snapshot_instances:" + userId)),
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                ))
            );
        } catch (Exception e) {
            log.error("Failed to create snapshot", e);
            return buildEditMessage(callbackQuery, "? ???????" + e.getMessage());
        }
    }

    //  Step 5:  
    private BotApiMethod<? extends Serializable> confirmDelete(CallbackQuery callbackQuery, String backupId) {
        return buildEditMessage(callbackQuery,
            "?? *???????*\n\n" +
            "?? ID?`" + truncateName(backupId) + "`\n\n" +
            "????????",
            KeyboardBuilder.buildConfirmationKeyboard(
                "snapshot_delete:" + backupId,
                "cancel"
            )
        );
    }

    //  Step 6:  
    private BotApiMethod<? extends Serializable> deleteSnapshot(CallbackQuery callbackQuery, String backupId, TelegramClient telegramClient) {
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
                        "? *?????*",
                        KeyboardBuilder.fromRows(List.of(
                            new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "snapshot_management")),
                            new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                        ))
                    );
                } catch (Exception ignore) {}
            }
            return buildEditMessage(callbackQuery, "? ????????????");
        } catch (Exception e) {
            log.error("Failed to delete snapshot: {}", backupId, e);
            return buildEditMessage(callbackQuery, "? ?????" + e.getMessage());
        }
    }

    //  Helpers 
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
            case "AVAILABLE" -> "? ??";
            case "CREATING"  -> "? ???";
            case "DELETING"  -> "?? ???";
            case "DELETED"   -> "? ???";
            case "FAULTY"    -> "?? ??";
            default          -> state;
        };
    }

    @Override
    public String getCallbackPattern() {
        return "snapshotmanagement_";
    }
}