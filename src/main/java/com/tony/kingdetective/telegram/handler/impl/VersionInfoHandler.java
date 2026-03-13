package com.tony.kingdetective.telegram.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;

import static java.lang.Math.toIntExact;

/**
 * 版本信息回调处理�?
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class VersionInfoHandler extends VersionInfoBaseHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        try {
            return getVersionInfo(
                    callbackQuery.getMessage().getChatId(),
                    callbackQuery.getMessage().getMessageId(),
                    telegramClient
            );
        } catch (Exception e) {
            log.error("Handle version info error", e);
            return buildEditMessage(callbackQuery, "获取版本信息失败");
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "version_info";
    }
}

/**
 * 更新系统版本回调处理�?
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
class UpdateSysVersionHandler extends VersionInfoBaseHandler {
    
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        long messageId = callbackQuery.getMessage().getMessageId();
        
        try {
            // 使用Java NIO创建trigger文件，更加可�?
            java.io.File triggerFile = new java.io.File("/app/king-detective/update_version_trigger.flag");
            
            // 如果文件存在且是目录，删除该目录
            if (triggerFile.exists() && triggerFile.isDirectory()) {
                log.warn("Trigger文件被错误地创建为目录，正在修复...");
                org.apache.commons.io.FileUtils.deleteDirectory(triggerFile);
            }
            
            // 确保父目录存�?
            java.io.File parentDir = triggerFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 写入trigger内容
            java.nio.file.Files.write(
                triggerFile.toPath(), 
                "trigger".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
            );
            
            log.info("�?成功创建更新触发�? {}", triggerFile.getAbsolutePath());
            
            // 删除原消�?
            telegramClient.execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(toIntExact(messageId))
                    .build());
            
            // 发送更新提�?
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("🔄 正在更新 king-detective 最新版本，请稍�?..\n\n" +
                          "💡 更新过程�?-3分钟\n" +
                          "📋 可通过以下命令查看进度：\n" +
                          "<code>docker logs -f king-detective-watcher</code>")
                    .parseMode("HTML")
                    .build();
                    
        } catch (java.io.IOException e) {
            log.error("创建trigger文件失败", e);
            try {
                telegramClient.execute(DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(toIntExact(messageId))
                        .build());
            } catch (TelegramApiException ex) {
                log.error("删除消息失败", ex);
            }
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("�?触发更新失败: " + e.getMessage() + "\n\n请检查容器权限或手动更新")
                    .build();
        } catch (TelegramApiException e) {
            log.error("TG Bot error", e);
            return null;
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "update_sys_version";
    }
}
