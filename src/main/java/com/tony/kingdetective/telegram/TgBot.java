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
            sender.send(chatId, "❌ 请先在相关菜单中发起操作");
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
                            sender.send(chatId, "❌ 未知命令，输入 /help 查看帮助");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error handling command: {}", command, e);
                sender.send(chatId, "❌ 命令处理失败: " + e.getMessage());
            }
        });
    }

    private void handleCancelCommand(long chatId) {
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        if (storage.hasActiveSession(chatId)) {
            storage.clearSession(chatId);
            sender.send(chatId, "✅ 已取消当前操作");
        } else {
            sender.send(chatId, "❓ 当前没有进行中的操作");
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
                    "❌ 参数不足\n\n格式: /ssh_config host port username password\n" +
                    "例如: /ssh_config 192.168.1.100 22 root mypassword"
                );
                return;
            }

            String[] parts = configString.split("\\s+", 4);
            if (parts.length < 4) {
                sender.send(chatId, "❌ 参数不足，需要 host port username password 共4个参数");
                return;
            }

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            String username = parts[2];
            String password = parts[3];

            sender.send(chatId, "🔄 正在测试连接...");

            Thread.ofVirtual().start(() -> {
                try {
                    SshService sshService = SpringUtil.getBean(SshService.class);
                    if (sshService.testConnection(host, port, username, password)) {
                        SshConnectionStorage.getInstance().saveConnection(chatId, host, port, username, password);
                        sender.send(chatId, String.format(
                            "✅ SSH 连接配置成功\n\n主机: %s:%d\n用户: %s\n\n使用 /ssh [命令] 来执行命令",
                            host, port, username
                        ));
                    } else {
                        sender.send(chatId, "❌ 连接测试失败，请检查配置是否正确");
                    }
                } catch (Exception e) {
                    log.error("Failed to test SSH connection", e);
                    sender.send(chatId, "❌ 连接失败: " + e.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            sender.send(chatId, "❌ 端口号格式错误");
        } catch (Exception e) {
            log.error("Failed to configure SSH", e);
            sender.send(chatId, "❌ 配置失败: " + e.getMessage());
        }
    }

    private void handleSshCommand(long chatId, String command) {
        SshConnectionStorage storage = SshConnectionStorage.getInstance();
        if (!storage.hasConnection(chatId)) {
            sender.send(chatId, "❌ 未配置 SSH 连接，请先使用 /ssh_config 命令配置");
            return;
        }

        try {
            String sshCommand = command.substring(5).trim();
            if (sshCommand.isEmpty()) {
                sender.send(chatId, "❌ 请输入要执行的命令，例如: /ssh ls -la");
                return;
            }

            sender.send(chatId, "⏳ 正在执行命令...");

            SshConnectionStorage.SshInfo info = storage.getConnection(chatId);
            SshService sshService = SpringUtil.getBean(SshService.class);

            CompletableFuture.supplyAsync(() ->
                sshService.executeCommand(info.getHost(), info.getPort(), info.getUsername(), info.getPassword(), sshCommand)
            ).thenAccept(result -> {
                sender.sendMd(chatId, sshService.formatOutput(result));
                log.info("SSH command executed: chatId={}, command={}", chatId, sshCommand);
            }).exceptionally(ex -> {
                log.error("Failed to execute SSH command", ex);
                sender.send(chatId, "❌ 执行失败: " + ex.getMessage());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to handle SSH command", e);
            sender.send(chatId, "❌ 处理失败: " + e.getMessage());
        }
    }

    // 
    // AI 
    // 

    private void handleAiChat(long chatId, String message) {
        try {
            sender.send(chatId, "🤔 思考中...");
            AiChatService aiChatService = SpringUtil.getBean(AiChatService.class);
            aiChatService.chat(chatId, message).thenAccept(response ->
                sender.sendMd(chatId, MarkdownFormatter.formatAiResponse(response))
            ).exceptionally(ex -> {
                log.error("AI chat failed", ex);
                sender.send(chatId, "❌ AI 对话失败: " + ex.getMessage());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to handle AI chat", e);
            sender.send(chatId, "❌ 处理失败: " + e.getMessage());
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
                    log.warn("未找到处理回调的 handler: callbackData={}", callbackData);
                    return;
                }

                BotApiMethod<? extends Serializable> response = handler.handle(
                    update.getCallbackQuery(), telegramClient
                );

                if (response != null) {
                    telegramClient.execute(response);
                }
            } catch (TelegramApiException e) {
                log.error("处理回调失败: callbackData={}", callbackData, e);
                sender.send(chatId, "❌ 处理请求时发生错误，请重试");
            } catch (Exception e) {
                log.error("回调处理异常: callbackData={}", callbackData, e);
                sender.send(chatId, "❌ 系统错误，请重试或联系管理员");
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
                .text("请选择需要执行的操作：")
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboard(KeyboardBuilder.buildMainMenu())
                    .build())
                .build());
        } catch (TelegramApiException e) {
            log.error("发送主菜单失败", e);
        }
    }

    private void sendHelpMessage(long chatId) {
        String helpText =
            "📖 *命令帮助*\n\n" +
            "*基础命令：*\n" +
            "├ `/start` — 显示主菜单\n" +
            "├ `/help` — 显示此帮助信息\n" +
            "└ `/cancel` — 取消当前操作\n\n" +
            "*SSH 管理：*\n" +
            "├ `/ssh_config host port user pwd` — 配置连接\n" +
            "├ `/ssh [命令]` — 执行 SSH 命令\n" +
            "└ 示例: `/ssh ls -la`\n\n" +
            "*AI 聊天：*\n" +
            "└ 直接发送消息即可与 AI 对话\n\n" +
            "💡 更多功能请点击 /start 查看主菜单";

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
                .text("❌ 无权限操作此机器人🤖，项目地址：https://github.com/tony-wang1990/king-detective")
                .build());
        } catch (TelegramApiException e) {
            log.error("发送无权限消息失败", e);
        }
    }
}
