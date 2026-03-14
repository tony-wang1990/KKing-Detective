package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.enums.SysCfgEnum;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.ConfigSessionStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
 * VNC Configuration Management Handler
 * Handles VNC URL configuration for instance connections
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class VncConfigHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        // Get current VNC configuration
        LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OciKv::getCode, SysCfgEnum.SYS_VNC.getCode());
        OciKv vncConfig = kvService.getOne(wrapper);
        
        boolean hasConfig = vncConfig != null && StringUtils.isNotBlank(vncConfig.getValue());
        
        String text;
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        if (hasConfig) {
            String vncUrl = vncConfig.getValue().trim();
            text = String.format(
                " *VNC ????*\n\n" +
                " ?????\n" +
                "??VNC URL: %s\n" +
                "????? ?????\n\n" +
                " ?????\n" +
                "??URL ???????? VNC ???\n" +
                "????????????????\"?VNC??\"??\n" +
                "?????? URL ??????VNC ?????\n\n" +
                "?? URL ?????\n" +
                "??IP??: http://IP:?? (???? /vnc.html)\n" +
                "????HTTP: http://domain.com (?? /vnc.html)\n" +
                "????HTTPS: https://domain.com (?? /myvnc/vnc.html)\n\n" +
                " ???\n" +
                "??http://192.168.1.100:6080\n" +
                "??http://vnc.example.com\n" +
                "??https://vnc.example.com\n\n" +
                "?? ???\n" +
                "??????URL ??????\n" +
                "???? VNC ?????????\n" +
                "????????????????IP:6080\n\n" +
                "Please enter configuration",
                vncUrl
            );
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(" ????", "vnc_setup")
            ));
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("???????", "vnc_delete")
            ));
        } else {
            text = " *VNC ????*\n\n" +
                   " ?????? VNC URL\n\n" +
                   " ?????\n" +
                   "?? VNC URL ???????????VNC ????\n" +
                   "?????? URL ?? VNC ?????\n\n" +
                   "?? URL ?????\n" +
                   "??IP??: http://IP:?? (???? /vnc.html)\n" +
                   "????HTTP: http://domain.com (?? /vnc.html)\n" +
                   "????HTTPS: https://domain.com (?? /myvnc/vnc.html)\n\n" +
                   " ???\n" +
                   "??http://192.168.1.100:6080\n" +
                   "??http://vnc.example.com\n" +
                   "??https://vnc.example.com\n\n" +
                   "?? ???\n" +
                   "??????URL ??????\n" +
                   "??????????????????IP:6080\n" +
                   "???? VNC ?????? noVNC??????\n\n" +
                   "Please enter configuration";
            
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("???? VNC URL", "vnc_setup")
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
        return "vnc_config";
    }
}

/**
 * VNC Setup Handler
 * Prompts user to enter VNC URL
 */
@Component
class VncSetupHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VncSetupHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Mark this chat as configuring VNC
        ConfigSessionStorage.getInstance().startVncConfig(chatId);
        
        String text = " *?? VNC URL*\n\n" +
                     "??????VNC URL??????????\n\n" +
                     " ?????\n" +
                     "??http://192.168.1.100:6080\n" +
                     "??http://vnc.example.com\n" +
                     "??https://vnc.example.com\n\n" +
                     "?? ?????\n" +
                     "??????http:// ??https:// ??\n" +
                     "??????????????\n" +
                     "?????????????0/443?\n" +
                     "??URL ??????VNC ?????\n" +
                     "  - IP?? ??/vnc.html\n" +
                     "  - HTTP?? ??/vnc.html\n" +
                     "  - HTTPS?? ??/myvnc/vnc.html\n\n" +
                     " ???\n" +
                     "/cancel";
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?????", "vnc_config")
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
        return "vnc_setup";
    }
}

/**
 * VNC Delete Handler
 * Deletes VNC configuration
 */
@Component
class VncDeleteHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VncDeleteHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Delete VNC configuration
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OciKv::getCode, SysCfgEnum.SYS_VNC.getCode());
            kvService.remove(wrapper);
            
            log.info("VNC configuration deleted");
            
            String text = "??*VNC ??????\n\n" +
                         "???????????????IP:6080?\n\n" +
                         " ???\n" +
                         "????????? VNC URL";
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "vnc_config")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to delete VNC configuration", e);
            
            String text = "??*?? VNC ????*\n\n" +
                         "Error: " + e.getMessage();
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "vnc_config")
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
        return "vnc_delete";
    }
}
