package com.tony.kingdetective.telegram.handler;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;

/**
 * Telegram Bot ?
 * 
 * @author Tony Wang
 */
public interface CallbackHandler {
    
    /**
     * 
     * 
     * @param callbackQuery  Telegram ?
     * @param telegramClient Telegram ?
     * @return  Bot API ?null?
     */
    BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient);
    
    /**
     * ?
     * 
     * @return 
     */
    String getCallbackPattern();
    
    /**
     * ?
     * 
     * @param callbackData 
     * @return ?true
     */
    default boolean canHandle(String callbackData) {
        return callbackData != null && callbackData.startsWith(getCallbackPattern());
    }
}
