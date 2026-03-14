package com.tony.kingdetective.telegram;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.factory.CallbackHandlerFactory;
import com.tony.kingdetective.telegram.handler.CallbackHandler;
import com.tony.kingdetective.telegram.handler.TextSessionDispatcher;
import com.tony.kingdetective.telegram.service.AiChatService;
import com.tony.kingdetective.telegram.service.SshService;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
import com.tony.kingdetective.telegram.storage.SshConnectionStorage;
import com.tony.kingdetective.telegram.utils.MarkdownFormatter;
import com.tony.kingdetective.telegram.utils.TgMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Telegram Bot 
 *
 * 
 * - TgBot 
 * - Session / {@link TextSessionDispatcher}
 * -  {@link CallbackHandlerFactory} +  Handler 
 * -  {@link TgMessageSender}
 *
 *  Java 21 Virtual Threads
 *
 * @author Tony Wang
 */
@Slf4j
public class TgBot implements LongPollingSingleThreadUpdateConsumer {

    private final String BOT_TOKEN;
    private final String CHAT_ID;
    private final TelegramClient telegramClient;
    private final TgMessageSender sender;

    public TgBot(String botToken, String chatId) {
        this.BOT_TOKEN = botToken;
        this.CHAT_ID = chatId;
        this.telegramClient = new OkHttpTelegramClient(BOT_TOKEN);
        this.sender = new TgMessageSender(telegramClient);
    }

    @Override
    public void consume(List<Update> updates) {
        LongPollingSingleThreadUpdateConsumer.super.consume(updates);
    }

    @Override
    public void consume(Update update) {
        Thread.ofVirtual().start(() -> {
            try {
                if (update.hasMessage() && update.getMessage().hasText()) {
                    handleTextMessage(update);
                } else if (update.hasMessage() && update.getMessage().hasDocument()) {
                    handleDocumentMessage(update);
                } else if (update.hasCallbackQuery()) {
                    handleCallbackQuery(update);
                }
            } catch (Exception e) {
                log.error("Error processing update", e);
            }
        });
    }

    // 
    // 
    // 

    private void handleTextMessage(Update update) {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        if (!isAuthorized(chatId)) {
            sendUnauthorizedMessage(chatId);
            return;
        }

        // 
        if (text.startsWith("/")) {
            handleCommand(chatId, text);
            return;
        }

        // Session  TextSessionDispatcher 
        TextSessionDispatcher dispatcher = SpringUtil.getBean(TextSessionDispatcher.class);
        if (dispatcher.dispatch(chatId, text, telegramClient)) {
            return;
        }

        //  session AI 
        handleAiChat(chatId, text);
    }

    // 
    // 
    // 

    private void handleDocumentMessage(Update update) {
        long chatId = update.getMessage().getChatId();

        if (!isAuthorized(chatId)) {
            sendUnauthorizedMessage(chatId);
            return;
        }

        TextSessionDispatcher dispatcher = SpringUtil.getBean(TextSessionDispatcher.class);
        if (!dispatcher.dispatchDocument(chatId, update, telegramClient)) {
            sender.send(chatId, "? ????????????");
        }
    }

    // 
    // 
    // 

