package com.tony.kingdetective.telegram.handler.impl;

import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;


import java.io.Serializable;

import static java.lang.Math.toIntExact;

/**
 * 通用回调处理�?
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class CancelHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        return DeleteMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(toIntExact(callbackQuery.getMessage().getMessageId()))
                .build();
    }
    
    @Override
    public String getCallbackPattern() {
        return "cancel";
    }
}

/**
 * 返回主菜单处理器
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
class BackToMainHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        return buildEditMessage(
                callbackQuery,
                "请选择需要执行的操作�?,
                new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "back_to_main";
    }
}
