package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.model.*;
import com.oracle.bmc.core.requests.*;
import com.oracle.bmc.core.responses.*;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.InstanceSelectionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Instance shape change handler - upgrade or downgrade instance shape
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class InstanceShapeChangeHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String ociCfgId = callbackData.split(":")[1];
        long chatId = callbackQuery.getMessage().getChatId();
        
        // Set config context
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        storage.setConfigContext(chatId, ociCfgId);
        storage.clearSelection(chatId);
        
        // Get running instances
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            List<SysUserDTO.CloudInstance> instances = instanceService.listRunningInstances(sysUserDTO);
            
            if (CollectionUtil.isEmpty(instances)) {
                return buildEditMessage(
                        callbackQuery,
                        "? ????????",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "select_config:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            // Cache instances
            storage.setInstanceCache(chatId, instances);
            
            return buildShapeChangeInstanceListMessage(callbackQuery, instances, ociCfgId, chatId);
            
        } catch (Exception e) {
            log.error("Failed to list instances for shape change", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ?????????" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "select_config:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    /**
     * Build instance list for shape change
     */
    private BotApiMethod<? extends Serializable> buildShapeChangeInstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId) {
        
        StringBuilder message = new StringBuilder("???????\n\n");
        message.append(String.format("? %d ???????\n", instances.size()));
        message.append("???????????\n\n");
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            
            message.append(String.format(
                    "%d. %s\n" +
                    "   Shape: %s\n" +
                    "   ??: %s\n\n",
                    i + 1,
                    instance.getName(),
                    instance.getShape(),
                    instance.getRegion()
            ));
            
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(KeyboardBuilder.button(
                    String.format("? ??%d", i + 1),
                    "shape_change_instance:" + i
            ));
            keyboard.add(row);
        }
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("?? ??", "select_config:" + ociCfgId)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(keyboard)
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "shape_change:";
    }
}

/**
 * Select instance for shape change
 */
@Slf4j
@Component
class ShapeChangeInstanceSelectHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int instanceIndex = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, instanceIndex);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (instance == null) {
            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("?????")
                        .showAlert(true)
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to answer callback query", e);
            }
            return null;
        }
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                // Get available shapes
                GetInstanceRequest getRequest = GetInstanceRequest.builder()
                        .instanceId(instance.getOcId())
                        .build();
                
                GetInstanceResponse getResponse = fetcher.getComputeClient().getInstance(getRequest);
                Instance ociInstance = getResponse.getInstance();
                
                // List available shapes
                ListShapesRequest listRequest = ListShapesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .availabilityDomain(ociInstance.getAvailabilityDomain())
                        .build();
                
                ListShapesResponse listResponse = fetcher.getComputeClient().listShapes(listRequest);
                List<Shape> shapes = listResponse.getItems();
                
                // Filter flexible shapes
                List<Shape> flexShapes = shapes.stream()
                        .filter(s -> s.getShape().contains("Flex") || s.getShape().contains("A1"))
                        .collect(Collectors.toList());
                
                StringBuilder message = new StringBuilder();
                message.append("???????\n\n");
                message.append(String.format("??: %s\n", instance.getName()));
                message.append(String.format("??Shape: %s\n\n", instance.getShape()));
                message.append("????:\n\n");
                
                List<InlineKeyboardRow> keyboard = new ArrayList<>();
                
                if (!flexShapes.isEmpty()) {
                    // Show common configurations for Flex shapes
                    if (instance.getShape().contains("Flex") || instance.getShape().contains("A1")) {
                        message.append("? ????? (??????):\n\n");
                        
                        // Common configurations
                        String[][] configs = {
                                {"1", "6", "?? 1?6G"},
                                {"2", "12", "? 2?12G"},
                                {"4", "24", "? 4?24G"}
                        };
                        
                        for (String[] config : configs) {
                            String ocpus = config[0];
                            String memory = config[1];
                            String label = config[2];
                            
                            keyboard.add(new InlineKeyboardRow(
                                    KeyboardBuilder.button(
                                            label,
                                            String.format("confirm_shape_change:%d:%s:%s", instanceIndex, ocpus, memory)
                                    )
                            ));
                        }
                    } else {
                        message.append("?? ?????????\n");
                        message.append("????????:\n\n");
                        
                        for (Shape shape : flexShapes) {
                            keyboard.add(new InlineKeyboardRow(
                                    KeyboardBuilder.button(
                                            shape.getShape(),
                                            "switch_to_flex:" + instanceIndex + ":" + shape.getShape()
                                    )
                            ));
                        }
                    }
                } else {
                    message.append("? ??????????");
                }
                
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("?? ??", "shape_change:" + ociCfgId)
                ));
                keyboard.add(KeyboardBuilder.buildCancelRow());
                
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(keyboard)
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to get shape options", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "shape_change:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "shape_change_instance:";
    }
}

/**
 * Confirm and execute shape change
 */
@Slf4j
@Component
class ConfirmShapeChangeHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String[] parts = callbackData.split(":");
        int instanceIndex = Integer.parseInt(parts[1]);
        String ocpus = parts[2];
        String memory = parts[3];
        
        long chatId = callbackQuery.getMessage().getChatId();
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, instanceIndex);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (instance == null) {
            return buildEditMessage(
                    callbackQuery,
                    "? ?????",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildCancelRow()))
            );
        }
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                // Update instance shape
                UpdateInstanceShapeConfigDetails shapeConfig = UpdateInstanceShapeConfigDetails.builder()
                        .ocpus(Float.parseFloat(ocpus))
                        .memoryInGBs(Float.parseFloat(memory))
                        .build();
                
                UpdateInstanceDetails updateDetails = UpdateInstanceDetails.builder()
                        .shapeConfig(shapeConfig)
                        .build();
                
                UpdateInstanceRequest updateRequest = UpdateInstanceRequest.builder()
                        .instanceId(instance.getOcId())
                        .updateInstanceDetails(updateDetails)
                        .build();
                
                fetcher.getComputeClient().updateInstance(updateRequest);
                
                return buildEditMessage(
                        callbackQuery,
                        String.format(
                                "? ??????????\n\n" +
                                "??: %s\n" +
                                "????: %s? / %sG??\n\n" +
                                "?? ??:\n" +
                                "? ???????????\n" +
                                "? ???????????\n" +
                                "? ??????????",
                                instance.getName(),
                                ocpus,
                                memory
                        ),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "shape_change:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to change instance shape", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????\n\n" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "shape_change:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "confirm_shape_change:";
    }
}
