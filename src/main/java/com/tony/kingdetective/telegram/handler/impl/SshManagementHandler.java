package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.service.SshService;
import com.tony.kingdetective.telegram.storage.SshConnectionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH Management Handler
 * Handles SSH connection menu and operations
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class SshManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        SshConnectionStorage storage = SshConnectionStorage.getInstance();
        
        boolean hasConnection = storage.hasConnection(chatId);
        
        String text;
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        if (hasConnection) {
            SshConnectionStorage.SshInfo info = storage.getConnection(chatId);
            text = String.format(
                " *SSH ????*\n\n" +
                " ?????\n" +
                "????: %s:%d\n" +
                "????: %s\n" +
                "????? ?????\n\n" +
                " ?????\n" +
                "???/ssh [??] ????SSH ??\n" +
                "??: /ssh ls -la\n\n" +
                "?? ??????",
                info.getHost(),
                info.getPort(),
                info.getUsername()
            );
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(" ????", "ssh_setup")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(" ????", "ssh_test")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("???????", "ssh_disconnect")
            ));
        } else {
            text = " *SSH ????*\n\n" +
                   " ?????? SSH ??\n\n" +
                   " ?????\n" +
                   "???????? SSH ????\n" +
                   "?????host port username password\n" +
                   "??: 192.168.1.100 22 root mypassword\n\n" +
                   "?? ??????";
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("??????", "ssh_setup")
            ));
        }
        
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
        return "ssh_management";
    }
}

/**
 * SSH Setup Handler
 */
@Slf4j
@Component
class SshSetupHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        String text = " *?? SSH ??*\n\n" +
                     "?????????????\n\n" +
                     "/ssh_config host port username password\n\n" +
                     " ???\n" +
                     "/ssh_config 192.168.1.100 22 root mypassword\n\n" +
                     "?? ???\n" +
                     "???????????\n" +
                     "???????? 22\n" +
                     "????????????????";
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?????", "ssh_management")
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
        return "ssh_setup";
    }
}

/**
 * SSH Test Connection Handler
 */
@Slf4j
@Component
class SshTestHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        SshConnectionStorage storage = SshConnectionStorage.getInstance();
        
        if (!storage.hasConnection(chatId)) {
            return buildEditMessage(
                callbackQuery,
                "??????SSH ??\n\n????????",
                new InlineKeyboardMarkup(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.button("?????", "ssh_management"))
                ))
            );
        }
        
        // Send testing message first
        try {
            telegramClient.execute(buildEditMessage(
                callbackQuery,
                " ??????...",
                null
            ));
        } catch (TelegramApiException e) {
            log.error("Failed to send testing message", e);
        }
        
        SshConnectionStorage.SshInfo info = storage.getConnection(chatId);
        SshService sshService = SpringUtil.getBean(SshService.class);
        
        boolean success = sshService.testConnection(
            info.getHost(),
            info.getPort(),
            info.getUsername(),
            info.getPassword()
        );
        
        String text;
        if (success) {
            text = String.format(
                "??*??????*\n\n" +
                "??: %s:%d\n" +
                "??: %s\n\n" +
                "SSH ?????????????",
                info.getHost(),
                info.getPort(),
                info.getUsername()
            );
        } else {
            text = String.format(
                "??*??????*\n\n" +
                "??: %s:%d\n" +
                "??: %s\n\n" +
                "????\n" +
                "?????????????\n" +
                "????????????\n" +
                "??????????\n" +
                "?"SSH ?,
                info.getHost(),
                info.getPort(),
                info.getUsername()
            );
        }
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?????", "ssh_management")
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        // Send result message
        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(new InlineKeyboardMarkup(keyboard))
                .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send test result", e);
        }
        
        return null;
    }
    
    @Override
    public String getCallbackPattern() {
        return "ssh_test";
    }
}

/**
 * SSH Disconnect Handler
 */
@Slf4j
@Component
class SshDisconnectHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        SshConnectionStorage storage = SshConnectionStorage.getInstance();
        
        storage.removeConnection(chatId);
        log.info("SSH connection removed: chatId={}", chatId);
        
        String text = "??SSH ???????\n\n???????????";
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?????", "ssh_management")
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
        return "ssh_disconnect";
    }
}
