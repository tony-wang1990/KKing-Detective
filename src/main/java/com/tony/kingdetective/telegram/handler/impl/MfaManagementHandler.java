package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.params.sys.UpdateSysCfgParams;
import com.tony.kingdetective.bean.response.sys.GetSysCfgRsp;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.utils.CommonUtils;
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
 * MFA Management Handler
 * Handles MFA enable/disable and code generation
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class MfaManagementHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            // Get system configuration
            GetSysCfgRsp sysCfg = sysService.getSysCfg();
            
            boolean mfaEnabled = sysCfg.getEnableMfa() != null && sysCfg.getEnableMfa();
            boolean hasSecret = StringUtils.isNotBlank(sysCfg.getMfaSecret());
            
            String text;
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            if (mfaEnabled && hasSecret) {
                // MFA is enabled and has secret - show code
                int mfaCode = CommonUtils.generateMfaCode(sysCfg.getMfaSecret());
                String formattedCode = String.format("%06d", mfaCode);
                
                text = String.format(
                    "? *MFA ??*\n\n" +
                    "? ??????????\n\n" +
                    "? ??????\n" +
                    "`%s`\n\n" +
                    "? ?????\n" +
                    "?????? 30 ?????\n" +
                    "??????????\n" +
                    "???????MFA ?????\n\n" +
                    "?? ???\n" +
                    "?? MFA ??????????????\n" +
                    "??????????????\n\n" +
                    "?? ??????"?,
                    formattedCode
                );
                
                keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? ?????"?, "mfa_refresh")
                ));
                
                keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? ?? MFA", "mfa_disable_confirm")
                ));
                
            } else if (!mfaEnabled && hasSecret) {
                // Has secret but MFA is disabled
                text = "? *MFA ??*\n\n" +
                       "? ??????????\n\n" +
                       "? ???\n" +
                       "MFA ????????????????\n" +
                       "?????????????\n\n" +
                       "?? ???\n" +
                       "???????????????\n" +
                       "???????????? Web ???????\n\n" +
                       "?? ??????"?;
                
                keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("???? MFA", "mfa_enable"),
                    KeyboardBuilder.button("???????", "mfa_delete_secret")
                ));
                
            } else {
                // No secret configured
                text = "? *MFA ??*\n\n" +
                       "? ??????????\n\n" +
                       "? ??? MFA?\n" +
                       "MFA (Multi-Factor Authentication) ??????????\n" +
                       "???????????????????\n\n" +
                       "? ?????\n" +
                       "??OCI ????\n" +
                       "????????????\n" +
                       "????????\n\n" +
                       "?? ?????\n" +
                       "?????????? MFA?\n" +
                       "??????????????\n\n" +
                       "? ???\n" +
                       "?????????????\n" +
                       "????????????????\n" +
                       "??????????????";
                
                keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("???? MFA", "mfa_enable")
                ));
            }
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get MFA management info", e);
            return buildErrorMessage(callbackQuery, e.getMessage());
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "mfa_management";
    }
    
    /**
     * Build error message
     */
    private BotApiMethod<? extends Serializable> buildErrorMessage(CallbackQuery callbackQuery, String errorMsg) {
        String text = String.format(
            "??*?? MFA ????*\n\n" +
            "??????s\n\n" +
            "????????????"?,
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

/**
 * MFA Refresh Handler
 * Refreshes the MFA code display
 */
@Component
class MfaRefreshHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaRefreshHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        // Just redirect back to MFA management to refresh
        return new MfaManagementHandler().handle(callbackQuery, telegramClient);
    }
    
    @Override
    public String getCallbackPattern() {
        return "mfa_refresh";
    }
}

/**
 * MFA Enable Handler
 * Enables MFA using updateSysCfg API and sends QR code
 */
