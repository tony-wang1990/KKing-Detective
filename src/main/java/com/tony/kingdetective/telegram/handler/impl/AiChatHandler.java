package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.ChatSessionStorage;
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
 * AI Chat Handler
 * Handles AI chat menu and settings
 * 
 * @author yohann
 */
@Slf4j
@Component
public class AiChatHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        ChatSessionStorage storage = ChatSessionStorage.getInstance();
        
        // Get current settings
        String currentModel = storage.getModel(chatId);
        boolean internetEnabled = storage.isInternetEnabled(chatId);
        int historyCount = storage.getHistory(chatId).size();
        
        String text = String.format(
            "? *AI ????*\n\n" +
            "? ?????\n" +
            "? ??: %s\n" +
            "? ????: %s\n" +
            "? ?????: %d\n\n" +
            "? ?????\n" +
            "????????????? AI ??\n" +
            "AI ????? 10 ?????\n\n" +
            "?? ??????",
            getModelDisplayName(currentModel),
            internetEnabled ? "? ???" : "? ???",
            historyCount
        );
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Model selection row
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("? ????", "ai_select_model")
        ));
        
        // Internet search toggle row
        String internetButtonText = internetEnabled ? "? ??????" : "? ??????";
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button(internetButtonText, "ai_toggle_internet")
        ));
        
        // Clear history row
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?? ??????", "ai_clear_history")
        ));
        
        // Navigation
        keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "ai_chat";
    }
    
    /**
     * Get model display name
     */
    private String getModelDisplayName(String model) {
        if (model.contains("DeepSeek-R1")) {
            return "DeepSeek-R1 (????)";
        } else if (model.contains("DeepSeek-V3")) {
            return "DeepSeek-V3 (????)";
        } else if (model.contains("Qwen")) {
            return "Qwen (????)";
        }
        return model;
    }
}

/**
 * Model Selection Handler
 */
@Slf4j
@Component
class AiModelSelectionHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String text = "? *?? AI ??*\n\n" +
                     "??????? AI ???\n\n" +
                     "? ??????????\n" +
                     "? DeepSeek-R1: ????????????\n" +
                     "? DeepSeek-V3: ???????????\n" +
                     "? Qwen: ??????????";
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("? DeepSeek-R1", "ai_set_model_deepseek_r1")
        ));
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("? DeepSeek-V3", "ai_set_model_deepseek_v3")
        ));
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("? Qwen-2.5", "ai_set_model_qwen")
        ));
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?? ??", "ai_chat")
        ));
        
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "ai_select_model";
    }
}

/**
 * Set Model Handler
 */
@Slf4j
@Component
class AiSetModelHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        String callbackData = callbackQuery.getData();
        
        ChatSessionStorage storage = ChatSessionStorage.getInstance();
        String modelName;
        String displayName;
        
        if (callbackData.contains("deepseek_r1")) {
            modelName = "deepseek-ai/DeepSeek-R1-Distill-Qwen-7B";
            displayName = "DeepSeek-R1";
        } else if (callbackData.contains("deepseek_v3")) {
            modelName = "deepseek-ai/DeepSeek-V3";
            displayName = "DeepSeek-V3";
        } else if (callbackData.contains("qwen")) {
            modelName = "Qwen/Qwen2.5-7B-Instruct";
            displayName = "Qwen-2.5";
        } else {
            modelName = "deepseek-ai/DeepSeek-R1-Distill-Qwen-7B";
            displayName = "DeepSeek-R1";
        }
        
        storage.setModel(chatId, modelName);
        log.info("AI model changed: chatId={}, model={}", chatId, modelName);
        
        String text = String.format("? ???? *%s* ??\n\n????????", displayName);
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?? ?? AI ??", "ai_chat")
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "ai_set_model";
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && callbackData.startsWith("ai_set_model_");
    }
}

/**
 * Toggle Internet Search Handler
 */
@Slf4j
@Component
class AiToggleInternetHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        ChatSessionStorage storage = ChatSessionStorage.getInstance();
        
        boolean currentStatus = storage.isInternetEnabled(chatId);
        storage.setInternetEnabled(chatId, !currentStatus);
        
        String statusText = !currentStatus ? "???" : "???";
        log.info("AI internet search toggled: chatId={}, enabled={}", chatId, !currentStatus);
        
        String text = String.format("? ????%s\n\n%s", 
            statusText,
            !currentStatus ? "AI ??????????????" : "AI ????????????"
        );
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?? ?? AI ??", "ai_chat")
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "ai_toggle_internet";
    }
}

/**
 * Clear History Handler
 */
@Slf4j
@Component
class AiClearHistoryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        ChatSessionStorage storage = ChatSessionStorage.getInstance();
        
        storage.clearHistory(chatId);
        log.info("AI chat history cleared: chatId={}", chatId);
        
        String text = "? ???????\n\n??????????";
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?? ?? AI ??", "ai_chat")
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "ai_clear_history";
    }
}