    private void handleCommand(long chatId, String command) {
        Thread.ofVirtual().start(() -> {
            try {
                switch (command) {
                    case "/start" -> sendMainMenu(chatId);
                    case "/help"  -> sendHelpMessage(chatId);
                    case "/cancel" -> handleCancelCommand(chatId);
                    default -> {
                        if (command.startsWith("/ssh_config ")) {
                            handleSshConfig(chatId, command);
                        } else if (command.startsWith("/ssh ")) {
                            handleSshCommand(chatId, command);
                        } else {
                            sender.send(chatId, "? ??????? /help ????");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error handling command: {}", command, e);
                sender.send(chatId, "? ??????: " + e.getMessage());
            }
        });
    }

    private void handleCancelCommand(long chatId) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        if (storage.hasActiveSession(chatId)) {
            storage.clearSession(chatId);
            sender.send(chatId, "? ???????");
        } else {
            sender.send(chatId, "? ??????????");
        }
    }

    // 
    // SSH
    // 

    private void handleSshConfig(long chatId, String command) {
        try {
            String configString = command.substring(12).trim();
            if (configString.isEmpty()) {
                sender.send(chatId,
                    "? ????\n\n??: /ssh_config host port username password\n" +
                    "??: /ssh_config 192.168.1.100 22 root mypassword"
                );
                return;
            }

            String[] parts = configString.split("\\s+", 4);
            if (parts.length < 4) {
                sender.send(chatId, "? ??????? host port username password ?4???");
                return;
            }

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            String username = parts[2];
            String password = parts[3];

            sender.send(chatId, "? ??????...");

            Thread.ofVirtual().start(() -> {
                try {
                    SshService sshService = SpringUtil.getBean(SshService.class);
                    if (sshService.testConnection(host, port, username, password)) {
                        SshConnectionStorage.getInstance().saveConnection(chatId, host, port, username, password);
                        sender.send(chatId, String.format(
                            "? SSH ??????\n\n??: %s:%d\n??: %s\n\n?? /ssh [??] ?????",
                            host, port, username
                        ));
                    } else {
                        sender.send(chatId, "? ????????????????");
                    }
                } catch (Exception e) {
                    log.error("Failed to test SSH connection", e);
                    sender.send(chatId, "? ????: " + e.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            sender.send(chatId, "? ???????");
        } catch (Exception e) {
            log.error("Failed to configure SSH", e);
            sender.send(chatId, "? ????: " + e.getMessage());
        }
    }

    private void handleSshCommand(long chatId, String command) {
        SshConnectionStorage storage = SshConnectionStorage.getInstance();
        if (!storage.hasConnection(chatId)) {
            sender.send(chatId, "? ??? SSH ??????? /ssh_config ????");
            return;
        }

        try {
            String sshCommand = command.substring(5).trim();
            if (sshCommand.isEmpty()) {
                sender.send(chatId, "? ????????????: /ssh ls -la");
                return;
            }

            sender.send(chatId, "? ??????...");

            SshConnectionStorage.SshInfo info = storage.getConnection(chatId);
            SshService sshService = SpringUtil.getBean(SshService.class);

            CompletableFuture.supplyAsync(() ->
                sshService.executeCommand(info.getHost(), info.getPort(), info.getUsername(), info.getPassword(), sshCommand)
            ).thenAccept(result -> {
                sender.sendMd(chatId, sshService.formatOutput(result));
                log.info("SSH command executed: chatId={}, command={}", chatId, sshCommand);
            }).exceptionally(ex -> {
                log.error("Failed to execute SSH command", ex);
                sender.send(chatId, "? ????: " + ex.getMessage());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to handle SSH command", e);
            sender.send(chatId, "? ????: " + e.getMessage());
        }
    }

    // 
    // AI 
    // 

    private void handleAiChat(long chatId, String message) {
        try {
            sender.send(chatId, "? ???...");
            AiChatService aiChatService = SpringUtil.getBean(AiChatService.class);
            aiChatService.chat(chatId, message).thenAccept(response ->
                sender.sendMd(chatId, MarkdownFormatter.formatAiResponse(response))
            ).exceptionally(ex -> {
                log.error("AI chat failed", ex);
                sender.send(chatId, "? AI ????: " + ex.getMessage());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to handle AI chat", e);
            sender.send(chatId, "? ????: " + e.getMessage());
        }
    }

    // 
    // 
    // 

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (!isAuthorized(chatId)) {
            sendUnauthorizedMessage(chatId);
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                CallbackHandlerFactory factory = SpringUtil.getBean(CallbackHandlerFactory.class);
                CallbackHandler handler = factory.getHandler(callbackData).orElse(null);

                if (handler == null) {
                    log.warn("???????? handler: callbackData={}", callbackData);
                    return;
                }

                BotApiMethod<? extends Serializable> response = handler.handle(
                    update.getCallbackQuery(), telegramClient
                );

                if (response != null) {
                    telegramClient.execute(response);
                }
            } catch (TelegramApiException e) {
                log.error("??????: callbackData={}", callbackData, e);
                sender.send(chatId, "? ?????????????");
            } catch (Exception e) {
                log.error("??????: callbackData={}", callbackData, e);
                sender.send(chatId, "? ??????????????");
            }
        });
    }

    // 
    //  & 
    // 

    private void sendMainMenu(long chatId) {
        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text("???????????")
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboard(KeyboardBuilder.buildMainMenu())
                    .build())
                .build());
        } catch (TelegramApiException e) {
            log.error("???????", e);
        }
    }

    private void sendHelpMessage(long chatId) {
        String helpText =
            "? *????*\n\n" +
            "*?????*\n" +
            "? `/start` ? ?????\n" +
            "? `/help` ? ???????\n" +
            "? `/cancel` ? ??????\n\n" +
            "*SSH ???*\n" +
            "? `/ssh_config host port user pwd` ? ????\n" +
            "? `/ssh [??]` ? ?? SSH ??\n" +
            "? ??: `/ssh ls -la`\n\n" +
            "*AI ???*\n" +
            "? ????????? AI ??\n\n" +
            "? ??????? /start ?????";

        sender.sendMd(chatId, MarkdownFormatter.formatMarkdown(helpText));
    }

    // 
    // 
    // 

    private boolean isAuthorized(long chatId) {
        return CHAT_ID.equals(String.valueOf(chatId));
    }

    private void sendUnauthorizedMessage(long chatId) {
        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text("? ????????????????https://github.com/tony-wang1990/king-detective")
                .build());
        } catch (TelegramApiException e) {
            log.error("?????????", e);
        }
    }
}
