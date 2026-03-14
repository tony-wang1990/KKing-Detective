package com.tony.kingdetective.telegram.handler;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import static java.lang.Math.toIntExact;

/**
 * 
 * 
 * @author yohann
 */
public abstract class AbstractCallbackHandler implements CallbackHandler {
    
    /**
     * 
     * 
     * @param callbackQuery 
     * @param text 
     * @param markup 
     * @return 
     */
    protected EditMessageText buildEditMessage(CallbackQuery callbackQuery, String text, InlineKeyboardMarkup markup) {
        return EditMessageText.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(toIntExact(callbackQuery.getMessage().getMessageId()))
                .text(text)
                .parseMode("Markdown")  // Enable Markdown parsing
                .replyMarkup(markup)
                .build();
    }
    
    /**
     * 
     * 
     * @param callbackQuery 
     * @param text 
     * @return 
     */
    protected EditMessageText buildEditMessage(CallbackQuery callbackQuery, String text) {
        return buildEditMessage(callbackQuery, text, null);
    }
}
