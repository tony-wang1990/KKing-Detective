package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.ISysService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Daily report handler
 * Send daily summary reports of OCI resources
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class DailyReportHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_KEY = "daily_report_enabled";
    private static final String REPORT_TIME_KEY = "daily_report_time";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            // Check current report status
            OciKv reportKv = kvService.getByKey(REPORT_KEY);
            boolean isEnabled = reportKv != null && "true".equals(reportKv.getValue());
            
            // Get report time
            OciKv timeKv = kvService.getByKey(REPORT_TIME_KEY);
            String reportTime = (timeKv != null && timeKv.getValue() != null) ? timeKv.getValue() : "09";
            
            StringBuilder message = new StringBuilder();
            message.append("??????\n\n");
            message.append(String.format("????: %s\n\n", isEnabled ? "? ???" : "? ???"));
            
            message.append("? ????:\n");
            message.append("? ????????\n");
            message.append("? ????????\n");
            message.append("? ????????\n");
            message.append("? ?????\n");
            message.append("? ??????\n\n");
            
            message.append(String.format("? ????: ?? %s:00\n\n", reportTime));
            
            if (isEnabled) {
                message.append("? ???????????\n");
                message.append("???????????\n\n");
                message.append("?? ??: ??????????");
            } else {
                message.append("? ???????????\n");
                message.append("??????");
            }
            
            List<InlineKeyboardRow> keyboard = List.of(
                    new InlineKeyboardRow(
                            KeyboardBuilder.button(
                                    isEnabled ? "? ????" : "? ????",
                                    isEnabled ? "report_disable" : "report_enable"
                            )
                    ),
                    new InlineKeyboardRow(
                            KeyboardBuilder.button("? ??????", "report_today"),
                            KeyboardBuilder.button("?? ????", "report_schedule")
                    ),
                    KeyboardBuilder.buildBackToMainMenuRow(),
                    KeyboardBuilder.buildCancelRow()
            );
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get daily report status", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "daily_report";
    }
}

/**
 * Enable daily report handler
 */
@Slf4j
@Component
class ReportEnableHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_KEY = "daily_report_enabled";
    private static final String REPORT_TIME_KEY = "daily_report_time";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv reportKv = kvService.getByKey(REPORT_KEY);
            if (reportKv == null) {
                reportKv = new OciKv();
                reportKv.setCode(REPORT_KEY);
                reportKv.setValue("true");
                reportKv.setType("SYSTEM"); // Fix: Set type for NOT NULL constraint
                kvService.save(reportKv);
            } else {
                reportKv.setValue("true");
                kvService.updateById(reportKv);
            }
            
            // Get report time
            OciKv timeKv = kvService.getByKey(REPORT_TIME_KEY);
            String reportTime = (timeKv != null && timeKv.getValue() != null) ? timeKv.getValue() : "09";
            
            return buildEditMessage(
                    callbackQuery,
                    "? ???????\n\n" +
                    String.format("?????? %s:00 ??????\n\n", reportTime) +
                    "??????:\n" +
                    "? ??????\n" +
                    "? ??????\n" +
                    "? ?????\n" +
                    "? ??????\n\n" +
                    "? ??: ??????????????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to enable daily report", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "report_enable";
    }
}

/**
 * Disable daily report handler
 */
@Slf4j
@Component
class ReportDisableHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_KEY = "daily_report_enabled";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        
        try {
            OciKv reportKv = kvService.getByKey(REPORT_KEY);
            if (reportKv != null) {
                reportKv.setValue("false");
                kvService.updateById(reportKv);
            }
            
            return buildEditMessage(
                    callbackQuery,
                    "? ???????\n\n" +
                    "?????????????\n\n" +
                    "? ?????:\n" +
                    "? ????????\n" +
                    "? ????????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        } catch (Exception e) {
            log.error("Failed to disable daily report", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "report_disable";
    }
}

/**
 * View today's report handler
 */
@Slf4j
@Component
class ReportTodayHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("????????\n");
            message.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            message.append("\n\n");
            
            int totalInstances = 0;
            int runningInstances = 0;
            
            for (SysUserDTO user : users) {
                if (Boolean.TRUE.equals(user.getOciCfg().getDeleted())) {
                    continue;
                }
                
                try {
                    List<SysUserDTO.CloudInstance> instances = instanceService.listRunningInstances(user);
                    totalInstances += instances.size();
                    runningInstances += instances.size();
                    
                    message.append(String.format("? %s: %d????\n", 
                            user.getUsername(), instances.size()));
                } catch (Exception e) {
                    log.error("Failed to get instances for user: {}", user.getUsername(), e);
                }
            }
            
            message.append("\n????????????????\n");
            message.append("? ????:\n");
            message.append(String.format("? ????: %d\n", users.size()));
            message.append(String.format("? ????: %d\n", runningInstances));
            message.append(String.format("? ????: %d\n\n", totalInstances));
            
            message.append("? ????????????");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("? ??", "report_today"),
                                    KeyboardBuilder.button("?? ??", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to generate today's report", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "daily_report")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "report_today";
    }
}

/**
 * Report schedule settings handler
 */
@Slf4j
@Component
class ReportScheduleHandler extends AbstractCallbackHandler {
    
    private static final String REPORT_TIME_KEY = "daily_report_time";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        String reportTime = "09";
        
        try {
            OciKv timeKv = kvService.getByKey(REPORT_TIME_KEY);
            if (timeKv != null && timeKv.getValue() != null) {
                reportTime = timeKv.getValue();
            }
        } catch (Exception ignored) {}
        
        StringBuilder message = new StringBuilder();
        message.append("????????\n\n");
        message.append(String.format("??????: %s:00\n\n", reportTime));
        message.append("????????:\n");
        
        List<InlineKeyboardRow> keyboard = List.of(
                new InlineKeyboardRow(
                        KeyboardBuilder.button("? 07:00", "report_time:07"),
                        KeyboardBuilder.button("? 09:00", "report_time:09")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("? 12:00", "report_time:12"),
                        KeyboardBuilder.button("? 18:00", "report_time:18")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("? 21:00", "report_time:21"),
                        KeyboardBuilder.button("? 23:00", "report_time:23")
                ),
                new InlineKeyboardRow(
                        KeyboardBuilder.button("?? ??", "daily_report")
                ),
                KeyboardBuilder.buildCancelRow()
        );
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "report_schedule";
    }
}
