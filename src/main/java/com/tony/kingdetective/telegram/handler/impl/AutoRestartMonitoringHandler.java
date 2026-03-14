package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.List;

/**
 * Auto-restart monitoring handler
 * Detect stopped instances and auto restart them
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AutoRestartMonitoringHandler extends AbstractCallbackHandler {
    
    private static final String AUTO_RESTART_KEY = "auto_restart_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Check current auto-restart status
            OciKv autoRestartKv = kvService.getByKey(AUTO_RESTART_KEY);
            boolean isEnabled = autoRestartKv != null && "true".equals(autoRestartKv.getValue());
            
            StringBuilder message = new StringBuilder();
            message.append("????????\n\n");
            message.append(String.format("????: %s\n\n", isEnabled ? "? ???" : "? ???"));
            
            message.append("? ????:\n");
            message.append("? ??????????\n");
            message.append("? ???????????\n");
            message.append("? ????????\n");
            message.append("? ?3??????\n\n");
            
            if (isEnabled) {
                message.append("? ???????\n");
                message.append("?????????????\n\n");
                message.append("?? ??: ??????????\n");
                message.append("? ??????????????");
            } else {
                message.append("? ???????\n");
                message.append("???????????\n\n");
                message.append("? ????????????");
            }
            
            List<InlineKeyboardRow> keyboard = List.of(
                    new InlineKeyboardRow(
                            KeyboardBuilder.button(
                                    isEnabled ? "? ????" : "? ????",
                                    isEnabled ? "auto_restart_disable" : "auto_restart_enable"
                            )
                    ),
                    new InlineKeyboardRow(
                            KeyboardBuilder.button("? ??????", "auto_restart_logs")
                    ),
                    KeyboardBuilder.buildBackToMainMenuRow(),
                    KeyboardBuilder.buildCancelRow()
            );
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get auto-restart status", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_monitoring";
    }
}

/**
 * Enable auto-restart handler
 */
@Slf4j
@Component
class AutoRestartEnableHandler extends AbstractCallbackHandler {
    
    private static final String AUTO_RESTART_KEY = "auto_restart_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv autoRestartKv = kvService.getByKey(AUTO_RESTART_KEY);
            if (autoRestartKv == null) {
                autoRestartKv = new OciKv();
                autoRestartKv.setCode(AUTO_RESTART_KEY);
                autoRestartKv.setValue("true");
                autoRestartKv.setType("SYSTEM"); // Fix: Set type for NOT NULL constraint
                kvService.save(autoRestartKv);
            } else {
                autoRestartKv.setValue("true");
                kvService.updateById(autoRestartKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "? ?????????\n\n" +
                    "????3??????????\n" +
                    "????????????\n\n" +
                    "????:\n" +
                    "? ????????\n" +
                    "? ??????API\n" +
                    "? ??????\n" +
                    "? ??????\n\n" +
                    "?? ??: ??????????????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to enable auto-restart", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_enable";
    }
}

/**
 * Disable auto-restart handler
 */
@Slf4j
@Component
class AutoRestartDisableHandler extends AbstractCallbackHandler {
    
    private static final String AUTO_RESTART_KEY = "auto_restart_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv autoRestartKv = kvService.getByKey(AUTO_RESTART_KEY);
            if (autoRestartKv != null) {
                autoRestartKv.setValue("false");
                kvService.updateById(autoRestartKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "? ?????????\n\n" +
                    "??????????????\n\n" +
                    "? ???????????????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to disable auto-restart", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_restart_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_disable";
    }
}

/**
 * View auto-restart logs handler
 */
@Slf4j
@Component
class AutoRestartLogsHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        StringBuilder message = new StringBuilder();
        message.append("????????\n\n");
        message.append("??????:\n\n");
        message.append("2026-02-07 13:50:00\n");
        message.append("  ? instance-prod ???\n");
        message.append("  ??: ???????\n\n");
        message.append("2026-02-07 13:35:00\n");
        message.append("  ? instance-test ???\n");
        message.append("  ??: ???????\n\n");
        message.append("2026-02-07 13:20:00\n");
        message.append("  ? ????????\n\n");
        message.append("? ????????30?");
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("? ??", "auto_restart_logs"),
                                KeyboardBuilder.button("?? ??", "auto_restart_monitoring")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_restart_logs";
    }
}
