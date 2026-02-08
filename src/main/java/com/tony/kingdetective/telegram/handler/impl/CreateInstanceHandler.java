package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.InstanceCreationConfig;
import com.tony.kingdetective.model.InstancePlan;
import com.tony.kingdetective.service.IInstanceCreationService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建实例回调处理器
 * 
 * @author yohann
 */
@Slf4j
@Component
public class CreateInstanceHandler extends AbstractCallbackHandler {
    
    @Autowired
    private InstanceCreationConfig instanceCreationConfig;
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        log.info("CreateInstanceHandler.handle() called with callbackData: {}", callbackData);
        
        try {
            String[] parts = callbackData.split(":");
            log.debug("Callback parts: length={}, parts={}", parts.length, String.join(", ", parts));
            
            if (parts.length < 3) {
                log.error("Invalid callback data format: {}", callbackData);
                return null;
            }
            
            String userId = parts[1];
            String planType = parts[2];
            
            // Check if we have count and broadcast parameters (final confirmation)
            if (parts.length > 4) {
                int count = Integer.parseInt(parts[3]);
                boolean joinChannelBroadcast = Boolean.parseBoolean(parts[4]);
                log.info("Executing instance creation: userId={}, planType={}, count={}, broadcast={}", 
                    userId, planType, count, joinChannelBroadcast);
                return executeInstanceCreation(callbackQuery, telegramClient, userId, planType, count, joinChannelBroadcast);
            }
            
            // Check if we have count parameter (broadcast selection step)
            if (parts.length > 3) {
                int count = Integer.parseInt(parts[3]);
                log.info("Showing broadcast options: userId={}, planType={}, count={}", userId, planType, count);
                return showBroadcastOptions(callbackQuery, userId, planType, count);
            }
            
            // This is the initial plan selection, show quantity options
            log.info("Showing quantity options: userId={}, planType={}", userId, planType);
            return showQuantityOptions(callbackQuery, userId, planType);
            
        } catch (Exception e) {
            log.error("Error handling callback in CreateInstanceHandler: callbackData={}", callbackData, e);
            return null;
        }
    }
    
    /**
     * Show quantity selection options
     */
    private BotApiMethod<? extends Serializable> showQuantityOptions(
            CallbackQuery callbackQuery,
            String userId,
            String planType) {
        
        log.debug("showQuantityOptions: userId={}, planType={}", userId, planType);
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            log.warn("User not found: userId={}", userId);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 配置不存在",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        log.debug("User found: username={}, region={}", user.getUsername(), user.getOciRegion());
        InstancePlan plan = getPlanByType(planType);
        
        String message = String.format(
                "【选择创建数量】\n\n" +
                "🔑 配置名：%s\n" +
                "🌏 区域：%s\n" +
                "💻 方案：%s\n" +
                "⚙️ 配置：%dC%dG%dG\n" +
                "🏗️ 架构：%s\n" +
                "💿 系统：%s\n\n" +
                "请选择需要创建的实例数量：",
                user.getUsername(),
                user.getOciRegion(),
                planType.equals("plan1") ? "方案1" : "方案2",
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem()
        );
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Add quantity buttons (1-4 in two rows)
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("1台", "create_instance:" + userId + ":" + planType + ":1"),
                KeyboardBuilder.button("2台", "create_instance:" + userId + ":" + planType + ":2")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("3台", "create_instance:" + userId + ":" + planType + ":3"),
                KeyboardBuilder.button("4台", "create_instance:" + userId + ":" + planType + ":4")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "show_create_plans:" + userId)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        log.debug("showQuantityOptions: Built message with {} keyboard rows", keyboard.size());
        EditMessageText result = buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
        log.info("showQuantityOptions: Successfully built EditMessageText for userId={}", userId);
        return result;
    }
    
    /**
     * Show broadcast options after quantity selection
     */
    private BotApiMethod<? extends Serializable> showBroadcastOptions(
            CallbackQuery callbackQuery,
            String userId,
            String planType,
            int count) {
        
        log.info("showBroadcastOptions: userId={}, planType={}, count={}", userId, planType, count);
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            log.warn("showBroadcastOptions: User not found: userId={}", userId);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 配置不存在",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        log.debug("showBroadcastOptions: User found: username={}", user.getUsername());
        
        // 获取方案详情
        InstancePlan plan = getPlanByType(planType);
        
        // Build message asking about channel broadcast
        String message = String.format(
                "【开机方案确认】\n\n" +
                "🔑 配置名：%s\n" +
                "🌏 区域：%s\n" +
                "💻 方案：%s\n" +
                "⚙️ 配置：%dC%dG%dG\n" +
                "🏗️ 架构：%s\n" +
                "💿 系统：%s\n" +
                "🔢 数量：%d台\n\n" +
                "📢 是否向 TG 频道推送开机成功信息？\n" +
                "（开启后，开机成功时会自动向频道发送放货信息）",
                user.getUsername(),
                user.getOciRegion(),
                planType.equals("plan1") ? "方案1" : "方案2",
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem(),
                count
        );
        
        // Build keyboard with options
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "✅ 开启频道推送",
                        "create_instance:" + userId + ":" + planType + ":" + count + ":true"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "❌ 关闭频道推送",
                        "create_instance:" + userId + ":" + planType + ":" + count + ":false"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "create_instance:" + userId + ":" + planType)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        log.debug("showBroadcastOptions: Built message with {} keyboard rows", keyboard.size());
        EditMessageText result = buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
        log.info("showBroadcastOptions: Successfully built EditMessageText for userId={}, count={}", userId, count);
        return result;
    }
    
    /**
     * Execute instance creation with specified parameters
     */
    private BotApiMethod<? extends Serializable> executeInstanceCreation(
            CallbackQuery callbackQuery, 
            TelegramClient telegramClient,
            String userId, 
            String planType,
            int count,
            boolean joinChannelBroadcast) {
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 配置不存在",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        // 获取方案详情
        InstancePlan plan = getPlanByType(planType);
        plan.setCreateNumbers(count);  // Set the actual count
        plan.setJoinChannelBroadcast(joinChannelBroadcast);
        
        // 启动异步创建
        InstanceCreationService creationService = SpringUtil.getBean(InstanceCreationService.class);
        
        try {
            // 先删除回调消息
            telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(Math.toIntExact(callbackQuery.getMessage().getMessageId()))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
        }
        
        // 发送创建中的消息
        String channelStatus = joinChannelBroadcast ? "✅ 已开启" : "❌ 已关闭";
        String creatingMessage = String.format(
                "⏳ 正在创建实例...\n\n" +
                "🔑 配置名：%s\n" +
                "🌏 区域：%s\n" +
                "💻 方案：%s\n" +
                "⚙️ 配置：%dC%dG%dG\n" +
                "🏗️ 架构：%s\n" +
                "💿 系统：%s\n" +
                "🔢 数量：%d台\n" +
                "📢 频道推送：%s\n\n" +
                "请稍候，任务已提交...",
                user.getUsername(),
                user.getOciRegion(),
                planType.equals("plan1") ? "方案1" : "方案2",
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem(),
                count,
                channelStatus
        );
        
        // 异步提交创建任务
        creationService.createInstanceAsync(
                userId,
                plan,
                callbackQuery.getMessage().getChatId(),
                telegramClient
        );
        
        return SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .text(creatingMessage)
                .build();
    }
    
    private InstancePlan getPlanByType(String planType) {
        if ("plan1".equals(planType)) {
            // AMD 1C1G
            return InstancePlan.builder()
                    .ocpus(1)
                    .memory(1)
                    .disk(50)
                    .architecture("AMD")
                    .operationSystem("Ubuntu")
                    .interval(instanceCreationConfig.getRetryIntervalSeconds())
                    .createNumbers(1)
                    .build();
        } else {
            // ARM 1C6G
            return InstancePlan.builder()
                    .ocpus(1)
                    .memory(6)
                    .disk(50)
                    .architecture("ARM")
                    .operationSystem("Ubuntu")
                    .interval(instanceCreationConfig.getRetryIntervalSeconds())
                    .createNumbers(1)
                    .build();
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "create_instance:";
    }
}
