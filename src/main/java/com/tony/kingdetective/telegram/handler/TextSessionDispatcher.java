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

        sender.send(chatId, "❌ 当前操作不支持文件上传，请发送 /cancel 取消");
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
                "❌ URL 格式错误\n\n" +
                "必须以 http:// 或 https:// 开头\n\n" +
                "示例：\n" +
                "• http://192.168.1.100:6080\n" +
                "• https://vnc.example.com\n\n" +
                "请重新输入或发送 /cancel 取消"
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
                    "✅ *VNC URL 配置成功*\n\n" +
                    "配置的 URL: `%s`\n\n" +
                    "💡 在实例管理中选择单个实例后可使用此 URL 进行 VNC 连接。",
                    url
                )
            );
            log.info("VNC URL configured: chatId={}, url={}", chatId, url);
        } catch (Exception e) {
            log.error("Failed to save VNC URL", e);
            sender.send(chatId, "❌ 保存失败: " + e.getMessage());
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
            sender.send(chatId, "❌ 密码太短，至少需要 6 位字符\n\n请重新输入或发送 /cancel 取消");
            return;
        }

        sender.send(chatId, "⏳ 正在创建加密备份，请稍候...");

        try {
            ISysService sysService = SpringUtil.getBean(ISysService.class);
            BackupParams params = new BackupParams();
            params.setEnableEnc(true);
            params.setPassword(password);
            String backupFilePath = sysService.createBackupFile(params);

            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                throw new RuntimeException("备份文件不存在: " + backupFilePath);
            }

            String caption = "📦 *备份文件*\n\n" +
                "✅ 类型：加密备份\n" +
                "📅 时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                "⚠️ 请妥善保管密码，恢复时需要输入。";

            SendDocument sendDocument = SendDocument.builder()
                .chatId(chatId)
                .document(new InputFile(backupFile))
                .caption(caption)
                .parseMode("Markdown")
                .build();

            telegramClient.execute(sendDocument);
            FileUtil.del(backupFile);

            storage.clearSession(chatId);
            sender.sendMd(chatId, "✅ *加密备份成功，文件已发送。服务器副本已删除。*");
            log.info("Encrypted backup sent and deleted: chatId={}", chatId);
        } catch (Exception e) {
            log.error("Failed to create encrypted backup", e);
            sender.send(chatId, "❌ 备份失败: " + e.getMessage());
            storage.clearSession(chatId);
        }
    }

    private void handleRestoreFileUpload(long chatId, Update update, TelegramClient telegramClient, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        try {
            Document document = update.getMessage().getDocument();
            String fileName = document.getFileName();

            if (!fileName.toLowerCase().endsWith(".zip")) {
                sender.send(chatId, "❌ 只支持 ZIP 格式的备份文件，请重新上传或发送 /cancel 取消");
                return;
            }

            sender.send(chatId, "⏳ 正在下载备份文件...");

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
                "✅ *文件上传成功*\n\n" +
                "文件名：`" + fileName + "`\n\n" +
                "请发送解密密码（普通备份输入任意字符即可）\n" +
                "发送 /cancel 可取消操作"
            );
            log.info("Backup file downloaded: chatId={}, file={}", chatId, tempFilePath);
        } catch (Exception e) {
            log.error("Failed to handle backup file upload", e);
            sender.send(chatId, "❌ 文件上传失败: " + e.getMessage());
            storage.clearSession(chatId);
        }
    }

    private void handleRestorePasswordInput(long chatId, String password, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);

        if (state == null || state.getData().get("backupFilePath") == null) {
            sender.send(chatId, "❌ 会话已过期，请重新上传备份文件");
            storage.clearSession(chatId);
            return;
        }

        String backupFilePath = (String) state.getData().get("backupFilePath");
        password = password.trim();
        File backupFile = new File(backupFilePath);

        if (!backupFile.exists()) {
            sender.send(chatId, "❌ 备份文件不存在，请重新上传");
            storage.clearSession(chatId);
            return;
        }

        sender.send(chatId, "⏳ 正在恢复数据，请稍候（勿关闭程序）...");

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
                "✅ *数据恢复成功*\n\n" +
                "建议重启服务以确保所有配置生效。"
            );
            log.info("Data restored: chatId={}, file={}", chatId, backupFilePath);
        } catch (Exception e) {
            log.error("Failed to restore data: chatId={}", chatId, e);
            FileUtil.del(backupFile);
            storage.clearSession(chatId);
            sender.sendMd(chatId,
                "❌ *恢复失败*\n\n错误：" + e.getMessage() + "\n\n" +
                "可能原因：密码错误、文件损坏或文件不匹配。"
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
                    "❌ *配置格式错误*\n\n" +
                    "未检测到必要字段 (user, fingerprint, tenancy, region)。\n" +
                    "请检查复制的内容是否完整，或发送 /cancel 取消。"
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
                "✅ *配置已识别*\n\n" +
                "🔑 *第二步：上传私钥*\n\n" +
                "请发送私钥文件 (`.pem`) 或直接粘贴私钥文本内容。"
            );
        } catch (Exception e) {
            log.error("Failed to parse OCI config input", e);
            sender.send(chatId, "❌ 处理配置失败: " + e.getMessage());
        }
    }

    private void handleAddAccountKeyInput(long chatId, String text, TgMessageSender sender) {
        if (!text.contains("BEGIN") || !text.contains("PRIVATE KEY")) {
            sender.sendMd(chatId, "❌ *非法的私钥格式*\n\n请确保包含 `-----BEGIN ... PRIVATE KEY-----` 头。");
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
                sender.sendMd(chatId, "❌ *文件无效*\n\n文件内容不是有效的私钥格式。");
                return;
            }
            processAccountKey(chatId, keyContent, sender);
        } catch (Exception e) {
            log.error("Failed to handle key file upload", e);
            sender.send(chatId, "❌ 读取文件失败: " + e.getMessage());
        }
    }

    private void processAccountKey(long chatId, String keyContent, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);

        if (state == null) {
            sender.send(chatId, "❌ 会话已过期，请重新开始。");
            return;
        }

        state.getData().put("keyContent", keyContent);
        storage.startAddAccountRemark(chatId, state.getData());

        sender.sendMd(chatId,
            "✅ *私钥已接收*\n\n" +
            "🏷️ *第三步：设置备注名*\n\n" +
            "给这个账户取一个名字（例如：`US-SanJose` 或 `甲骨文1号`）。"
        );
    }

    private void handleAddAccountRemarkInput(long chatId, String remark, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);

        if (state == null) {
            sender.send(chatId, "❌ 会话已过期，请重新开始。");
            return;
        }

        sender.send(chatId, "⏳ 正在验证并保存...");

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
                    "🎉 *账户添加成功！*\n\n" +
                    "备注名: `%s`\n" +
                    "区域: `%s`\n" +
                    "状态: ✅ 已保存",
                    remark, region
                )
            );
            log.info("New OCI account added: chatId={}, remark={}, region={}", chatId, remark, region);
        } catch (Exception e) {
            log.error("Failed to save new account", e);
            sender.send(chatId, "❌ 保存失败: " + e.getMessage());
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
            sender.send(chatId, "❌ 会话已过期");
            return;
        }
        
        String userId = (String) state.getData().get("userId");
        text = text.trim();
        if (!text.startsWith("ssh-rsa") && !text.startsWith("ssh-ed25519") && !text.startsWith("ecdsa-sha2-nistp256")) {
            sender.send(chatId, "❌ 无效的公钥格式，必须以 ssh-rsa / ssh-ed25519 等开头");
            return;
        }

        sender.send(chatId, "⏳ 正在上传公钥...");
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sender.send(chatId, "❌ 账户不存在");
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
            sender.sendMd(chatId, "✅ *公钥已成功上传*");
        } catch (Exception e) {
            log.error("Failed to upload ssh pubkey", e);
            sender.send(chatId, "❌ 上传失败: " + e.getMessage());
        }
    }

    // 
    // 
    // 

    private void handleAlertEmailInput(long chatId, String email, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        email = email.trim();
        
        if (!email.contains("@") || !email.contains(".")) {
            sender.send(chatId, "❌ 邮箱格式不正确，请重新输入");
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
            sender.sendMd(chatId, "✅ *告警邮箱配置成功*\n\n当前邮箱：`" + email + "`");
        } catch (Exception e) {
            log.error("Failed to save alert email", e);
            sender.send(chatId, "❌ 配置失败: " + e.getMessage());
        }
    }

    // 
    // 
    // 

    private void handleInstanceTagInput(long chatId, String text, TgMessageSender sender) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        ConfigSessionStorage.SessionState state = storage.getSessionState(chatId);
        if (state == null) {
            sender.send(chatId, "❌ 会话已过期");
            return;
        }

        text = text.trim();
        if (!text.contains("=")) {
            sender.send(chatId, "❌ 格式错误，请使用 `key=value` 的形式发送");
            return;
        }

        String[] parts = text.split("=", 2);
        String key = parts[0].trim();
        String val = parts[1].trim();
        if (key.isEmpty()) {
            sender.send(chatId, "❌ 标签键不能为空");
            return;
        }

        String userId = (String) state.getData().get("userId");
        String instanceId = (String) state.getData().get("instanceId");

        sender.send(chatId, "⏳ 正在保存标签...");
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) {
                sender.send(chatId, "❌ 账户不存在");
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
            sender.sendMd(chatId, "✅ *标签已添加*\n\n`" + key + "` = `" + val + "`");
        } catch (Exception e) {
            log.error("Failed to add tag", e);
            sender.send(chatId, "❌ 保存失败: " + e.getMessage());
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
            sender.send(chatId, "❌ 会话已过期");
            return;
        }

        text = text.trim();
        String[] times = text.split("\\|");
        if (times.length != 2) {
            sender.send(chatId, "❌ 格式错误，请使用 `HH|HH` 的形式，例如 `01|08`");
            return;
        }

        String stopHourStr = times[0].trim();
        String startHourStr = times[1].trim();

        try {
            int stopHour = Integer.parseInt(stopHourStr);
            int startHour = Integer.parseInt(startHourStr);
            if (stopHour < 0 || stopHour > 23 || startHour < 0 || startHour > 23) {
                throw new NumberFormatException("小时必须在 00-23 之间");
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
            sender.sendMd(chatId, "✅ *定时任务配置成功！*\n\n设定为：\n- 每日 `" + stopHourStr + ":00` 关机\n- 每日 `" + startHourStr + ":00` 开机");

        } catch (NumberFormatException e) {
            sender.send(chatId, "❌ 格式错误: 小时必须是 0-23 的有效数字");
        } catch (Exception e) {
            log.error("Failed to save scheduled power config", e);
            sender.send(chatId, "❌ 保存失败: " + e.getMessage());
        }
    }
}
