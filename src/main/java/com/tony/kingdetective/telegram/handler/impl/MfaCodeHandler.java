package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.enums.SysCfgEnum;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.utils.CommonUtils;
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
 * MFA Code Handler
 * Handles MFA one-time password generation
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class MfaCodeHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        try {
            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            
            // Get MFA secret from database
            OciKv mfa = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                    .eq(OciKv::getCode, SysCfgEnum.SYS_MFA_SECRET.getCode()));
            
            if (mfa == null || mfa.getValue() == null) {
                return buildMfaNotEnabledMessage(callbackQuery);
            }
            
            // Generate current MFA code
            int mfaCode = CommonUtils.generateMfaCode(mfa.getValue());
            
            // Format code with leading zeros if needed
            String formattedCode = String.format("%06d", mfaCode);
            
            log.info("MFA code generated for chatId: {}", callbackQuery.getMessage().getChatId());
            
            String text = String.format(
                " *MFA ????\n\n" +
                "?????????\n\n" +
                "`%s`\n\n" +
                "??????????30 ?\n" +
                " ????????\n\n" +
                " ??????????????",
                formattedCode
            );
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            // Refresh button
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(" ?????", "mfa_code")
            ));
            
            // Navigation
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to generate MFA code", e);
            return buildErrorMessage(callbackQuery, e.getMessage());
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "mfa_code";
    }
    
    /**
     * Build MFA not enabled message
     */
    private BotApiMethod<? extends Serializable> buildMfaNotEnabledMessage(CallbackQuery callbackQuery) {
        String text = "??*MFA ??????\n\n" +
                     " MFA ????????\n\n" +
                     " ?????????????????????";
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
    
    /**
     * Build error message
     */
    private BotApiMethod<? extends Serializable> buildErrorMessage(CallbackQuery callbackQuery, String errorMsg) {
        String text = String.format(
            "??*????????\n\n" +
            "??????s\n\n" +
            "????????????",
            errorMsg
        );
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
            callbackQuery,
            text,
            new InlineKeyboardMarkup(keyboard)
        );
    }
}
