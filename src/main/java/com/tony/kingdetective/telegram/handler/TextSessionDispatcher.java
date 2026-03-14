package com.tony.kingdetective.telegram.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.bean.params.sys.BackupParams;
import com.tony.kingdetective.enums.SysCfgEnum;
import com.tony.kingdetective.enums.SysCfgTypeEnum;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
import com.tony.kingdetective.telegram.utils.TgMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Session 
 *  TgBot.java  Session 
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class TextSessionDispatcher {

    @Value("${oci-cfg.key-dir-path}")
    private String keyDirPath;

    /**
     *  Session 
     *
     * @return true false  session
     */
    public boolean dispatch(long chatId, String text, TelegramClient telegramClient) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        if (!storage.hasActiveSession(chatId)) {
            return false;
        }

        ConfigSessionStorage.SessionType sessionType = storage.getSessionType(chatId);
        TgMessageSender sender = new TgMessageSender(telegramClient);

        switch (sessionType) {
            case VNC_CONFIG -> handleVncUrlInput(chatId, text, sender);
            case BACKUP_PASSWORD -> handleBackupPasswordInput(chatId, text, telegramClient, sender);
            case RESTORE_PASSWORD -> handleRestorePasswordInput(chatId, text, sender);
            case ADD_ACCOUNT_CONFIG -> handleAddAccountConfigInput(chatId, text, sender);
            case ADD_ACCOUNT_KEY -> handleAddAccountKeyInput(chatId, text, sender);
            case ADD_ACCOUNT_REMARK -> handleAddAccountRemarkInput(chatId, text, sender);
            case SSH_PUBKEY_INPUT -> handleSshPubkeyInput(chatId, text, sender);
            case ALERT_EMAIL_INPUT -> handleAlertEmailInput(chatId, text, sender);
            case INSTANCE_TAG_INPUT -> handleInstanceTagInput(chatId, text, sender);
            case SCHEDULED_POWER_INPUT -> handleScheduledPowerInput(chatId, text, sender);
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     *  /  PEM 
     *
     * @return true 
     */
    public boolean dispatchDocument(long chatId, Update update, TelegramClient telegramClient) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        if (!storage.hasActiveSession(chatId)) {
            return false;
        }

        TgMessageSender sender = new TgMessageSender(telegramClient);
        ConfigSessionStorage.SessionType sessionType = storage.getSessionType(chatId);

        if (sessionType == ConfigSessionStorage.SessionType.ADD_ACCOUNT_KEY) {
            handleAddAccountKeyFile(chatId, update, telegramClient, sender);
            return true;
        }

        if (sessionType == ConfigSessionStorage.SessionType.RESTORE_PASSWORD) {
            handleRestoreFileUpload(chatId, update, telegramClient, sender);
            return true;
        }

        sender.send(chatId, "? ??????????????? /cancel ??");
        return true;
    }

    // 
    // VNC 
    // 

    private void handleVncUrlInput(long chatId, String url, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        url = url.trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            sender.send(chatId,
                "? URL ????\n\n" +
                "??? http:// ? https:// ??\n\n" +
                "???\n" +
                "? http://192.168.1.100:6080\n" +
                "? https://vnc.example.com\n\n" +
                "???????? /cancel ??"
            );
            return;
        }

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        try {
            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OciKv::getCode, SysCfgEnum.SYS_VNC.getCode());
            OciKv vncConfig = kvService.getOne(wrapper);

            if (vncConfig != null) {
                vncConfig.setValue(url);
                kvService.updateById(vncConfig);
            } else {
                vncConfig = new OciKv();
                vncConfig.setId(IdUtil.getSnowflakeNextIdStr());
                vncConfig.setCode(SysCfgEnum.SYS_VNC.getCode());
                vncConfig.setValue(url);
                vncConfig.setType(SysCfgTypeEnum.SYS_INIT_CFG.getCode());
                kvService.save(vncConfig);
            }

            storage.clearSession(chatId);
            sender.sendMd(chatId,
                String.format(
                    "? *VNC URL ????*\n\n" +
                    "??? URL: `%s`\n\n" +
                    "? ????????????????? URL ?? VNC ???",
                    url
                )
            );
            log.info("VNC URL configured: chatId={}, url={}", chatId, url);
        } catch (Exception e) {
            log.error("Failed to save VNC URL", e);
            sender.send(chatId, "? ????: " + e.getMessage());
            storage.clearSession(chatId);
        }
    }

    // 
    //  / 
    // 

    private void handleBackupPasswordInput(long chatId, String password, TelegramClient telegramClient, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        password = password.trim();

        if (password.length() < 6) {
            sender.send(chatId, "? ????????? 6 ???\n\n???????? /cancel ??");
            return;
        }

        sender.send(chatId, "? ????????????...");

        try {
            ISysService sysService = SpringUtil.getBean(ISysService.class);
            BackupParams params = new BackupParams();
            params.setEnableEnc(true);
            params.setPassword(password);
            String backupFilePath = sysService.createBackupFile(params);

            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                throw new RuntimeException("???????: " + backupFilePath);
            }

            String caption = "? *????*\n\n" +
                "? ???????\n" +
                "? ???" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                "?? ????????????????";

            SendDocument sendDocument = SendDocument.builder()
                .chatId(chatId)
                .document(new InputFile(backupFile))
                .caption(caption)
                .parseMode("Markdown")
                .build();

            telegramClient.execute(sendDocument);
            FileUtil.del(backupFile);

            storage.clearSession(chatId);
            sender.sendMd(chatId, "? *??????????????????????*");
            log.info("Encrypted backup sent and deleted: chatId={}", chatId);
        } catch (Exception e) {
            log.error("Failed to create encrypted backup", e);
            sender.send(chatId, "? ????: " + e.getMessage());
            storage.clearSession(chatId);
        }
    }

    private void handleRestoreFileUpload(long chatId, Update update, TelegramClient telegramClient, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        try {
            Document document = update.getMessage().getDocument();
            String fileName = document.getFileName();

            if (!fileName.toLowerCase().endsWith(".zip")) {
                sender.send(chatId, "? ??? ZIP ???????????????? /cancel ??");
                return;
            }

            sender.send(chatId, "? ????????...");

            GetFile getFile = new GetFile(document.getFileId());
            org.telegram.telegrambots.meta.api.objects.File tgFile = telegramClient.execute(getFile);
            File downloadedFile = telegramClient.downloadFile(tgFile);

            String tempFilePath = System.getProperty("user.dir") + File.separator + "temp_restore_" + System.currentTimeMillis() + ".zip";
            File localFile = new File(tempFilePath);
            FileUtil.copy(downloadedFile, localFile, true);

            ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);
            if (state != null) {
                state.getData().put("backupFilePath", tempFilePath);
            }

            sender.sendMd(chatId,
                "? *??????*\n\n" +
                "????`" + fileName + "`\n\n" +
                "?????????????????????\n" +
                "?? /cancel ?????"
            );
            log.info("Backup file downloaded: chatId={}, file={}", chatId, tempFilePath);
        } catch (Exception e) {
            log.error("Failed to handle backup file upload", e);
            sender.send(chatId, "? ??????: " + e.getMessage());
            storage.clearSession(chatId);
        }
    }

    private void handleRestorePasswordInput(long chatId, String password, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);

        if (state == null || state.getData().get("backupFilePath") == null) {
            sender.send(chatId, "? ???????????????");
            storage.clearSession(chatId);
            return;
        }

        String backupFilePath = (String) state.getData().get("backupFilePath");
        password = password.trim();
        File backupFile = new File(backupFilePath);

        if (!backupFile.exists()) {
            sender.send(chatId, "? ?????????????");
            storage.clearSession(chatId);
            return;
        }

        sender.send(chatId, "? ?????????????????...");

        try {
            ISysService sysService = SpringUtil.getBean(ISysService.class);
            try {
                sysService.recoverFromFile(backupFilePath, password);
            } catch (Exception e) {
                if (password.length() < 3) {
                    sysService.recoverFromFile(backupFilePath, "");
                } else {
                    throw e;
                }
            }

            storage.clearSession(chatId);
            FileUtil.del(backupFile);

            sender.sendMd(chatId,
                "? *??????*\n\n" +
                "????????????????"
            );
            log.info("Data restored: chatId={}, file={}", chatId, backupFilePath);
        } catch (Exception e) {
            log.error("Failed to restore data: chatId={}", chatId, e);
            FileUtil.del(backupFile);
            storage.clearSession(chatId);
            sender.sendMd(chatId,
                "? *????*\n\n???" + e.getMessage() + "\n\n" +
                "?????????????????????"
            );
        }
    }

    // 
    // 
    // 

    private void handleAddAccountConfigInput(long chatId, String text, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        try {
            String user = getConfigValue(text, "user");
            String fingerprint = getConfigValue(text, "fingerprint");
            String tenancy = getConfigValue(text, "tenancy");
            String region = getConfigValue(text, "region");

            if (user == null || fingerprint == null || tenancy == null || region == null) {
                sender.sendMd(chatId,
                    "? *??????*\n\n" +
                    "???????? (user, fingerprint, tenancy, region)?\n" +
                    "???????????????? /cancel ???"
                );
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("fingerprint", fingerprint);
            data.put("tenancy", tenancy);
            data.put("region", region);
            data.put("rawConfig", text);

            storage.startAddAccountKey(chatId, data);

            sender.sendMd(chatId,
                "? *?????*\n\n" +
                "? *????????*\n\n" +
                "??????? (`.pem`) ????????????"
            );
        } catch (Exception e) {
            log.error("Failed to parse OCI config input", e);
            sender.send(chatId, "? ??????: " + e.getMessage());
        }
    }

    private void handleAddAccountKeyInput(long chatId, String text, TgMessageSender sender) {
        if (!text.contains("BEGIN") || !text.contains("PRIVATE KEY")) {
            sender.sendMd(chatId, "? *???????*\n\n????? `-----BEGIN ... PRIVATE KEY-----` ??");
            return;
        }
        processAccountKey(chatId, text, sender);
    }

    private void handleAddAccountKeyFile(long chatId, Update update, TelegramClient telegramClient, TgMessageSender sender) {
        try {
            Document document = update.getMessage().getDocument();
            GetFile getFile = new GetFile(document.getFileId());
            org.telegram.telegrambots.meta.api.objects.File tgFile = telegramClient.execute(getFile);
            File downloadedFile = telegramClient.downloadFile(tgFile);
            String keyContent = FileUtil.readUtf8String(downloadedFile);

            if (!keyContent.contains("BEGIN") || !keyContent.contains("PRIVATE KEY")) {
                sender.sendMd(chatId, "? *????*\n\n??????????????");
                return;
            }
            processAccountKey(chatId, keyContent, sender);
        } catch (Exception e) {
            log.error("Failed to handle key file upload", e);
            sender.send(chatId, "? ??????: " + e.getMessage());
        }
    }

    private void processAccountKey(long chatId, String keyContent, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);

        if (state == null) {
            sender.send(chatId, "? ????????????");
            return;
        }

        state.getData().put("keyContent", keyContent);
        storage.startAddAccountRemark(chatId, state.getData());

        sender.sendMd(chatId,
            "? *?????*\n\n" +
            "?? *?????????*\n\n" +
            "??????????????`US-SanJose` ? `???1?`??"
        );
    }

    private void handleAddAccountRemarkInput(long chatId, String remark, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);

        if (state == null) {
            sender.send(chatId, "? ????????????");
            return;
        }

        sender.send(chatId, "? ???????...");

        try {
            Map<String, Object> data = state.getData();
            String userOctId = (String) data.get("user");
            String fingerprint = (String) data.get("fingerprint");
            String tenancy = (String) data.get("tenancy");
            String region = (String) data.get("region");
            String keyContent = (String) data.get("keyContent");

            // 
            if (!FileUtil.exist(keyDirPath)) {
                FileUtil.mkdir(keyDirPath);
            }
            String safeRemark = remark.replaceAll("[^a-zA-Z0-9_-]", "_");
            String keyFileName = String.format("oci_api_key_%s_%d.pem", safeRemark, System.currentTimeMillis());
            String keyPath = keyDirPath + File.separator + keyFileName;
            FileUtil.writeUtf8String(keyContent, keyPath);

            //  (Linux)
            try {
                if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                    Files.setPosixFilePermissions(Paths.get(keyPath),
                        Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                }
            } catch (Exception ignored) {}

            // 
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser ociUser = new OciUser();
            ociUser.setId(IdUtil.getSnowflakeNextIdStr());
            ociUser.setUsername(remark);
            ociUser.setOciTenantId(tenancy);
            ociUser.setOciUserId(userOctId);
            ociUser.setOciFingerprint(fingerprint);
            ociUser.setOciRegion(region);
            ociUser.setOciKeyPath(keyPath);
            ociUser.setTenantName(remark);
            ociUser.setCreateTime(LocalDateTime.now());
            ociUser.setDeleted(0);
            userService.save(ociUser);

            storage.clearSession(chatId);

            sender.sendMd(chatId,
                String.format(
                    "? *???????*\n\n" +
                    "???: `%s`\n" +
                    "??: `%s`\n" +
                    "??: ? ???",
                    remark, region
                )
            );
            log.info("New OCI account added: chatId={}, remark={}, region={}", chatId, remark, region);
        } catch (Exception e) {
            log.error("Failed to save new account", e);
            sender.send(chatId, "? ????: " + e.getMessage());
            storage.clearSession(chatId);
        }
    }

    // 
    // SSH 
    // 

    private void handleSshPubkeyInput(long chatId, String text, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);
        if (state == null) {
            sender.send(chatId, "? ?????");
            return;
        }
        
        String userId = (String) state.getData().get("userId");
        text = text.trim();
        if (!text.startsWith("ssh-rsa") && !text.startsWith("ssh-ed25519") && !text.startsWith("ecdsa-sha2-nistp256")) {
            sender.send(chatId, "? ??????????? ssh-rsa / ssh-ed25519 ???");
            return;
        }

        sender.send(chatId, "? ??????...");
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sender.send(chatId, "? ?????");
                storage.clearSession(chatId);
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

            try (com.tony.kingdetective.config.OracleInstanceFetcher fetcher = new com.tony.kingdetective.config.OracleInstanceFetcher(dto)) {
                fetcher.getIdentityClient().uploadApiKey(
                    com.oracle.bmc.identity.requests.UploadApiKeyRequest.builder()
                        .userId(user.getOciUserId())
                        .createApiKeyDetails(
                            com.oracle.bmc.identity.model.CreateApiKeyDetails.builder()
                                .key(text)
                                .build()
                        )
                        .build()
                );
            }
            storage.clearSession(chatId);
            sender.sendMd(chatId, "? *???????*");
        } catch (Exception e) {
            log.error("Failed to upload ssh pubkey", e);
            sender.send(chatId, "? ????: " + e.getMessage());
        }
    }

    // 
    // 
    // 

    private void handleAlertEmailInput(long chatId, String email, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        email = email.trim();
        
        if (!email.contains("@") || !email.contains(".")) {
            sender.send(chatId, "? ?????????????");
            return;
        }

        try {
            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OciKv::getCode, "sys-alert-email");
            OciKv config = kvService.getOne(wrapper);

            if (config != null) {
                config.setValue(email);
                kvService.updateById(config);
            } else {
                config = new OciKv();
                config.setId(IdUtil.getSnowflakeNextIdStr());
                config.setCode("sys-alert-email");
                config.setValue(email);
                config.setType(SysCfgTypeEnum.SYS_INIT_CFG.getCode());
                kvService.save(config);
            }

            storage.clearSession(chatId);
            sender.sendMd(chatId, "? *????????*\n\n?????`" + email + "`");
        } catch (Exception e) {
            log.error("Failed to save alert email", e);
            sender.send(chatId, "? ????: " + e.getMessage());
        }
    }

    // 
    // 
    // 

    private void handleInstanceTagInput(long chatId, String text, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);
        if (state == null) {
            sender.send(chatId, "? ?????");
            return;
        }

        text = text.trim();
        if (!text.contains("=")) {
            sender.send(chatId, "? ???????? `key=value` ?????");
            return;
        }

        String[] parts = text.split("=", 2);
        String key = parts[0].trim();
        String val = parts[1].trim();
        if (key.isEmpty()) {
            sender.send(chatId, "? ???????");
            return;
        }

        String userId = (String) state.getData().get("userId");
        String instanceId = (String) state.getData().get("instanceId");

        sender.send(chatId, "? ??????...");
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sender.send(chatId, "? ?????");
                storage.clearSession(chatId);
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

            try (com.tony.kingdetective.config.OracleInstanceFetcher fetcher = new com.tony.kingdetective.config.OracleInstanceFetcher(dto)) {
                var instance = fetcher.getComputeClient().getInstance(
                    com.oracle.bmc.core.requests.GetInstanceRequest.builder()
                        .instanceId(instanceId)
                        .build()
                ).getInstance();

                java.util.Map<String, String> tags = new HashMap<>();
                if (instance.getFreeformTags() != null) {
                    tags.putAll(instance.getFreeformTags());
                }
                tags.put(key, val);

                fetcher.getComputeClient().updateInstance(
                    com.oracle.bmc.core.requests.UpdateInstanceRequest.builder()
                        .instanceId(instanceId)
                        .updateInstanceDetails(
                            com.oracle.bmc.core.model.UpdateInstanceDetails.builder()
                                .freeformTags(tags)
                                .build()
                        )
                        .build()
                );
            }

            storage.clearSession(chatId);
            sender.sendMd(chatId, "? *?????*\n\n`" + key + "` = `" + val + "`");
        } catch (Exception e) {
            log.error("Failed to add tag", e);
            sender.send(chatId, "? ????: " + e.getMessage());
        }
    }

    // 
    // 
    // 

    private String getConfigValue(String text, String key) {
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.startsWith(key + "=") || line.startsWith(key + " =")) {
                return line.split("=", 2)[1].trim();
            }
        }
        return null;
    }

    // 
    // 
    // 

    private void handleScheduledPowerInput(long chatId, String text, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);
        if (state == null) {
            sender.send(chatId, "? ?????");
            return;
        }

        text = text.trim();
        String[] times = text.split("\\|");
        if (times.length != 2) {
            sender.send(chatId, "? ???????? `HH|HH` ?????? `01|08`");
            return;
        }

        String stopHourStr = times[0].trim();
        String startHourStr = times[1].trim();

        try {
            int stopHour = Integer.parseInt(stopHourStr);
            int startHour = Integer.parseInt(startHourStr);
            if (stopHour < 0 || stopHour > 23 || startHour < 0 || startHour > 23) {
                throw new NumberFormatException("????? 00-23 ??");
            }
            //  "01" 
            stopHourStr = String.format("%02d", stopHour);
            startHourStr = String.format("%02d", startHour);

            String userId = (String) state.getData().get("userId");
            String instanceId = (String) state.getData().get("instanceId");

            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            String key = "scheduled_power:" + instanceId;
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<OciKv>().eq(OciKv::getCode, key);
            
            String configStr = "STOP=" + stopHourStr + ":00,START=" + startHourStr + ":00,USER=" + userId;
            OciKv cfg = kvService.getOne(wrapper);
            if (cfg != null) {
                cfg.setValue(configStr);
                kvService.updateById(cfg);
            } else {
                cfg = new OciKv();
                cfg.setId(IdUtil.getSnowflakeNextIdStr());
                cfg.setCode(key);
                cfg.setValue(configStr);
                cfg.setType(SysCfgTypeEnum.SYS_INIT_CFG.getCode());
                kvService.save(cfg);
            }

            storage.clearSession(chatId);
            sender.sendMd(chatId, "? *?????????*\n\n????\n- ?? `" + stopHourStr + ":00` ??\n- ?? `" + startHourStr + ":00` ??");

        } catch (NumberFormatException e) {
            sender.send(chatId, "? ????: ????? 0-23 ?????");
        } catch (Exception e) {
            log.error("Failed to save scheduled power config", e);
            sender.send(chatId, "? ????: " + e.getMessage());
        }
    }
}
