package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.InstanceSelectionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.tony.kingdetective.config.VirtualThreadConfig.VIRTUAL_EXECUTOR;

/**
 * Confirm terminate instances handler
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class ConfirmTerminateHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        Set<String> selectedInstances = storage.getSelectedInstances(chatId);
        
        if (selectedInstances.isEmpty()) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("??????????")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        List<InlineKeyboardRow> keyboard = List.of(
                new InlineKeyboardRow(
                        KeyboardBuilder.button("?", "terminate_instances:true")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("????????", "terminate_instances:false")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("?????", "instance_management:" + storage.getConfigContext(chatId))
                ),
                KeyboardBuilder.buildCancelRow()
        );
        
        String message = String.format(
                "????????\n\n" +
                "?? ?????%d ?????????????\n\n" +
                "???????????\n" +
                "?????preserveBootVolume = true\n" +
                "??????preserveBootVolume = false\n\n" +
                "??????????",
                selectedInstances.size()
        );
        
        return buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "confirm_terminate_instances";
    }
}

/**
 * Terminate selected instances handler
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
class TerminateSelectedInstancesHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        boolean preserveBootVolume = Boolean.parseBoolean(callbackData.split(":")[1]);
        
        long chatId = callbackQuery.getMessage().getChatId();
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        Set<String> selectedInstances = storage.getSelectedInstances(chatId);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (selectedInstances.isEmpty()) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("???????")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        if (ociCfgId == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("???????")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        // Answer callback query
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("??????...")
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
        
        // Delete the confirmation message
        try {
            telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(Math.toIntExact(callbackQuery.getMessage().getMessageId()))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
        }
        
        // Send processing message
        String processingMessage = String.format(
                "?????? %d ????..\n\n" +
                "??????%s\n\n" +
                "??????????..",
                selectedInstances.size(),
                preserveBootVolume ? "Yes" : "No"
        );
        
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(processingMessage)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send processing message", e);
        }
        
        // Async terminate instances
        terminateInstancesAsync(ociCfgId, selectedInstances, preserveBootVolume, chatId, telegramClient);
        
        // Clear all data (selection, context, and cache) since we're done
        storage.clearAll(chatId);
        
        return null;
    }
    
    /**
     * Terminate instances asynchronously
     */
    private void terminateInstancesAsync(String ociCfgId, Set<String> instanceIds, 
                                        boolean preserveBootVolume, long chatId, 
                                        TelegramClient telegramClient) {
        
        CompletableFuture.runAsync(() -> {
            ISysService sysService = SpringUtil.getBean(ISysService.class);
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            int successCount = 0;
            int failedCount = 0;
            StringBuilder resultMessage = new StringBuilder();
            
            // Terminate instances in parallel
            List<CompletableFuture<Void>> futures = instanceIds.stream()
                    .map(instanceId -> CompletableFuture.runAsync(() -> {
                        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                            fetcher.terminateInstance(instanceId, preserveBootVolume, preserveBootVolume);
                            log.info("Successfully terminated instance: instanceId={}, preserveBootVolume={}", 
                                    instanceId, preserveBootVolume);
                        } catch (Exception e) {
                            log.error("Failed to terminate instance: instanceId={}", instanceId, e);
                            throw new RuntimeException(e);
                        }
                    }, VIRTUAL_EXECUTOR))
                    .toList();
            
            // Wait for all tasks to complete and count results
            for (int i = 0; i < futures.size(); i++) {
                try {
                    futures.get(i).join();
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    log.error("Instance termination failed", e);
                }
            }
            
            // Build result message
            if (failedCount > 0) {
                resultMessage.append(String.format(
                        "????????????????%d ???\n???? %d ?????????????~\n\n??????%s",
                        successCount, failedCount, preserveBootVolume ? "Yes" : "No"
                ));
            } else {
                resultMessage.append(String.format(
                        "??????????%d ???????????????~\n\n??????%s",
                        successCount, preserveBootVolume ? "Yes" : "No"
                ));
            }
            
            // Send result message
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(resultMessage.toString())
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to send result message: chatId={}", chatId, e);
            }
            
        }, VIRTUAL_EXECUTOR);
    }
    
    @Override
    public String getCallbackPattern() {
        return "terminate_instances:";
    }
}
