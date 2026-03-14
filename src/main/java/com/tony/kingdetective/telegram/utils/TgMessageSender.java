package com.tony.kingdetective.telegram.utils;

import com.tony.kingdetective.telegram.utils.MarkdownFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Telegram  Markdown 
 *
 * @author Tony Wang
 */
@Slf4j
@RequiredArgsConstructor
public class TgMessageSender {

    private final TelegramClient telegramClient;

    /**
     *  Markdown
     */
    public void send(long chatId, String text) {
        sendInternal(chatId, text, false);
    }

    /**
     *  Markdown 
     */
    public void sendMd(long chatId, String text) {
        sendInternal(chatId, text, true);
    }

    private void sendInternal(long chatId, String text, boolean markdown) {
        try {
            String truncated = MarkdownFormatter.truncate(text);
            SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(chatId)
                .text(truncated);
            if (markdown) {
                builder.parseMode("Markdown");
            }
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.error("发送消息失败: chatId={}", chatId, e);
            // Markdown 
            if (markdown) {
                try {
                    telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build());
                } catch (TelegramApiException ex) {
                    log.error("降级发送也失败: chatId={}", chatId, ex);
                }
            }
        }
    }
}
