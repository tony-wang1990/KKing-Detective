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
 * ?
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
            return buildEditMessage(callbackQuery, "????????");
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "version_info";
    }
}

/**
 * ?
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
            // Java NIOtrigger?
            java.io.File triggerFile = new java.io.File("/app/king-detective/update_version_trigger.flag");
            
            // 
            if (triggerFile.exists() && triggerFile.isDirectory()) {
                log.warn("Trigger????????????????...");
                org.apache.commons.io.FileUtils.deleteDirectory(triggerFile);
            }
            
            // ?
            java.io.File parentDir = triggerFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // trigger
            java.nio.file.Files.write(
                triggerFile.toPath(), 
                "trigger".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
            );
            
            log.info("???????????? {}", triggerFile.getAbsolutePath());
            
            // ?
            telegramClient.execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(toIntExact(messageId))
                    .build());
            
            // ?
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("? ???? king-detective ?????????..\n\n" +
                          "? ??????-3??\n" +
                          "? ????????????\n" +
                          "<code>docker logs -f king-detective-watcher</code>")
                    .parseMode("HTML")
                    .build();
                    
        } catch (java.io.IOException e) {
            log.error("??trigger????", e);
            try {
                telegramClient.execute(DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(toIntExact(messageId))
                        .build());
            } catch (TelegramApiException ex) {
                log.error("??????", ex);
            }
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("????????: " + e.getMessage() + "\n\n????????????")
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