@Component
class MfaEnableHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaEnableHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        long chatId = callbackQuery.getMessage().getChatId();
        
        try {
            // Show processing message
            telegramClient.execute(buildEditMessage(
                callbackQuery,
                "?"? MFA...\n\n?,
                null
            ));
            
            // Get current config first
            GetSysCfgRsp sysCfg = sysService.getSysCfg();
            
            // Enable MFA (this will auto-generate secret and QR code)
            UpdateSysCfgParams params = new UpdateSysCfgParams();
            params.setEnableMfa(true);
            params.setTgBotToken(sysCfg.getTgBotToken());
            params.setTgChatId(sysCfg.getTgChatId());
            params.setDingToken(sysCfg.getDingToken());
            params.setDingSecret(sysCfg.getDingSecret());
            params.setEnableDailyBroadcast(sysCfg.getEnableDailyBroadcast());
            params.setDailyBroadcastCron(sysCfg.getDailyBroadcastCron());
            params.setEnableVersionInform(sysCfg.getEnableVersionInform());
            params.setGjAiApi(sysCfg.getGjAiApi());
            params.setBootBroadcastToken(sysCfg.getBootBroadcastToken());
            
            sysService.updateSysCfg(params);
            
            log.info("MFA enabled for chatId: {}", chatId);
            
            // Refresh config to get the newly generated secret
            sysCfg = sysService.getSysCfg();
            String formattedCode = "N/A";
            
            if (StringUtils.isNotBlank(sysCfg.getMfaSecret())) {
                int mfaCode = CommonUtils.generateMfaCode(sysCfg.getMfaSecret());
                formattedCode = String.format("%06d", mfaCode);
            }
            
            // Send QR code image
            java.io.File qrFile = new java.io.File(CommonUtils.MFA_QR_PNG_PATH);
            if (qrFile.exists()) {
                org.telegram.telegrambots.meta.api.methods.send.SendPhoto sendPhoto = 
                    org.telegram.telegrambots.meta.api.methods.send.SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new org.telegram.telegrambots.meta.api.objects.InputFile(qrFile))
                        .caption(
                            "? *MFA ????\n\n" +
                            "???????????? Google Authenticator?Microsoft Authenticator ??\n" +
                            "????????????\n\n" +
                            "?? ???\n" +
                            "???????????\n" +
                            "????????????\n" +
                            "?"??
                        )
                        .parseMode("Markdown")
                        .build();
                
                try {
                    telegramClient.execute(sendPhoto);
                } catch (Exception e) {
                    log.error("Failed to send QR code image", e);
                }
            }
            
            String text = String.format(
                "??*MFA ????\n\n" +
                "? ??????\n" +
                "`%s`\n\n" +
                "? ?????\n" +
                "1?? ????????????\n" +
                "2?? ???????????\n" +
                "3?? ?????? 6 ??????\n" +
                "4?? ???????? MFA ??\n\n" +
                "? ???\n" +
                "?????? 30 ?????\n" +
                "??????????????\n" +
                "?"??,
                formattedCode
            );
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "mfa_management")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to enable MFA", e);
            
            String text = "??*?? MFA ??*\n\n" +
                         "?????"? + e.getMessage();
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "mfa_management")
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
        return "mfa_enable";
    }
}

/**
 * MFA Disable Confirm Handler
 * Confirms before disabling MFA
 */
@Component
class MfaDisableConfirmHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaDisableConfirmHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String text = "?? *???? MFA*\n\n" +
                     "?????? MFA ????\n\n" +
                     "?????\n" +
                     "?????????\n" +
                     "??MFA ??????\n" +
                     "??????????\n\n" +
                     "? ???\n" +
                     "???????????????????"?;
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("??????", "mfa_disable"),
            KeyboardBuilder.button("????", "mfa_management")
        ));
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?????", "mfa_management")
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
        return "mfa_disable_confirm";
    }
}

/**
 * MFA Disable Handler
 * Disables MFA using updateSysCfg API
 */
