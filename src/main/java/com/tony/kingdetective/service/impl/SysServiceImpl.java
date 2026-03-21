package com.tony.kingdetective.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.system.SystemUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.tony.kingdetective.bean.constant.CacheConstant;
import com.tony.kingdetective.bean.dto.GoogleLoginConfigDTO;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.IpData;
import com.tony.kingdetective.bean.entity.OciCreateTask;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.bean.params.sys.*;
import com.tony.kingdetective.bean.response.sys.GetGlanceRsp;
import com.tony.kingdetective.bean.response.sys.GetSysCfgRsp;
import com.tony.kingdetective.bean.response.sys.LoginRsp;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.enums.EnableEnum;
import com.tony.kingdetective.enums.MessageTypeEnum;
import com.tony.kingdetective.enums.SysCfgEnum;
import com.tony.kingdetective.enums.SysCfgTypeEnum;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.mapper.OciKvMapper;
import com.tony.kingdetective.service.*;
import com.tony.kingdetective.telegram.TgBot;
import com.tony.kingdetective.utils.CommonUtils;
import com.tony.kingdetective.utils.CustomExpiryGuavaCache;
import com.tony.kingdetective.utils.MessageServiceFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tony.kingdetective.service.impl.OciServiceImpl.*;
import static com.tony.kingdetective.task.OciTask.botsApplication;
import static com.tony.kingdetective.task.OciTask.pushVersionUpdateMsg;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.service.impl
 * @className: ISysServiceImpl
 * @author: Tony Wang
 * @date: 2024/11/30 17:09
 */
@Service
@Slf4j
public class SysServiceImpl implements ISysService {

    @Value("${web.account}")
    private String account;
    @Value("${web.password}")
    private String password;

    @Resource
    private MessageServiceFactory messageServiceFactory;
    @Resource
    private IOciUserService userService;
    @Resource
    private IOciKvService kvService;
    @Resource
    @Lazy
    private IIpDataService ipDataService;
    @Resource
    private IOciCreateTaskService createTaskService;
    @Resource
    @Lazy
    private IInstanceService instanceService;
    @Resource
    private HttpServletRequest request;
    @Resource
    private HttpServletResponse response;
    @Resource
    private OciKvMapper kvMapper;
    @Resource
    private TaskScheduler taskScheduler;
    @Resource
    private ExecutorService virtualExecutor;
    @Resource
    private CustomExpiryGuavaCache<String, Object> customCache;

    @Override
    public void sendMessage(String message) {
        virtualExecutor.execute(() -> messageServiceFactory.getMessageService(MessageTypeEnum.MSG_TYPE_DING_DING).sendMessage(message));
        virtualExecutor.execute(() -> messageServiceFactory.getMessageService(MessageTypeEnum.MSG_TYPE_TELEGRAM).sendMessage(message));
    }

