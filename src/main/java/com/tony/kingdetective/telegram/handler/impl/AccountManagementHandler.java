package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IOciUserService;
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
 * Account management handler - manage OCI accounts (CRUD)
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AccountManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            List<OciUser> users = userService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("??????\n\n");
            
            if (CollectionUtil.isEmpty(users)) {
                message.append("??OCI??\n\n");
                message.append("? ??????");
                
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("? ????", "account_add")
                                ),
                                KeyboardBuilder.buildBackToMainMenuRow(),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            message.append(String.format("? %d ???\n\n", users.size()));
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            for (int i = 0; i < users.size(); i++) {
                OciUser user = users.get(i);
                String status = (user.getDeleted() != null && user.getDeleted() == 1) ? "? ???" : "? ??";
                
                message.append(String.format(
                        "%d. %s\n" +
                        "   ??: %s\n" +
                        "   ??: %s\n" +
                        "   ??ID: ...%s\n\n",
                        i + 1,
                        user.getUsername(),
                        status,
                        user.getOciRegion(),
                        user.getTenantId().substring(Math.max(0, user.getTenantId().length() - 8))
                ));
                
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("?? ??%d", i + 1),
                        "account_detail:" + user.getId()
                ));
                keyboard.add(row);
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? ?????", "account_add"),
                    KeyboardBuilder.button("? ????", "account_management")
            ));
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to list accounts", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_management";
    }
}

/**
 * Account detail handler
 */
@Slf4j
@Component
class AccountDetailHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String accountId = callbackData.split(":")[1];
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            
            if (user == null) {
                return buildEditMessage(
                        callbackQuery,
                        "? ?????",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "account_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("??????\n\n");
            message.append(String.format("???: %s\n", user.getUsername()));
            message.append(String.format("??: %s\n", (user.getDeleted() != null && user.getDeleted() == 1) ? "? ???" : "? ??"));
            message.append(String.format("???: %s\n", user.getOciRegion()));
            message.append(String.format("??ID: %s\n", user.getTenantId()));
            message.append(String.format("??ID: %s\n", user.getUserId()));
            message.append(String.format("??: %s\n", user.getFingerprint()));
            message.append(String.format("????: %s\n", user.getCreateTime()));
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            // Enable/Disable button
            if (user.getDeleted() != null && user.getDeleted() == 1) {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("? ????", "account_enable:" + accountId)
                ));
            } else {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("? ????", "account_disable:" + accountId)
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? ??API??", "delete_api_config:" + accountId)
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? ????", "account_delete_confirm:" + accountId)
            ));
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("?? ??", "account_management")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get account detail", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_detail:";
    }
}

/**
 * Enable account handler
 */
@Slf4j
@Component
class AccountEnableHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            if (user != null) {
                user.setDeleted(0);
                userService.updateById(user);
                
                return buildEditMessage(
                        callbackQuery,
                        String.format("? ?? %s ???", user.getUsername()),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ????", "account_detail:" + accountId)
                                ),
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ????", "account_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
        } catch (Exception e) {
            log.error("Failed to enable account", e);
        }
        
        return buildEditMessage(
                callbackQuery,
                "? ????",
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("?? ??", "account_management")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_enable:";
    }
}

/**
 * Disable account handler
 */
@Slf4j
@Component
class AccountDisableHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            if (user != null) {
                user.setDeleted(1);
                userService.updateById(user);
                
                return buildEditMessage(
                        callbackQuery,
                        String.format("? ?? %s ???", user.getUsername()),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ????", "account_detail:" + accountId)
                                ),
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ????", "account_management")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
        } catch (Exception e) {
            log.error("Failed to disable account", e);
        }
        
        return buildEditMessage(
                callbackQuery,
                "? ????",
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("?? ??", "account_management")
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_disable:";
    }
}

/**
 * Delete account confirmation handler
 */
@Slf4j
@Component
class AccountDeleteConfirmHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            
            return buildEditMessage(
                    callbackQuery,
                    String.format(
                            "?? ???????\n\n" +
                            "??: %s\n" +
                            "??: %s\n\n" +
                            "????????",
                            user.getUsername(),
                            user.getOciRegion()
                    ),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("? ????", "account_delete:" + accountId),
                                    KeyboardBuilder.button("? ??", "account_detail:" + accountId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to show delete confirmation", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_delete_confirm:";
    }
}

/**
 * Delete account handler
 */
@Slf4j
@Component
class AccountDeleteHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            String username = user.getUsername();
            
            userService.removeById(accountId);
            
            return buildEditMessage(
                    callbackQuery,
                    String.format("? ?? %s ???", username),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ????", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to delete account", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_delete:";
    }
}