@Component
class MfaDisableHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaDisableHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            // Get current config first
            GetSysCfgRsp sysCfg = sysService.getSysCfg();
            
            // Disable MFA
            UpdateSysCfgParams params = new UpdateSysCfgParams();
            params.setEnableMfa(false);
            params.setTgBotToken(sysCfg.getTgBotToken());
            params.setTgChatId(sysCfg.getTgChatId());
            params.setDingToken(sysCfg.getDingToken());
            params.setDingSecret(sysCfg.getDingSecret());
            params.setEnableDailyBroadcast(sysCfg.getEnableDailyBroadcast());
            params.setDailyBroadcastCron(sysCfg.getDailyBroadcastCron());
            params.setEnableVersionInform(sysCfg.getEnableVersionInform());
            params.setGjAiApi(sysCfg.getGjAiApi());
            params.setBootBroadcastToken(sysCfg.getBootBroadcastToken());
            
            sysService.updateSysCfg(params);
            
            log.info("MFA disabled for chatId: {}", callbackQuery.getMessage().getChatId());
            
            String text = "??*MFA ????\n\n" +
                         "MFA ??????\n\n" +
                         "? ???\n" +
                         "??MFA ??????\n" +
                         "???????????";
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "mfa_management")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to disable MFA", e);
            
            String text = "??*?? MFA ??*\n\n" +
                         "?????"? + e.getMessage();
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "mfa_management")
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
        return "mfa_disable";
    }
}

/**
 * MFA Delete Secret Handler
 * Deletes MFA secret key (will be regenerated when re-enabled via Web)
 */
@Component
class MfaDeleteSecretHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaDeleteSecretHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String text = "?? *???? MFA ??*\n\n" +
                     "??????????MFA ????\n\n" +
                     "?????\n" +
                     "??????????\n" +
                     "?????? Web ??????\n" +
                     "?????????????\n\n" +
                     "? ???\n" +
                     "????????????????"?MFA?;
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("??????", "mfa_delete_secret_confirm"),
            KeyboardBuilder.button("????", "mfa_management")
        ));
        
        keyboard.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?????", "mfa_management")
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
        return "mfa_delete_secret";
    }
}

/**
 * MFA Delete Secret Confirm Handler
 * Actually deletes the MFA secret
 */
@Component
class MfaDeleteSecretConfirmHandler extends AbstractCallbackHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaDeleteSecretConfirmHandler.class);
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            // Get current config
            GetSysCfgRsp sysCfg = sysService.getSysCfg();
            
            // Disable MFA (this will delete the secret)
            UpdateSysCfgParams params = new UpdateSysCfgParams();
            params.setEnableMfa(false);
            params.setTgBotToken(sysCfg.getTgBotToken());
            params.setTgChatId(sysCfg.getTgChatId());
            params.setDingToken(sysCfg.getDingToken());
            params.setDingSecret(sysCfg.getDingSecret());
            params.setEnableDailyBroadcast(sysCfg.getEnableDailyBroadcast());
            params.setDailyBroadcastCron(sysCfg.getDailyBroadcastCron());
            params.setEnableVersionInform(sysCfg.getEnableVersionInform());
            params.setGjAiApi(sysCfg.getGjAiApi());
            params.setBootBroadcastToken(sysCfg.getBootBroadcastToken());
            
            sysService.updateSysCfg(params);
            
            log.info("MFA secret deleted for chatId: {}", callbackQuery.getMessage().getChatId());
            
            String text = "??*MFA ??????\n\n" +
                         "????????????\n\n" +
                         "? ???????\n" +
                         "1?? ???? Web ????\n" +
                         "2?? ??????????\n" +
                         "3?? ?? MFA?????????\n" +
                         "4?? ?????????\n\n" +
                         "? ???\n" +
                         "????????????????"?;
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "mfa_management")
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                callbackQuery,
                text,
                new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to delete MFA secret", e);
            
            String text = "??*?? MFA ????*\n\n" +
                         "?????"? + e.getMessage();
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "mfa_management")
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
        return "mfa_delete_secret_confirm";
    }
}
