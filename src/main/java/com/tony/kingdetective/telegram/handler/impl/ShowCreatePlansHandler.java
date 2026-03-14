package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
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
 * Show create instance plans handler
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class ShowCreatePlansHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String userId = callbackData.split(":")[1];
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            return buildEditMessage(
                    callbackQuery,
                    "?"??,
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Plan 1: AMD 1C1G50G
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "? ??1: 1??G50G (AMD)",
                        "ci:" + userId + ":plan1"
                )
        ));
        
        // Plan 2: ARM 1C6G50G
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "? ??2: 1??G50G (ARM)",
                        "ci:" + userId + ":plan2"
                )
        ));
        
        // Plan 3: ARM 2C12G50G (NEW)
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "? ??3: 2??2G50G (ARM)",
                        "ci:" + userId + ":plan3"
                )
        ));
        
        // Plan 4: ARM 4C24G100G (NEW)
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "????4: 4??4G100G (ARM)",
                        "ci:" + userId + ":plan4"
                )
        ));
        
        // Back button - now goes back to config list since we skipped config operations
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????????", "config_list")
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        // Format tenant create time
        String tenantCreateTimeStr = user.getTenantCreateTime() != null 
                ? user.getTenantCreateTime().toString().replace("T", " ")
                : "??";
        
        String message = String.format(
                "????????\n\n" +
                "? ????%s\n" +
                "? ????s\n" +
                "? ????%s\n" +
                "? ????????s\n\n" +
                "????????",
                user.getUsername(),
                user.getOciRegion(),
                user.getTenantName() != null ? user.getTenantName() : "??",
                tenantCreateTimeStr
        );
        
        return buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "show_create_plans:";
    }
}
