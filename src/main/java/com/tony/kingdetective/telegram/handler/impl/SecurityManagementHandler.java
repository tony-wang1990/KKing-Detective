package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.IpBlacklist;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.service.IIpBlacklistService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Security Management Handler
 * Main menu for security features
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class SecurityManagementHandler extends AbstractCallbackHandler {
    
    private static final String DEFENSE_MODE_KEY = "defense_mode_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        IIpBlacklistService blacklistService = SpringUtil.getBean(IIpBlacklistService.class);
        
        try {
            // Get defense mode status
            OciKv defenseModeKv = kvService.getByKey(DEFENSE_MODE_KEY);
            boolean isDefenseModeEnabled = defenseModeKv != null && "true".equals(defenseModeKv.getValue());
            
            // Get blacklist count
            long blacklistCount = blacklistService.count();
            
            StringBuilder message = new StringBuilder();
            message.append("?? ?????\n\n");
            
            // Defense Mode Status
            message.append("? ????\n");
            message.append(String.format("??: %s\n", isDefenseModeEnabled ? "? ???" : "? ???"));
            if (isDefenseModeEnabled) {
                message.append("?? ??Web?????\n");
            }
            message.append("\n");
            
            // IP Blacklist Status
            message.append("? IP???\n");
            message.append(String.format("??IP??: %d\n", blacklistCount));
            message.append("\n");
            
            message.append("????????????????\n");
            message.append("? ??TG??Web???\n");
            message.append("? ????????IP");
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? " + (isDefenseModeEnabled ? "??" : "??") + "????", "defense_mode_toggle")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? IP?????", "ip_blacklist_management")
            ));
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to show security management", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "security_management";
    }
}

/**
 * Defense Mode Toggle Handler
 */
@Slf4j
@Component
class DefenseModeToggleHandler extends AbstractCallbackHandler {
    
    private static final String DEFENSE_MODE_KEY = "defense_mode_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv defenseModeKv = kvService.getByKey(DEFENSE_MODE_KEY);
            boolean currentStatus = defenseModeKv != null && "true".equals(defenseModeKv.getValue());
            boolean newStatus = !currentStatus;
            
            if (defenseModeKv == null) {
                defenseModeKv = new OciKv();
                defenseModeKv.setCode(DEFENSE_MODE_KEY);
                defenseModeKv.setValue(String.valueOf(newStatus));
                defenseModeKv.setType("SYSTEM"); // Fix: Set type to satisfy NOT NULL constraint
                kvService.save(defenseModeKv);
            } else {
                defenseModeKv.setValue(String.valueOf(newStatus));
                kvService.updateById(defenseModeKv);
            }
            
            String message;
            if (newStatus) {
                message = "? ???????\n\n" +
                        "?? ????:\n" +
                        "? ??IP????Web?\n" +
                        "? ??????IP\n" +
                        "? ????TG??\n" +
                        "? OCI??????\n\n" +
                        "? Web??????";
            } else {
                message = "? ???????\n\n" +
                        "Web????????\n" +
                        "IP???????";
            }
            
            return buildEditMessage(
                    callbackQuery,
                    message,
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "security_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to toggle defense mode", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "security_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "defense_mode_toggle";
    }
}

/**
 * IP Blacklist Management Handler
 */
@Slf4j
@Component
class IpBlacklistManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IIpBlacklistService blacklistService = SpringUtil.getBean(IIpBlacklistService.class);
        
        try {
            List<IpBlacklist> blacklist = blacklistService.listAll();
            
            StringBuilder message = new StringBuilder();
            message.append("?? IP??????\n\n");
            
            if (blacklist.isEmpty()) {
                message.append("????IP\n\n");
                message.append("? ????5?????");
            } else {
                message.append(String.format("? %d ?IP???:\n\n", blacklist.size()));
                
                for (int i = 0; i < Math.min(blacklist.size(), 10); i++) {
                    IpBlacklist item = blacklist.get(i);
                    message.append(String.format(
                            "%d. %s\n" +
                            "   ??: %s\n" +
                            "   ??: %s\n\n",
                            i + 1,
                            item.getIpAddress(),
                            item.getReason() != null ? item.getReason() : "??",
                            item.getCreateTime()
                    ));
                }
                
                if (blacklist.size() > 10) {
                    message.append(String.format("... ?? %d ?IP\n\n", blacklist.size() - 10));
                }
            }
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            if (!blacklist.isEmpty()) {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("? ?????", "blacklist_clear_confirm")
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("?? ??", "security_management")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to show blacklist", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ???????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "security_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ip_blacklist_management";
    }
}

/**
 * Clear Blacklist Confirmation Handler
 */
@Slf4j
@Component
class BlacklistClearConfirmHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        return buildEditMessage(
                callbackQuery,
                "?? ????????\n\n" +
                "??????????IP\n" +
                "?????",
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("? ????", "blacklist_clear"),
                                KeyboardBuilder.button("? ??", "ip_blacklist_management")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "blacklist_clear_confirm";
    }
}

/**
 * Clear Blacklist Handler
 */
@Slf4j
@Component
class BlacklistClearHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IIpBlacklistService blacklistService = SpringUtil.getBean(IIpBlacklistService.class);
        
        try {
            long count = blacklistService.count();
            blacklistService.clearAll();
            
            return buildEditMessage(
                    callbackQuery,
                    String.format("? ??????\n\n??? %d ?IP", count),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "ip_blacklist_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to clear blacklist", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "ip_blacklist_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "blacklist_clear";
    }
}