    @Override
    public LoginRsp login(LoginParams params) {
        String clientIp = CommonUtils.getClientIP(request);
        if (getEnableMfa()) {
            if (params.getMfaCode() == null) {
                log.error("??IP?{} ????????????????????????", clientIp);
                sendMessage(String.format("??IP?%s ????????????????????????", clientIp));
                throw new OciException(-1, "???????");
            }
            OciKv mfa = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                    .eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode()));
            if (!CommonUtils.verifyMfaCode(mfa.getValue(), params.getMfaCode())) {
                log.error("??IP?{} ????????????????????????", clientIp);
                sendMessage(String.format("??IP?%s ????????????????????????", clientIp));
                throw new OciException(-1, "??????");
            }
        }
        if (!params.getAccount().equals(account) || !params.getPassword().equals(password)) {
            log.error("??IP?{} ????????????????????????", clientIp);
            sendMessage(String.format("??IP?%s ????????????????????????", clientIp));
            throw new OciException(-1, "????????");
        }
        Map<String, Object> payload = new HashMap<>(1);
        payload.put("account", CommonUtils.getMD5(account));
        String token = CommonUtils.genToken(payload, password);

        String latestVersion = CommonUtils.getLatestVersion();
        String currentVersion = kvService.getObj(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.SYS_INFO_VERSION.getCode())
                .eq(OciKv::getType, SysCfgTypeEnum.SYS_INFO.getCode())
                .select(OciKv::getValue), String::valueOf);
        sendMessage(String.format("??IP?%s ????????%s", clientIp, LocalDateTime.now().format(CommonUtils.DATETIME_FMT_NORM)));
        LoginRsp rsp = new LoginRsp();
        rsp.setToken(token);
        rsp.setCurrentVersion(currentVersion);
        rsp.setLatestVersion(latestVersion);
        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSysCfg(UpdateSysCfgParams params) {
        kvService.remove(new LambdaQueryWrapper<OciKv>().eq(OciKv::getType, SysCfgTypeEnum.SYS_INIT_CFG.getCode()));
        kvService.saveBatch(SysCfgEnum.getCodeListByType(SysCfgTypeEnum.SYS_INIT_CFG).stream()
                .map(x -> {
                    OciKv ociKv = new OciKv();
                    ociKv.setId(IdUtil.getSnowflakeNextIdStr());
                    ociKv.setCode(x.getCode());
                    ociKv.setType(SysCfgTypeEnum.SYS_INIT_CFG.getCode());
                    switch (x) {
                        case SYS_TG_BOT_TOKEN:
                            ociKv.setValue(params.getTgBotToken());
                            break;
                        case SYS_TG_CHAT_ID:
                            ociKv.setValue(params.getTgChatId());
                            break;
                        case SYS_DING_BOT_TOKEN:
                            ociKv.setValue(params.getDingToken());
                            break;
                        case SYS_DING_BOT_SECRET:
                            ociKv.setValue(params.getDingSecret());
                            break;
                        case ENABLE_DAILY_BROADCAST:
                            ociKv.setValue(params.getEnableDailyBroadcast().toString());
                            break;
                        case DAILY_BROADCAST_CRON:
                            ociKv.setValue(params.getDailyBroadcastCron());
                            break;
                        case ENABLED_VERSION_UPDATE_NOTIFICATIONS:
                            ociKv.setValue(params.getEnableVersionInform().toString());
                            break;
                        case SILICONFLOW_AI_API:
                            ociKv.setValue(params.getGjAiApi());
                            customCache.remove(SysCfgEnum.SILICONFLOW_AI_API.getCode());
                            break;
                        case BOOT_BROADCAST_TOKEN:
                            ociKv.setValue(params.getBootBroadcastToken());
                            break;
                        case GOOGLE_ONE_CLICK_LOGIN:
                            GoogleLoginConfigDTO googleConfig = new GoogleLoginConfigDTO();
                            googleConfig.setEnabled(params.getEnableGoogleLogin() != null ? params.getEnableGoogleLogin() : false);
                            googleConfig.setClientId(params.getGoogleClientId());
                            googleConfig.setAllowedEmails(params.getAllowedEmails());
                            ociKv.setValue(JSONUtil.toJsonStr(googleConfig));
                            break;
                        default:
                            break;
                    }
                    return ociKv;
                }).collect(Collectors.toList()));
        if (params.getEnableMfa()) {
            OciKv mfaInDb = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                    .eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode()));
            if (mfaInDb == null) {
                String secretKey = CommonUtils.generateSecretKey();
                OciKv mfa = new OciKv();
                mfa.setId(IdUtil.getSnowflakeNextIdStr());
                mfa.setCode(SysCfgEnum.SYS_MFA_SECRET.getCode());
                mfa.setValue(secretKey);
                mfa.setType(SysCfgTypeEnum.SYS_MFA_CFG.getCode());
                String qrCodeURL = CommonUtils.generateQRCodeURL(secretKey, account, "king-detective");
                CommonUtils.genQRPic(CommonUtils.MFA_QR_PNG_PATH, qrCodeURL);
                kvService.save(mfa);
            }
        } else {
            kvService.remove(new LambdaQueryWrapper<OciKv>().eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode()));
            FileUtil.del(CommonUtils.MFA_QR_PNG_PATH);
        }

        startTgBot(params.getTgBotToken(), params.getTgChatId());

        stopTask(CacheConstant.PREFIX_PUSH_VERSION_UPDATE_MSG);
        stopTask(CacheConstant.PREFIX_PUSH_VERSION_UPDATE_MSG + "_push");
        if (params.getEnableVersionInform()) {
            pushVersionUpdateMsg(kvService, this);
        }

        ScheduledFuture<?> scheduledFuture = TASK_MAP.get(CacheConstant.DAILY_BROADCAST_TASK_ID);
        if (null != scheduledFuture) {
            scheduledFuture.cancel(true);
        }
        if (params.getEnableDailyBroadcast()) {
            TASK_MAP.put(CacheConstant.DAILY_BROADCAST_TASK_ID, taskScheduler.schedule(this::dailyBroadcastTask,
                    new CronTrigger(StrUtil.isBlank(params.getDailyBroadcastCron()) ? CacheConstant.TASK_CRON : params.getDailyBroadcastCron())));
        }
    }

    @Override
    public GetSysCfgRsp getSysCfg() {
        GetSysCfgRsp rsp = new GetSysCfgRsp();
        rsp.setDingToken(getCfgValue(SysCfgEnum.SYS_DING_BOT_TOKEN));
        rsp.setDingSecret(getCfgValue(SysCfgEnum.SYS_DING_BOT_SECRET));
        rsp.setTgChatId(getCfgValue(SysCfgEnum.SYS_TG_CHAT_ID));
        rsp.setTgBotToken(getCfgValue(SysCfgEnum.SYS_TG_BOT_TOKEN));
        String edbValue = getCfgValue(SysCfgEnum.ENABLE_DAILY_BROADCAST);
        rsp.setEnableDailyBroadcast(Boolean.valueOf(null == edbValue ? EnableEnum.ON.getCode() : edbValue));
        String dbcValue = getCfgValue(SysCfgEnum.DAILY_BROADCAST_CRON);
        rsp.setDailyBroadcastCron(null == dbcValue ? CacheConstant.TASK_CRON : dbcValue);
        String evunValue = getCfgValue(SysCfgEnum.ENABLED_VERSION_UPDATE_NOTIFICATIONS);
        rsp.setEnableVersionInform(Boolean.valueOf(null == evunValue ? EnableEnum.ON.getCode() : evunValue));
        rsp.setGjAiApi(getCfgValue(SysCfgEnum.SILICONFLOW_AI_API));
        rsp.setBootBroadcastToken(getCfgValue(SysCfgEnum.BOOT_BROADCAST_TOKEN));

        // Parse Google login configuration from JSON
        String googleLoginJson = getCfgValue(SysCfgEnum.GOOGLE_ONE_CLICK_LOGIN);
        if (StrUtil.isNotBlank(googleLoginJson)) {
            try {
                GoogleLoginConfigDTO googleConfig = JSONUtil.toBean(googleLoginJson, GoogleLoginConfigDTO.class);
                rsp.setEnableGoogleLogin(googleConfig.getEnabled());
                rsp.setGoogleClientId(googleConfig.getClientId());
                rsp.setAllowedEmails(googleConfig.getAllowedEmails());
            } catch (Exception e) {
                log.error("??Google???????{}", e.getMessage());
                rsp.setEnableGoogleLogin(false);
                rsp.setGoogleClientId(null);
                rsp.setAllowedEmails(null);
            }
        } else {
            rsp.setEnableGoogleLogin(false);
            rsp.setGoogleClientId(null);
            rsp.setAllowedEmails(null);
        }

        OciKv mfa = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode()));
        rsp.setEnableMfa(mfa != null);
        Optional.ofNullable(mfa).ifPresent(x -> {
            rsp.setMfaSecret(x.getValue());
            try (FileInputStream in = new FileInputStream(CommonUtils.MFA_QR_PNG_PATH);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                IoUtil.copy(in, out);
                rsp.setMfaQrData("data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()));
            } catch (Exception e) {
                log.error("??MFA????????{}", e.getLocalizedMessage());
            }
        });
        return rsp;
    }

    @Override
    public boolean getEnableMfa() {
        return kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode())) != null;
    }

    @Override
    public void backup(BackupParams params) {
        if (params.isEnableEnc() && StrUtil.isBlank(params.getPassword())) {
            throw new OciException(-1, "??????");
        }
        File tempDir = null;
        File dataFile = null;
        File outEncZip = null;
        try {
            String basicDirPath = System.getProperty("user.dir") + File.separator;
            tempDir = FileUtil.mkdir(basicDirPath + "king-detective-backup-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(DatePattern.PURE_DATETIME_PATTERN)));
            String keysDirPath = basicDirPath + "keys";
            FileUtil.copy(keysDirPath, tempDir.getAbsolutePath(), true);

            Map<String, IService> serviceMap = SpringUtil.getBeanFactory().getBeansOfType(IService.class);
            Map<String, List> listMap = serviceMap.entrySet().parallelStream()
                    .collect(Collectors.toMap(Map.Entry::getKey, (x) -> x.getValue().list()));
            String jsonStr = JSONUtil.toJsonStr(listMap);
            dataFile = FileUtil.touch(basicDirPath + "data.json");
            FileUtil.writeString(jsonStr, dataFile, Charset.defaultCharset());
            FileUtil.copy(dataFile, tempDir, true);

            outEncZip = FileUtil.touch(tempDir.getAbsolutePath() + ".zip");
            ZipFile zipFile = CommonUtils.zipFile(
                    params.isEnableEnc(),
                    tempDir.getAbsolutePath(),
                    params.getPassword(),
                    outEncZip.getAbsolutePath());

            response.setCharacterEncoding(CharsetUtil.UTF_8);
            try (BufferedInputStream bufferedInputStream = FileUtil.getInputStream(zipFile.getFile())) {
                CommonUtils.writeResponse(response, bufferedInputStream,
                        "application/octet-stream",
                        zipFile.getFile().getName());
            } catch (Exception e) {
                log.error("???????{}", e.getLocalizedMessage());
                throw new OciException(-1, "??????");
            }
        } catch (Exception e) {
            log.error("???????{}", e.getLocalizedMessage());
            throw new OciException(-1, "??????");
        } finally {
            FileUtil.del(tempDir);
            FileUtil.del(dataFile);
            FileUtil.del(outEncZip);
        }
    }

    @Override
    public String createBackupFile(BackupParams params) {
        if (params.isEnableEnc() && StrUtil.isBlank(params.getPassword())) {
            throw new OciException(-1, "??????");
        }
        File tempDir = null;
        File dataFile = null;
        File outEncZip = null;
        try {
            String basicDirPath = System.getProperty("user.dir") + File.separator;
            tempDir = FileUtil.mkdir(basicDirPath + "king-detective-backup-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(DatePattern.PURE_DATETIME_PATTERN)));
            String keysDirPath = basicDirPath + "keys";
            FileUtil.copy(keysDirPath, tempDir.getAbsolutePath(), true);

            Map<String, IService> serviceMap = SpringUtil.getBeanFactory().getBeansOfType(IService.class);
            Map<String, List> listMap = serviceMap.entrySet().parallelStream()
                    .collect(Collectors.toMap(Map.Entry::getKey, (x) -> x.getValue().list()));
            String jsonStr = JSONUtil.toJsonStr(listMap);
            dataFile = FileUtil.touch(basicDirPath + "data.json");
            FileUtil.writeString(jsonStr, dataFile, Charset.defaultCharset());
            FileUtil.copy(dataFile, tempDir, true);

            outEncZip = FileUtil.touch(tempDir.getAbsolutePath() + ".zip");
            ZipFile zipFile = CommonUtils.zipFile(
                    params.isEnableEnc(),
                    tempDir.getAbsolutePath(),
                    params.getPassword(),
                    outEncZip.getAbsolutePath());

            // Return the zip file path instead of writing to response
            String backupFilePath = zipFile.getFile().getAbsolutePath();
            log.info("????????: {}", backupFilePath);

            // Don't delete the zip file, caller will handle it
            FileUtil.del(tempDir);
            FileUtil.del(dataFile);

            return backupFilePath;
        } catch (Exception e) {
            log.error("???????{}", e.getLocalizedMessage());
            // Clean up on error
            FileUtil.del(tempDir);
            FileUtil.del(dataFile);
            FileUtil.del(outEncZip);
            throw new OciException(-1, "??????");
        }
    }

    @Override
    public void recover(RecoverParams params) {
        String basicDirPath = System.getProperty("user.dir") + File.separator;
        MultipartFile file = params.getFileList().get(0);
        File tempZip = FileUtil.createTempFile();
        File unzipDir = null;
        try (InputStream inputStream = file.getInputStream();
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();) {
            IoUtil.copy(inputStream, byteArrayOutputStream);

            FileUtil.writeBytes(byteArrayOutputStream.toByteArray(), tempZip);

            CommonUtils.unzipFile(basicDirPath, params.getEncryptionKey(), tempZip.getAbsolutePath());

            unzipDir = new File(basicDirPath + file.getOriginalFilename().replaceAll(".zip", ""));
            if (!unzipDir.exists()) {
                throw new OciException(-1, "????");
            }

            for (File unzipFile : unzipDir.listFiles()) {
                if (unzipFile.isDirectory() && unzipFile.getName().contains("keys")) {
                    FileUtil.copyFilesFromDir(unzipFile, new File(basicDirPath + "keys"), false);
                }
                if (unzipFile.isFile() && unzipFile.getName().contains("data.json")) {
                    Map<String, IService> serviceMap = SpringUtil.getBeanFactory().getBeansOfType(IService.class);
                    List<String> impls = new ArrayList<>(serviceMap.keySet());
                    String readJsonStr = FileUtil.readUtf8String(unzipFile);
                    Map<String, List> map = JSONUtil.toBean(readJsonStr, Map.class);

                    impls.forEach(x -> {
                        List list = map.get(x);
                        if (null != list) {
                            list.forEach(obj -> {
                                try {
                                    String time = String.valueOf(BeanUtil.getFieldValue(obj, "tenantCreateTime"));
                                    Long timestamp = Long.valueOf(time);
                                    LocalDateTime localDateTime = Instant.ofEpochMilli(timestamp)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDateTime();
                                    BeanUtil.setFieldValue(obj, "tenantCreateTime", localDateTime);
                                } catch (Exception ignored) {
                                }
                                try {
                                    String time = String.valueOf(BeanUtil.getFieldValue(obj, "createTime"));
                                    Long timestamp = Long.valueOf(time);
                                    LocalDateTime localDateTime = Instant.ofEpochMilli(timestamp)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDateTime();
                                    BeanUtil.setFieldValue(obj, "createTime", localDateTime);
                                } catch (Exception ignored) {
                                }
                            });

                            IService service = serviceMap.get(x);
                            Class entityClass = service.getEntityClass();
                            String simpleName = entityClass.getSimpleName();
                            TableName annotation = (TableName) entityClass.getAnnotation(TableName.class);
                            String tableName = annotation == null ? StrUtil.toUnderlineCase(simpleName) : annotation.value();
                            log.info("clear table:{}", tableName);
                            kvMapper.removeAllData(tableName);
                            log.info("restore table:{},size:{}", tableName, list.size());
                            service.saveBatch(list);
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("???????{}", e.getLocalizedMessage());
            throw new OciException(-1, "??????");
        } finally {
            FileUtil.del(tempZip);
            FileUtil.del(unzipDir);
            virtualExecutor.execute(() -> {
                initGenMfaPng();
                cleanAndRestartTask();
            });
        }
    }

    @Override
    public void recoverFromFile(String backupFilePath, String password) {
        String basicDirPath = System.getProperty("user.dir") + File.separator;
        File tempZip = new File(backupFilePath);
        File unzipDir = null;

        if (!tempZip.exists()) {
            throw new OciException(-1, "???????");
        }

        try {
            // 
            String tempUnzipDir = basicDirPath + "temp_unzip_" + System.currentTimeMillis();
            new File(tempUnzipDir).mkdirs();

            CommonUtils.unzipFile(tempUnzipDir, password, tempZip.getAbsolutePath());

            //  king-detective-backup-* 
            File tempUnzipDirFile = new File(tempUnzipDir);
            File[] subDirs = tempUnzipDirFile.listFiles(File::isDirectory);

            if (subDirs == null || subDirs.length == 0) {
                // 
                unzipDir = tempUnzipDirFile;
            } else {
                // 
                unzipDir = subDirs[0];
            }

            if (!unzipDir.exists() || unzipDir.listFiles() == null || unzipDir.listFiles().length == 0) {
                throw new OciException(-1, "???????????");
            }

            log.info("????????: {}", unzipDir.getAbsolutePath());

            for (File unzipFile : unzipDir.listFiles()) {
                if (unzipFile.isDirectory() && unzipFile.getName().contains("keys")) {
                    FileUtil.copyFilesFromDir(unzipFile, new File(basicDirPath + "keys"), false);
                    log.info("?? keys ????");
                }
                if (unzipFile.isFile() && unzipFile.getName().contains("data.json")) {
                    log.info("?????????...");
                    Map<String, IService> serviceMap = SpringUtil.getBeanFactory().getBeansOfType(IService.class);
                    List<String> impls = new ArrayList<>(serviceMap.keySet());
                    String readJsonStr = FileUtil.readUtf8String(unzipFile);
                    Map<String, List> map = JSONUtil.toBean(readJsonStr, Map.class);

                    impls.forEach(x -> {
                        List list = map.get(x);
                        if (null != list) {
                            list.forEach(obj -> {
                                try {
                                    String time = String.valueOf(BeanUtil.getFieldValue(obj, "tenantCreateTime"));
                                    Long timestamp = Long.valueOf(time);
                                    LocalDateTime localDateTime = Instant.ofEpochMilli(timestamp)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDateTime();
                                    BeanUtil.setFieldValue(obj, "tenantCreateTime", localDateTime);
                                } catch (Exception ignored) {
                                }
                                try {
                                    String time = String.valueOf(BeanUtil.getFieldValue(obj, "createTime"));
                                    Long timestamp = Long.valueOf(time);
                                    LocalDateTime localDateTime = Instant.ofEpochMilli(timestamp)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDateTime();
                                    BeanUtil.setFieldValue(obj, "createTime", localDateTime);
                                } catch (Exception ignored) {
                                }
                            });

                            IService service = serviceMap.get(x);
                            Class entityClass = service.getEntityClass();
                            String simpleName = entityClass.getSimpleName();
                            TableName annotation = (TableName) entityClass.getAnnotation(TableName.class);
                            String tableName = annotation == null ? StrUtil.toUnderlineCase(simpleName) : annotation.value();
                            log.info("clear table:{}", tableName);
                            kvMapper.removeAllData(tableName);
                            log.info("restore table:{},size:{}", tableName, list.size());
                            service.saveBatch(list);
                        }
                    });
                    log.info("?????????");
                }
            }

            log.info("??????");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("???????{}", e.getLocalizedMessage());
            throw new OciException(-1, "??????");
        } finally {
            // 
            if (unzipDir != null) {
                try {
                    // 
                    File tempUnzipDirFile = new File(basicDirPath + "temp_unzip_" +
                            unzipDir.getParentFile().getName().replace("temp_unzip_", ""));
                    if (tempUnzipDirFile.exists()) {
                        FileUtil.del(tempUnzipDirFile);
                    } else {
                        FileUtil.del(unzipDir);
                    }
                } catch (Exception e) {
                    log.warn("????????", e);
                }
            }
            virtualExecutor.execute(() -> {
                initGenMfaPng();
                cleanAndRestartTask();
            });
        }
    }

    @Override
    public GetGlanceRsp glance() {
        GetGlanceRsp rsp = new GetGlanceRsp();
        List<String> ids = userService.listObjs(new LambdaQueryWrapper<OciUser>()
                .isNotNull(OciUser::getId)
                .select(OciUser::getId), String::valueOf);

        CompletableFuture<List<GetGlanceRsp.MapData>> mapDataFuture = CompletableFuture.supplyAsync(() -> Optional.ofNullable(ipDataService.list())
                .filter(CollectionUtil::isNotEmpty).orElseGet(Collections::emptyList).parallelStream()
                .filter(ip -> ip.getLat() != null && ip.getLng() != null)
                .collect(Collectors.groupingBy(
                        ip -> new AbstractMap.SimpleEntry<>(ip.getLat(), ip.getLng()),
                        Collectors.toList()
                ))
                .entrySet().parallelStream()
                .map(entry -> {
                    GetGlanceRsp.MapData mapData = new GetGlanceRsp.MapData();
                    mapData.setCountry(entry.getValue().get(0).getCountry());
                    mapData.setArea(entry.getValue().get(0).getArea());
                    mapData.setOrg(entry.getValue().stream().map(IpData::getOrg).distinct().collect(Collectors.joining(",")));
                    mapData.setAsn(entry.getValue().stream().map(IpData::getAsn).distinct().collect(Collectors.joining(",")));
                    mapData.setLat(entry.getKey().getKey());
                    mapData.setLng(entry.getKey().getValue());
                    mapData.setCount(entry.getValue().size());
                    mapData.setCity(entry.getValue().get(0).getCity());
                    return mapData;
                })
                .collect(Collectors.toList()), virtualExecutor);

        CompletableFuture<String> tasksFuture = CompletableFuture.supplyAsync(() -> {
            List<String> userIds = createTaskService.listObjs(new LambdaQueryWrapper<OciCreateTask>()
                    .isNotNull(OciCreateTask::getId)
                    .select(OciCreateTask::getUserId), String::valueOf);
            return String.valueOf(userIds.size());
        }, virtualExecutor);

        CompletableFuture<String> regionsFuture = CompletableFuture.supplyAsync(() -> {
            if (CollectionUtil.isEmpty(ids)) {
                return "0";
            }

            return String.valueOf(userService.listObjs(new LambdaQueryWrapper<OciUser>()
                            .isNotNull(OciUser::getId)
                            .select(OciUser::getOciRegion), String::valueOf)
                    .stream().distinct().count());
        }, virtualExecutor);

        CompletableFuture<String> daysFuture = CompletableFuture.supplyAsync(() -> {
            long uptimeMillis = SystemUtil.getRuntimeMXBean().getUptime();
            return String.valueOf(uptimeMillis / (24 * 60 * 60 * 1000));
        }, virtualExecutor);

        CompletableFuture<String> currentVersionFuture = CompletableFuture.supplyAsync(() -> kvService.getObj(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.SYS_INFO_VERSION.getCode())
                .eq(OciKv::getType, SysCfgTypeEnum.SYS_INFO.getCode())
                .select(OciKv::getValue), String::valueOf), virtualExecutor);

        CompletableFuture.allOf(mapDataFuture, tasksFuture, regionsFuture, daysFuture, currentVersionFuture).join();

        try {
            rsp.setCities(mapDataFuture.get());
            rsp.setUsers(String.valueOf(ids.size()));
            rsp.setTasks(tasksFuture.get());
            rsp.setRegions(regionsFuture.get());
            rsp.setDays(daysFuture.get());
            rsp.setCurrentVersion(currentVersionFuture.get());
        } catch (Exception e) {
            log.error("????????", e);
            throw new OciException(-1, "Error while fetching glance data");
        }

        return rsp;
    }

    @Override
    public List<SysUserDTO> list() {
        List<OciUser> users = userService.list();
        if (CollectionUtil.isEmpty(users)) {
            return Collections.emptyList();
        }
        return users.stream().map(ociUser -> SysUserDTO.builder()
                .ociCfg(SysUserDTO.OciCfg.builder()
                        .id(ociUser.getId())
                        .userId(ociUser.getOciUserId())
                        .tenantId(ociUser.getOciTenantId())
                        .region(ociUser.getOciRegion())
                        .fingerprint(ociUser.getOciFingerprint())
                        .privateKeyPath(ociUser.getOciKeyPath())
                        .privateKey(ociUser.getPrivateKey())
                        .build())
                .username(ociUser.getUsername())
                .build()).collect(Collectors.toList());
    }

    @Override
    public SysUserDTO getOciUser(String ociCfgId) {
        OciUser ociUser = userService.getById(ociCfgId);
        if (ociUser == null) {
            throw new OciException(-1, "???? OCI ???????????");
        }
        return SysUserDTO.builder()
                .ociCfg(SysUserDTO.OciCfg.builder()
                        .userId(ociUser.getOciUserId())
                        .tenantId(ociUser.getOciTenantId())
                        .region(ociUser.getOciRegion())
                        .fingerprint(ociUser.getOciFingerprint())
                        .privateKeyPath(ociUser.getOciKeyPath())
                        .privateKey(ociUser.getPrivateKey())
                        .build())
                .username(ociUser.getUsername())
                .build();
    }

    @Override
    public SysUserDTO getOciUser(String ociCfgId, String region, String compartmentId) {
        OciUser ociUser = userService.getById(ociCfgId);
        if (ociUser == null) {
            throw new OciException(-1, "???? OCI ???????????");
        }
        return SysUserDTO.builder()
                .ociCfg(SysUserDTO.OciCfg.builder()
                        .userId(ociUser.getOciUserId())
                        .tenantId(ociUser.getOciTenantId())
                        .region(StrUtil.isBlank(region) ? ociUser.getOciRegion() : region)
                        .fingerprint(ociUser.getOciFingerprint())
                        .privateKeyPath(ociUser.getOciKeyPath())
                        .privateKey(ociUser.getPrivateKey())
                        .compartmentId(compartmentId)
                        .build())
                .username(ociUser.getUsername())
                .build();
    }

    @Override
    public void checkMfaCode(String mfaCode) {
        OciKv mfa = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode()));
        if (!CommonUtils.verifyMfaCode(mfa.getValue(), Integer.parseInt(mfaCode))) {
            throw new OciException(-1, "??????");
        }
    }

    @Override
    public void updateVersion() {
        String latestVersion = CommonUtils.getLatestVersion();
        String currentVersion = kvService.getObj(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.SYS_INFO_VERSION.getCode())
                .eq(OciKv::getType, SysCfgTypeEnum.SYS_INFO.getCode())
                .select(OciKv::getValue), String::valueOf);
        if (latestVersion.equals(currentVersion)) {
            throw new OciException(-1, "?????????????????????");
        }
        List<String> command = List.of("/bin/sh", "-c", "echo trigger > /app/king-detective/update_version_trigger.flag");
        Process process = RuntimeUtil.exec(command.toArray(new String[0]));

        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            log.error("TG Bot error", e);
        }

        if (exitCode == 0) {
            log.info("Start the version update task...");
        } else {
            log.error("version update task exec error,exitCode:{}", exitCode);
        }
    }

    @Override
    public LoginRsp googleLogin(GoogleLoginParams params) {
        String clientIp = CommonUtils.getClientIP(request);
        log.info("??Google?????IP: {}, credential??: {}", clientIp,
                params.getCredential() != null ? params.getCredential().length() : 0);
        try {
            // Get Google login configuration from database
            String googleLoginJson = getCfgValue(SysCfgEnum.GOOGLE_ONE_CLICK_LOGIN);
            if (StrUtil.isBlank(googleLoginJson)) {
                log.error("??IP?{} Google?????Google???????", clientIp);
                throw new OciException(-1, "Google???????");
            }

            GoogleLoginConfigDTO googleConfig = JSONUtil.toBean(googleLoginJson, GoogleLoginConfigDTO.class);
            if (googleConfig.getEnabled() == null || !googleConfig.getEnabled()) {
                log.error("??IP?{} Google?????Google???????", clientIp);
                throw new OciException(-1, "Google???????");
            }

            if (StrUtil.isBlank(googleConfig.getClientId())) {
                log.error("??IP?{} Google?????Google Client ID???", clientIp);
                throw new OciException(-1, "Google Client ID???");
            }

            // Verify the Google ID token
            GoogleIdToken idToken = null;
            try {
                log.info("????Google Token, Client ID: {}", googleConfig.getClientId());

                // credential50
                String credentialPreview = params.getCredential().length() > 50
                        ? params.getCredential().substring(0, 50) + "..."
                        : params.getCredential();
                log.info("Credential ??: {}", params.getCredential());

                //  Google 
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                        .setAudience(Collections.singletonList(googleConfig.getClientId()))
                        .setIssuer("https://accounts.google.com") //  issuer
                        .build();

                log.info("???? verifier.verify()...");
                idToken = verifier.verify(params.getCredential());
                log.info("verifier.verify() ????: {}", idToken != null ? "??" : "NULL");

                if (idToken != null) {
                    log.info("Token?????");
                } else {
                    log.error("Token????null????????");
                    log.error("1. ???????Google???endpoint");
                    log.error("2. Client ID ???");
                    log.error("3. Token ????");
                    // 
                    sendMessage(String.format("??IP?%s Google?????Token????", clientIp));
                    throw new OciException(-1, "???Google???Token????");
                }
            } catch (Exception e) {
                log.error("??IP?{} Google???????token???{}", clientIp, e.getMessage(), e);
                log.error("???????", e);
                sendMessage(String.format("??IP?%s Google?????????????: %s", clientIp, e.getMessage()));
                throw new OciException(-1, "???Google??: " + e.getMessage());
            }

            if (idToken == null) {
                log.error("??IP?{} Google???????????token?null?", clientIp);
                sendMessage(String.format("??IP?%s Google??????????????????????????????", clientIp));
                throw new OciException(-1, "???Google??");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String userId = payload.getSubject(); // GoogleID
            Long expirationTime = payload.getExpirationTimeSeconds();
            Long issuedAt = payload.getIssuedAtTimeSeconds();

            log.info("Google Token???? - Email: {}, UserID: {}, Issuer: {}, Audience: {}, Exp: {}, Iat: {}",
                    email, userId, payload.getIssuer(), payload.getAudience(), expirationTime, issuedAt);

            // Check token expiration
            long currentTime = System.currentTimeMillis() / 1000;
            if (expirationTime != null && expirationTime < currentTime) {
                log.error("??IP?{} Google?????token?????????{}??????{}",
                        clientIp, expirationTime, currentTime);
                throw new OciException(-1, "Google?????");
            }

            // Check if token is issued in the future (clock skew attack)
            if (issuedAt != null && issuedAt > currentTime + 300) { // 5 minutes tolerance
                log.error("??IP?{} Google?????token??????????????{}??????{}",
                        clientIp, issuedAt, currentTime);
                throw new OciException(-1, "???Google??");
            }

            // Check token age (should be fresh, not older than 5 minutes)
            if (issuedAt != null && (currentTime - issuedAt) > 300) {
                log.warn("??IP?{} Google????????token??????{}??????{}????{}?",
                        clientIp, issuedAt, currentTime, (currentTime - issuedAt));
                // Not throwing error, just warning for now
            }

            // Anti-replay attack: check if token has been used before
            String tokenHash = CommonUtils.getMD5(params.getCredential());
            String cacheKey = "GOOGLE_TOKEN_USED:" + tokenHash;
            Object usedToken = customCache.get(cacheKey);
            if (usedToken != null) {
                log.error("??IP?{} Google??????token??????????????Email: {}", clientIp, email);
                sendMessage(String.format("??IP?%s ??????Google??token?????????Email: %s", clientIp, email));
                throw new OciException(-1, "?Google????????????");
            }
            // Mark token as used (cache for exp time or 1 hour, whichever is longer)
            long cacheDuration = expirationTime != null ? (expirationTime - currentTime + 3600) : 3600;
            customCache.put(cacheKey, true, cacheDuration);

            // Additional security checks
            String issuer = payload.getIssuer();
            if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
                log.error("??IP?{} Google????????issuer?{}", clientIp, issuer);
                sendMessage(String.format("??IP?%s Google????????issuer: %s", clientIp, issuer));
                throw new OciException(-1, "???Google??");
            }

            String audience = (String) payload.getAudience();
            if (!googleConfig.getClientId().equals(audience)) {
                log.error("??IP?{} Google????????audience?{}????{}", clientIp, audience, googleConfig.getClientId());
                sendMessage(String.format("??IP?%s Google????????audience", clientIp));
                throw new OciException(-1, "???Google??");
            }

            boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            log.info("??IP?{} ????Google?? {} ??", clientIp, email);

            if (!emailVerified) {
                log.error("??IP?{} Google??????????", clientIp);
                throw new OciException(-1, "Google?????");
            }

            // Validate email whitelist - must be configured
            if (StrUtil.isBlank(googleConfig.getAllowedEmails())) {
                log.error("??IP?{} Google????????????????", clientIp);
                sendMessage(String.format("??IP?%s ?????? %s Google??????????", clientIp, email));
                throw new OciException(-1, "?????????????Google?????");
            }

            // Check if email is in whitelist (exact match)
            String[] allowedEmailsArray = googleConfig.getAllowedEmails().split(",");
            boolean isAllowed = false;
            for (String allowedEmail : allowedEmailsArray) {
                String trimmedEmail = allowedEmail.trim();
                if (StrUtil.isNotBlank(trimmedEmail) && email.equalsIgnoreCase(trimmedEmail)) {
                    isAllowed = true;
                    log.info("?? {} ??????????", email);
                    break;
                }
            }

            if (!isAllowed) {
                log.error("??IP?{} Google??????? {} ?????????", clientIp, email);
                log.error("?????????{}", googleConfig.getAllowedEmails());
                sendMessage(String.format("??IP?%s ????????Google?? %s ???????????????????????", clientIp, email));
                throw new OciException(-1, "?Google???????????????");
            }

            // Generate JWT token
            Map<String, Object> tokenPayload = new HashMap<>(2);
            tokenPayload.put("account", CommonUtils.getMD5(email));
            tokenPayload.put("googleUser", true);
            String token = CommonUtils.genToken(tokenPayload, password);

            // Get version info
            String latestVersion = CommonUtils.getLatestVersion();
            String currentVersion = kvService.getObj(new LambdaQueryWrapper<OciKv>()
                    .eq(OciKv::getCode, SysCfgEnum.SYS_INFO_VERSION.getCode())
                    .eq(OciKv::getType, SysCfgTypeEnum.SYS_INFO.getCode())
                    .select(OciKv::getValue), String::valueOf);

            sendMessage(String.format("Google?? [%s] ?IP?%s ????????%s",
                    email, clientIp, LocalDateTime.now().format(CommonUtils.DATETIME_FMT_NORM)));

            LoginRsp rsp = new LoginRsp();
            rsp.setToken(token);
            rsp.setCurrentVersion(currentVersion);
            rsp.setLatestVersion(latestVersion);
            return rsp;
        } catch (Exception e) {
            log.error("??IP?{} Google??????????{}", clientIp, e.getMessage(), e);
            sendMessage(String.format("??IP?%s Google????????????????????????", clientIp));
            if (e instanceof OciException) {
                throw (OciException) e;
            }
            throw new OciException(-1, "Google?????" + e.getMessage());
        }
    }

    @Override
    public String getGoogleClientId() {
        String googleLoginJson = getCfgValue(SysCfgEnum.GOOGLE_ONE_CLICK_LOGIN);
        if (StrUtil.isBlank(googleLoginJson)) {
            return null;
        }

        try {
            GoogleLoginConfigDTO googleConfig = JSONUtil.toBean(googleLoginJson, GoogleLoginConfigDTO.class);
            // Only return client ID if Google login is enabled
            if (googleConfig.getEnabled() != null && googleConfig.getEnabled()) {
                return googleConfig.getClientId();
            }
            return null;
        } catch (Exception e) {
            log.error("??Google???????{}", e.getMessage());
            return null;
        }
    }

    private String getCfgValue(SysCfgEnum sysCfgEnum) {
        OciKv cfg = kvService.getOne(new LambdaQueryWrapper<OciKv>().eq(OciKv::getCode, sysCfgEnum.getCode()));
        return cfg == null ? null : cfg.getValue();
    }

    private void cleanAndRestartTask() {
        Random random = new Random();
        Optional.ofNullable(createTaskService.list())
                .filter(CollectionUtil::isNotEmpty).orElseGet(Collections::emptyList).stream()
                .forEach(task -> {
                    //  5~10 
                    int delay = 5 + random.nextInt(6);
                    CREATE_INSTANCE_POOL.schedule(() -> {
                        if (task.getCreateNumbers() <= 0) {
                            createTaskService.removeById(task.getId());
                        } else {
                            OciUser ociUser = userService.getById(task.getUserId());
                            SysUserDTO sysUserDTO = SysUserDTO.builder()
                                    .ociCfg(SysUserDTO.OciCfg.builder()
                                            .userId(ociUser.getOciUserId())
                                            .tenantId(ociUser.getOciTenantId())
                                            .region(ociUser.getOciRegion())
                                            .fingerprint(ociUser.getOciFingerprint())
                                            .privateKeyPath(ociUser.getOciKeyPath())
                                            .privateKey(ociUser.getPrivateKey())
                                            .build())
                                    .taskId(task.getId())
                                    .username(ociUser.getUsername())
                                    .ocpus(task.getOcpus())
                                    .memory(task.getMemory())
                                    .disk(task.getDisk().equals(50) ? null : Long.valueOf(task.getDisk()))
                                    .architecture(task.getArchitecture())
                                    .interval(Long.valueOf(task.getInterval()))
                                    .createNumbers(task.getCreateNumbers())
                                    .operationSystem(task.getOperationSystem())
                                    .rootPassword(task.getRootPassword())
                                    .build();
                            stopTask(CommonUtils.CREATE_TASK_PREFIX + task.getId());
                            addTask(CommonUtils.CREATE_TASK_PREFIX + task.getId(), () ->
                                            execCreate(sysUserDTO, this, instanceService, createTaskService),
                                    0, task.getInterval(), TimeUnit.SECONDS);
                        }
                    }, delay, TimeUnit.SECONDS);
                });
    }

    private void initGenMfaPng() {
        Optional.ofNullable(kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode()))).ifPresent(mfa -> {
            String qrCodeURL = CommonUtils.generateQRCodeURL(mfa.getValue(), account, "king-detective");
            CommonUtils.genQRPic(CommonUtils.MFA_QR_PNG_PATH, qrCodeURL);
        });
    }

    private void dailyBroadcastTask() {
        String message = "??????\n" +
                "\n" +
                "\uD83D\uDD58 ???\t%s\n" +
                "\uD83D\uDD11 ?API????\t%s\n" +
                "? ??API????\t%s\n" +
                "?\uFE0F ???API???\t\n- %s\n" +
                "\uD83D\uDECE ??????????\n" +
                "%s\n";
        List<String> ids = userService.listObjs(new LambdaQueryWrapper<OciUser>()
                .isNotNull(OciUser::getId)
                .select(OciUser::getId), String::valueOf);

        CompletableFuture<List<String>> fails = CompletableFuture.supplyAsync(() -> {
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            return ids.parallelStream().filter(id -> {
                SysUserDTO ociUser = this.getOciUser(id);
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(ociUser)) {
                    fetcher.getAvailabilityDomains();
                } catch (Exception e) {
                    return true;
                }
                return false;
            }).map(id -> this.getOciUser(id).getUsername()).collect(Collectors.toList());
        });

        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            List<OciCreateTask> ociCreateTaskList = createTaskService.list();
            if (ociCreateTaskList.isEmpty()) {
                return "?";
            }
            String template = "[%s] [%s] [%s] [%s?/%sGB/%sGB] [%s?] [%s] [%s?]";
            return ociCreateTaskList.parallelStream().map(x -> {
                OciUser ociUser = userService.getById(x.getUserId());
                Long counts = (Long) TEMP_MAP.get(CommonUtils.CREATE_COUNTS_PREFIX + x.getId());
                return String.format(template, ociUser.getUsername(), ociUser.getOciRegion(), x.getArchitecture(),
                        x.getOcpus().longValue(), x.getMemory().longValue(), x.getDisk(), x.getCreateNumbers(),
                        CommonUtils.getTimeDifference(x.getCreateTime()), counts == null ? "0" : counts);
            }).collect(Collectors.joining("\n"));
        });

        CompletableFuture.allOf(fails, task).join();

        this.sendMessage(String.format(message,
                LocalDateTime.now().format(CommonUtils.DATETIME_FMT_NORM),
                CollectionUtil.isEmpty(ids) ? 0 : ids.size(),
                fails.join().size(),
                String.join("\n- ", fails.join()),
                task.join()
        ));
    }

    private void startTgBot(String botToken, String chatId) {
        if (StrUtil.isBlank(botToken) || StrUtil.isBlank(chatId)) {
            if (null != botsApplication && botsApplication.isRunning()) {
                try {
                    botsApplication.close();
                } catch (Exception e) {
                    log.error("TG Bot Application close error", e);
                }
            }
            return; // Early return when parameters are blank
        }
        virtualExecutor.execute(() -> {
            if (StrUtil.isNotBlank(botToken) && StrUtil.isNotBlank(chatId)) {
                if (null != botsApplication && botsApplication.isRunning()) {
                    try {
                        botsApplication.close();
                    } catch (Exception e) {
                        log.error("TG Bot Application close error", e);
                    }
                }
                botsApplication = new TelegramBotsLongPollingApplication();
                try {
                    botsApplication.registerBot(botToken, new TgBot(botToken, chatId));
                    log.info("TG Bot started successfully with chatId: {}", chatId);
                    // Virtual thread continues to run, no need to join()
                } catch (Exception e) {
                    log.error("Failed to start TG Bot", e);
                }
            }
        });
    }

    @Override
    public String getAlertEmail() {
        OciKv config = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, "sys-alert-email"));
        return config != null ? config.getValue() : "";
    }

    @Override
    public void updateAlertEmail(String email) {
        OciKv config = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, "sys-alert-email"));
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
    }
}
