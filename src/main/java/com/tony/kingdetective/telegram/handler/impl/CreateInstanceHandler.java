package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.InstanceCreationConfig;
import com.tony.kingdetective.telegram.model.InstancePlan;
import com.tony.kingdetective.telegram.service.InstanceCreationService;
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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ?
 * 
 * @author Tony Wang
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
            
            // Check if we have all parameters (final confirmation: userId:planType:count:interval:broadcast)
            if (parts.length > 5) {
                int count = Integer.parseInt(parts[3]);
                int interval = Integer.parseInt(parts[4]);
                boolean joinChannelBroadcast = Boolean.parseBoolean(parts[5]);
                log.info("Executing instance creation: userId={}, planType={}, count={}, interval={}, broadcast={}", 
                    userId, planType, count, interval, joinChannelBroadcast);
                return executeInstanceCreation(callbackQuery, telegramClient, userId, planType, count, interval, joinChannelBroadcast);
            }
            
            // Check if we have count and interval (broadcast selection step)
            if (parts.length > 4) {
                int count = Integer.parseInt(parts[3]);
                int interval = Integer.parseInt(parts[4]);
                log.info("Showing broadcast options: userId={}, planType={}, count={}, interval={}", userId, planType, count, interval);
                return showBroadcastOptions(callbackQuery, userId, planType, count, interval);
            }
            
            // Check if we have count parameter (interval selection step)
            if (parts.length > 3) {
                int count = Integer.parseInt(parts[3]);
                log.info("Showing interval options: userId={}, planType={}, count={}", userId, planType, count);
                return showIntervalOptions(callbackQuery, userId, planType, count);
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
                    "????????",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        log.debug("User found: username={}, region={}", user.getUsername(), user.getOciRegion());
        InstancePlan plan = getPlanByType(planType);
        
        String message = String.format(
                "????????\n\n" +
                " ????%s\n" +
                " ????s\n" +
                " ????s\n" +
                " ????dC%dG%dG\n" +
                "???????s\n" +
                " ????s\n\n" +
                "??????????????",
                user.getUsername(),
                user.getOciRegion(),
                planType.equals("plan1") ? "??1" : "??2",
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem()
        );
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Add quantity buttons (1-4 in two rows)
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("1??", "ci:" + userId + ":" + planType + ":1"),
                KeyboardBuilder.button("2??", "ci:" + userId + ":" + planType + ":2")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("3??", "ci:" + userId + ":" + planType + ":3"),
                KeyboardBuilder.button("4??", "ci:" + userId + ":" + planType + ":4")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "show_create_plans:" + userId)
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
     * Show retry interval selection options
     */
    private BotApiMethod<? extends Serializable> showIntervalOptions(
            CallbackQuery callbackQuery,
            String userId,
            String planType,
            int count) {
        
        log.info("showIntervalOptions: userId={}, planType={}, count={}", userId, planType, count);
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            log.warn("showIntervalOptions: User not found: userId={}", userId);
            return buildEditMessage(
                    callbackQuery,
                    "????????",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        log.debug("showIntervalOptions: User found: username={}", user.getUsername());
        
        // 
        InstancePlan plan = getPlanByType(planType);
        
        // Get plan display name
        String planName = switch (planType) {
            case "plan1" -> "??1";
            case "plan2" -> "??2";
            case "plan3" -> "??3";
            case "plan4" -> "??4";
            default -> planType;
        };
        
        // Build message asking about retry interval
        String message = String.format(
                "????????\n\n" +
                " ????%s\n" +
                " ????s\n" +
                " ????s\n" +
                " ????dC%dG%dG\n" +
                "???????s\n" +
                " ????s\n" +
                " ????d?\n\n" +
                " ??????????\n" +
                "(?????????????????)",
                user.getUsername(),
                user.getOciRegion(),
                planName,
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem(),
                count
        );
        
        // Build keyboard with interval options
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "??15??(???",
                        "ci:" + userId + ":" + planType + ":" + count + ":15"
                ),
                KeyboardBuilder.button(
                        " 30??(??)",
                        "ci:" + userId + ":" + planType + ":" + count + ":30"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "??45??(??)",
                        "ci:" + userId + ":" + planType + ":" + count + ":45"
                ),
                KeyboardBuilder.button(
                        " 60??(??)",
                        "ci:" + userId + ":" + planType + ":" + count + ":60"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "create_instance:" + userId + ":" + planType)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        log.debug("showIntervalOptions: Built message with {} keyboard rows", keyboard.size());
        EditMessageText result = buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
        log.info("showIntervalOptions: Successfully built EditMessageText for userId={}, count={}", userId, count);
        return result;
    }
    
    /**
     * Show broadcast options after quantity and interval selection
     */
    private BotApiMethod<? extends Serializable> showBroadcastOptions(
            CallbackQuery callbackQuery,
            String userId,
            String planType,
            int count,
            int interval) {
        
        log.info("showBroadcastOptions: userId={}, planType={}, count={}, interval={}", userId, planType, count, interval);
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            log.warn("showBroadcastOptions: User not found: userId={}", userId);
            return buildEditMessage(
                    callbackQuery,
                    "????????",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        log.debug("showBroadcastOptions: User found: username={}", user.getUsername());
        
        // 
        InstancePlan plan = getPlanByType(planType);
        
        // Get plan display name
        String planName = switch (planType) {
            case "plan1" -> "??1";
            case "plan2" -> "??2";
            case "plan3" -> "??3";
            case "plan4" -> "??4";
            default -> planType;
        };
        
        // Build message asking about channel broadcast
        String message = String.format(
                "????????\n\n" +
                " ????%s\n" +
                " ????s\n" +
                " ????s\n" +
                " ????dC%dG%dG\n" +
                "???????s\n" +
                " ????s\n" +
                " ????d?\n" +
                " ??????d?\n\n" +
                " ????TG ???????????\n" +
                "(??????????????????????",
                user.getUsername(),
                user.getOciRegion(),
                planName,
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem(),
                count,
                interval
        );
        
        // Build keyboard with options
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "?????????",
                        "ci:" + userId + ":" + planType + ":" + count + ":" + interval + ":true"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "?????????",
                        "ci:" + userId + ":" + planType + ":" + count + ":" + interval + ":false"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?????", "ci:" + userId + ":" + planType + ":" + count)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        log.debug("showBroadcastOptions: Built message with {} keyboard rows", keyboard.size());
        EditMessageText result = buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
        log.info("showBroadcastOptions: Successfully built EditMessageText for userId={}, count={}, interval={}", userId, count, interval);
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
            int interval,
            boolean joinChannelBroadcast) {
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            return buildEditMessage(
                    callbackQuery,
                    "????????",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        // 
        InstancePlan plan = getPlanByType(planType);
        plan.setCreateNumbers(count);  // Set the actual count
        plan.setInterval(interval);  // Set the selected interval
        plan.setJoinChannelBroadcast(joinChannelBroadcast);
        
        // Get plan display name
        String planName = switch (planType) {
            case "plan1" -> "??1";
            case "plan2" -> "??2";
            case "plan3" -> "??3";
            case "plan4" -> "??4";
            default -> planType;
        };
        
        // 
        InstanceCreationService creationService = SpringUtil.getBean(InstanceCreationService.class);
        
        try {
            // ?
            telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(Math.toIntExact(callbackQuery.getMessage().getMessageId()))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
        }
        
        // ?
        String channelStatus = joinChannelBroadcast ? "Channel: ON" : "Channel: OFF";
        String creatingMessage = String.format(
                "????????...\n\n" +
                " ????%s\n" +
                " ????s\n" +
                " ????s\n" +
                " ????dC%dG%dG\n" +
                "???????s\n" +
                " ????s\n" +
                " ????d?\n" +
                " ??????d?\n" +
                " ?????%s\n\n" +
                "??????????..",
                user.getUsername(),
                user.getOciRegion(),
                planName,
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem(),
                count,
                interval,
                channelStatus
        );
        
        // 
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
        switch (planType) {
            case "plan1":
                // AMD 1C1G50G
                return InstancePlan.builder()
                        .ocpus(1)
                        .memory(1)
                        .disk(50)
                        .architecture("AMD")
                        .operationSystem("Ubuntu")
                        .interval(instanceCreationConfig.getRetryIntervalSeconds())
                        .createNumbers(1)
                        .build();
            
            case "plan2":
                // ARM 1C6G50G
                return InstancePlan.builder()
                        .ocpus(1)
                        .memory(6)
                        .disk(50)
                        .architecture("ARM")
                        .operationSystem("Ubuntu")
                        .interval(instanceCreationConfig.getRetryIntervalSeconds())
                        .createNumbers(1)
                        .build();
            
            case "plan3":
                // ARM 2C12G50G (NEW)
                return InstancePlan.builder()
                        .ocpus(2)
                        .memory(12)
                        .disk(50)
                        .architecture("ARM")
                        .operationSystem("Ubuntu")
                        .interval(instanceCreationConfig.getRetryIntervalSeconds())
                        .createNumbers(1)
                        .build();
            
            case "plan4":
                // ARM 4C24G100G (NEW)
                return InstancePlan.builder()
                        .ocpus(4)
                        .memory(24)
                        .disk(100)
                        .architecture("ARM")
                        .operationSystem("Ubuntu")
                        .interval(instanceCreationConfig.getRetryIntervalSeconds())
                        .createNumbers(1)
                        .build();
            
            default:
                throw new IllegalArgumentException("Invalid plan type: " + planType);
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ci:";
    }
}
