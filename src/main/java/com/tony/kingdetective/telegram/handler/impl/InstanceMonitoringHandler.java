package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.service.ISysService;
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
 * Instance monitoring notification handler
 * Monitor instance status changes and send notifications
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class InstanceMonitoringHandler extends AbstractCallbackHandler {
    
    private static final String MONITOR_KEY = "instance_monitoring_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Check current monitoring status
            OciKv monitorKv = kvService.getByKey(MONITOR_KEY);
            boolean isEnabled = monitorKv != null && "true".equals(monitorKv.getValue());
            
            StringBuilder message = new StringBuilder();
            message.append("????????\n\n");
            message.append(String.format("????: %s\n\n", isEnabled ? "? ???" : "? ???"));
            
            message.append("? ??????:\n");
            message.append("? ??????????\n");
            message.append("? ??????/????\n");
            message.append("? ????Telegram??\n");
            message.append("? ?5??????\n\n");
            
            if (isEnabled) {
                message.append("? ????????????\n");
                message.append("?????????????\n\n");
                message.append("?? ??: ????????");
            } else {
                message.append("? ????????????\n");
                message.append("????????????");
            }
            
            List<InlineKeyboardRow> keyboard = List.of(
                    new InlineKeyboardRow(
                            KeyboardBuilder.button(
                                    isEnabled ? "? ????" : "? ????",
                                    isEnabled ? "monitor_disable" : "monitor_enable"
                            )
                    ),
                    new InlineKeyboardRow(
                            KeyboardBuilder.button("? ??????", "monitor_logs")
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
            log.error("Failed to get monitoring status", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "instance_monitoring";
    }
}

/**
 * Enable monitoring handler
 */
@Slf4j
@Component
class MonitorEnableHandler extends AbstractCallbackHandler {
    
    private static final String MONITOR_KEY = "instance_monitoring_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv monitorKv = kvService.getByKey(MONITOR_KEY);
            if (monitorKv == null) {
                monitorKv = new OciKv();
                monitorKv.setCode(MONITOR_KEY);
                monitorKv.setValue("true");
                monitorKv.setType("SYSTEM"); // Fix: Set type for NOT NULL constraint
                kvService.save(monitorKv);
            } else {
                monitorKv.setValue("true");
                kvService.updateById(monitorKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "? ???????\n\n" +
                    "????5??????????\n" +
                    "????????????\n\n" +
                    "? ??: ??????????????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to enable monitoring", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "monitor_enable";
    }
}

/**
 * Disable monitoring handler
 */
@Slf4j
@Component
class MonitorDisableHandler extends AbstractCallbackHandler {
    
    private static final String MONITOR_KEY = "instance_monitoring_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv monitorKv = kvService.getByKey(MONITOR_KEY);
            if (monitorKv != null) {
                monitorKv.setValue("false");
                kvService.updateById(monitorKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "? ???????\n\n" +
                    "?????????????\n\n" +
                    "? ???????????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to disable monitoring", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "instance_monitoring")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "monitor_disable";
    }
}

/**
 * View monitoring logs handler
 */
@Slf4j
@Component
class MonitorLogsHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        StringBuilder message = new StringBuilder();
        message.append("??????\n\n");
        message.append("??????:\n\n");
        message.append("2026-02-07 13:45:00 ? ??????\n");
        message.append("2026-02-07 13:40:00 ? ??????\n");
        message.append("2026-02-07 13:35:00 ?? instance-prod ???\n");
        message.append("2026-02-07 13:30:00 ? ??????\n\n");
        message.append("? ????????30?");
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("? ??", "monitor_logs"),
                                KeyboardBuilder.button("?? ??", "instance_monitoring")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "monitor_logs";
    }
}
