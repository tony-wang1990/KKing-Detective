package com.tony.kingdetective.telegram.handler.impl;

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
 * Account add handler
 * Guide user to use Web UI for adding accounts
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AccountAddHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String message = "?? *Telegram Bot ??????????*\n\n" +
                "?? OCI ??????????????????????? Web ??????\n\n" +
                "? *Web ???*: ????????????\n" +
                "(???????? '????')\n\n" +
                "? *??*: ????????? Bot ??????????";
        
        return buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("? Bot ????", "account_add_bot")
                        ),
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("?? ??????", "account_management")
                        ),
                        KeyboardBuilder.buildBackToMainMenuRow(),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "account_add";
    }
}
