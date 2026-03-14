package com.tony.kingdetective.telegram.handler.impl;

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
import java.util.List;

/**
 * Delete OCI API configuration handler
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
class DeleteApiConfigHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
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
            
            return buildEditMessage(
                    callbackQuery,
                    String.format(
                            "?? ????API???\n\n" +
                            "??: %s\n" +
                            "??: %s\n" +
                            "??ID: ...%s\n\n" +
                            "????:\n" +
                            "? ??OCI API??\n" +
                            "? ????????\n" +
                            "? ????????",
                            user.getUsername(),
                            user.getOciRegion(),  
                            user.getTenantId().substring(Math.max(0, user.getTenantId().length() - 8))
                    ),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("? ????", "confirm_delete_api:" + accountId),
                                    KeyboardBuilder.button("? ??", "account_detail:" + accountId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to show delete API confirmation", e);
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
        return "delete_api_config:";
    }
}

/**
 * Confirm delete API config handler
 */
@Slf4j
@Component
class ConfirmDeleteApiHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String accountId = callbackQuery.getData().split(":")[1];
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        try {
            OciUser user = userService.getById(accountId);
            String username = user.getUsername();
            
            // Delete from database
            userService.removeById(accountId);
            
            return buildEditMessage(
                    callbackQuery,
                    String.format(
                            "? API?????\n\n" +
                            "??: %s\n\n" +
                            "???:\n" +
                            "? OCI API??\n" +
                            "? ????\n" +
                            "? ????\n" +
                            "? ????\n\n" +
                            "? ????????????????",
                            username
                    ),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ????", "account_management")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to delete API config", e);
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
        return "confirm_delete_api:";
    }
}
