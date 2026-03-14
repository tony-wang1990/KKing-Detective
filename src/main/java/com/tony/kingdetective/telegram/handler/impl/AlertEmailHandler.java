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
        return buildEditMessage(callbackQuery, "❌ 未知操作");
    }

    private BotApiMethod<? extends Serializable> showEmailConfig(CallbackQuery callbackQuery) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OciKv::getCode, "sys-alert-email");
        OciKv emailConfig = kvService.getOne(wrapper);

        String currentEmail = emailConfig != null ? emailConfig.getValue() : "未配置";

        return buildEditMessage(callbackQuery,
            "📧 *告警邮件设置*\n\n" +
            "当系统触发重要告警（如实例死机、IP被封）时，除 Telegram 消息外，还将向此邮箱发送通知。\n\n" +
            "📌 当前接收邮箱：`" + currentEmail + "`",
            KeyboardBuilder.fromRows(List.of(
                new InlineKeyboardRow(
                    KeyboardBuilder.button("➕ 配置新邮箱", "alert_email_add"),
                    KeyboardBuilder.button("🗑️ 删除配置", "alert_email_del")
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
            "📧 *配置告警邮箱*\n\n" +
            "请直接发送需要接收告警邮件的邮箱地址（如 `admin@example.com`）：\n\n" +
            "发送 /cancel 可取消"
        );
    }

    private BotApiMethod<? extends Serializable> deleteEmail(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        try {
            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OciKv::getCode, "sys-alert-email");
            kvService.remove(wrapper);

            return buildEditMessage(callbackQuery,
                "✅ *告警邮箱已清除*",
                KeyboardBuilder.fromRows(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.button("← 返回邮件设置", "alert_email_management")),
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                ))
            );
        } catch (Exception e) {
            log.error("Failed to delete alert email", e);
            return buildEditMessage(callbackQuery, "❌ 删除失败：" + e.getMessage());
        }
    }
}
