package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.core.lang.Validator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.enums.SysCfgEnum;
import com.tony.kingdetective.enums.SysCfgTypeEnum;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 *   Handler
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class AlertEmailHandler extends AbstractCallbackHandler {

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.equals("alert_email_management") ||
            callbackData.equals("alert_email_add") ||
            callbackData.equals("alert_email_del")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.equals("alert_email_management")) {
            return showEmailConfig(callbackQuery);
        } else if (data.equals("alert_email_add")) {
            return startAddEmail(callbackQuery);
        } else if (data.equals("alert_email_del")) {
            return deleteEmail(callbackQuery, telegramClient);
        }
        return buildEditMessage(callbackQuery, "? ????");
    }

    private BotApiMethod<? extends Serializable> showEmailConfig(CallbackQuery callbackQuery) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OciKv::getCode, "sys-alert-email");
        OciKv emailConfig = kvService.getOne(wrapper);

        String currentEmail = emailConfig != null ? emailConfig.getValue() : "???";

        return buildEditMessage(callbackQuery,
            "? *??????*\n\n" +
            "????????????????IP?????? Telegram ???????????????\n\n" +
            "? ???????`" + currentEmail + "`",
            KeyboardBuilder.fromRows(List.of(
                new InlineKeyboardRow(
                    KeyboardBuilder.button("? ?????", "alert_email_add"),
                    KeyboardBuilder.button("?? ????", "alert_email_del")
                ),
                new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
            ))
        );
    }

    private BotApiMethod<? extends Serializable> startAddEmail(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        ConfigSessionStorage storage = ConfigSessionStorage.getInstance();
        storage.startCustomSession(chatId, ConfigSessionStorage.SessionType.ALERT_EMAIL_INPUT, new HashMap<>());

        return buildEditMessage(callbackQuery,
            "? *??????*\n\n" +
            "???????????????????? `admin@example.com`??\n\n" +
            "?? /cancel ???"
        );
    }

    private BotApiMethod<? extends Serializable> deleteEmail(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        try {
            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OciKv::getCode, "sys-alert-email");
            kvService.remove(wrapper);

            return buildEditMessage(callbackQuery,
                "? *???????*",
                KeyboardBuilder.fromRows(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "alert_email_management")),
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                ))
            );
        } catch (Exception e) {
            log.error("Failed to delete alert email", e);
            return buildEditMessage(callbackQuery, "? ?????" + e.getMessage());
        }
    }
}
