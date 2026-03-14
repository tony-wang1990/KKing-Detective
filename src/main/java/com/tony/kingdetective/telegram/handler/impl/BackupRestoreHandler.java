package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.params.sys.BackupParams;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Backup and Restore Handler
 * Handles system data backup and restore operations
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class BackupRestoreHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String text = "? *??????\n\n" +
                "? ?????\n" +
                "??????????????\n" +
                "??????????????\n\n" +
                "? ???????\n" +
                "??OCI ????\n" +
                "??????\n" +
                "??????\n" +
                "????????\n\n" +
                "? ?????\n" +
                "????????????\n" +
                "??????????\n\n" +
                "?? ???\n" +
                "?????????????\n" +
                "????????????\n" +
                "???????????\n\n" +
                "Backup Execute";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("? ????", "backup_create")
        ));

        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("? ????", "restore_data")
        ));

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
        return "backup_restore";
    }
}

/**
 * Backup Create Handler
 * Initiates backup creation process
 */
@Component
class BackupCreateHandler extends AbstractCallbackHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BackupCreateHandler.class);

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String text = "? *????*\n\n" +
                "????????\n\n" +
                "? **?????*\n" +
                "??????????\n" +
                "??????????\n" +
                "????????\n\n" +
                "? **????????**\n" +
                "????????\n" +
                "????????\n" +
                "????????\n\n" +
                "?? ???\n" +
                "Please confirm backup";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("Execute Backup", "backup_execute_plain"),
                KeyboardBuilder.button("? ????", "backup_execute_encrypted")
        ));

        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "backup_restore")
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
        return "backup_create";
    }
}

/**
 * Backup Execute Plain Handler
 * Executes plain (unencrypted) backup and sends file via TG
 */
@Component
class BackupExecutePlainHandler extends AbstractCallbackHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BackupExecutePlainHandler.class);

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();

        try {
            // Send processing message
            telegramClient.execute(buildEditMessage(
                    callbackQuery,
                    "Processing backup...\n\n",
                    null
            ));

            // Execute backup using the new method
            ISysService sysService = SpringUtil.getBean(ISysService.class);
            BackupParams params = new BackupParams();
            params.setEnableEnc(false);
            params.setPassword(""); // null

            String backupFilePath = sysService.createBackupFile(params);

            log.info("Plain backup created for chatId: {}, file: {}", chatId, backupFilePath);

            // Send backup file via Telegram
            java.io.File backupFile = new java.io.File(backupFilePath);
            if (backupFile.exists()) {
                org.telegram.telegrambots.meta.api.methods.send.SendDocument sendDocument = 
                    org.telegram.telegrambots.meta.api.methods.send.SendDocument.builder()
                        .chatId(chatId)
                        .document(new org.telegram.telegrambots.meta.api.objects.InputFile(backupFile))
                        .caption(
                            "? *????*\n\n" +
                            "????????????????\n" +
                            "? ?????" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                            "? ???\n" +
                            "??????????\n" +
                            "????????????\n" +
                            "???????????\n\n" +
                            "?? ???\n" +
                            "???????????\n" +
                            "?????????????"
                        )
                        .parseMode("Markdown")
                        .build();
                
                try {
                    telegramClient.execute(sendDocument);
                    log.info("Backup file sent to chatId: {}", chatId);
                    
                    // Delete backup file from server after sending
                    cn.hutool.core.io.FileUtil.del(backupFile);
                    log.info("Backup file deleted from server: {}", backupFilePath);
                    
                } catch (Exception e) {
                    log.error("Failed to send backup file", e);
                    throw new Exception("?????????" + e.getMessage());
                }
            } else {
                throw new Exception("????????" + backupFilePath);
            }

            String text = "??*??????*\n\n" +
                    "?????????????\n\n" +
                    "? ???\n" +
                    "??????????????\n" +
                    "?????????????\n" +
                    "?????????????";

            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("?????", "backup_restore")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());

            // Send success message
            return buildEditMessage(
                    callbackQuery,
                    text,
                    new InlineKeyboardMarkup(keyboard)
            );

        } catch (Exception e) {
            log.error("Failed to create plain backup", e);

            String text = "??*??????*\n\n" +
                    "Error: " + e.getMessage() + "\n\n" +
                    "Processing restore...";

            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("?????", "backup_restore")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());

            return buildEditMessage(
                    callbackQuery,
                    text,
                    new InlineKeyboardMarkup(keyboard)
            );
        }
    }

    @Override
    public String getCallbackPattern() {
        return "backup_execute_plain";
    }
}

/**
 * Backup Execute Encrypted Handler
 * Prompts for password and executes encrypted backup
 */
@Component
class BackupExecuteEncryptedHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();

        // Mark session as waiting for backup password
        ConfigSessionStorage.getInstance().startBackupPassword(chatId);

        String text = "? *????*\n\n" +
                "??????????\n\n" +
                "? ?????\n" +
                "?????????\n" +
                "???? 8 ???\n" +
                "?????????\n\n" +
                "?? ?????\n" +
                "????????\n" +
                "?????????????\n" +
                "?????????????\n\n" +
                "? ???\n" +
                "/cancel";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "backup_restore")
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
        return "backup_execute_encrypted";
    }
}

/**
 * Restore Data Handler
 * Provides instructions for data restoration via TG upload
 */
@Component
class RestoreDataHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String text = "? *????*\n\n" +
                "?? **????**\n" +
                "??????????????\n" +
                "???????????\n\n" +
                "? ?????\n" +
                "1?? ????????????\n" +
                "2?? ???? ZIP ??\n" +
                "3?? ????????????\n" +
                "4?? ????????\n\n" +
                "? ???\n" +
                "???????????????\n" +
                "????????ZIP ??\n" +
                "?????????????\n" +
                "?????????????\n\n" +
                "?? ???\n" +
                "??????????????\n" +
                "??????";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("Start Restore", "restore_start")
        ));

        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "backup_restore")
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
        return "restore_data";
    }
}

/**
 * Restore Start Handler
 * Prompts user to upload backup file
 */
@Component
class RestoreStartHandler extends AbstractCallbackHandler {

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();

        // 
        ConfigSessionStorage.getInstance().startRestorePassword(chatId,
                String.valueOf(callbackQuery.getMessage().getMessageId()));

        String text = "? *?????\n\n" +
                "??????ZIP ???\n\n" +
                "? ?????\n" +
                "??????ZIP ???????\n" +
                "???????????????\n" +
                "????????????????\n\n" +
                "? ???\n" +
                "/cancel";

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "restore_data")
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
        return "restore_start";
    }
}
