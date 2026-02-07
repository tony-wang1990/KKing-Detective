package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.requests.GetInstanceRequest;
import com.oracle.bmc.core.requests.InstanceActionRequest;
import com.oracle.bmc.core.responses.GetInstanceResponse;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.InstanceSelectionStorage;
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
 * Memory occupy handler - occupy 25% memory to prevent Oracle reclaiming idle instances
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class MemoryOccupyHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        String ociCfgId = callbackData.split(":")[1];
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        storage.setConfigContext(chatId, ociCfgId);
        storage.clearSelection(chatId);
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            List<SysUserDTO.CloudInstance> instances = instanceService.listRunningInstances(sysUserDTO);
            
            if (CollectionUtil.isEmpty(instances)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 暂无运行中的实例",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            storage.setInstanceCache(chatId, instances);
            
            return buildMemoryOccupyInstanceListMessage(callbackQuery, instances, ociCfgId, chatId);
            
        } catch (Exception e) {
            log.error("Failed to list instances for memory occupy", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 获取实例列表失败：" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    private BotApiMethod<? extends Serializable> buildMemoryOccupyInstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId) {
        
        StringBuilder message = new StringBuilder("【占用25%内存】\n\n");
        message.append("⚠️ 防止甲骨文回收闲置机器\n\n");
        message.append(String.format("共 %d 个运行中的实例\n", instances.size()));
        message.append("选择实例进行内存占用：\n\n");
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            
            message.append(String.format(
                    "%d. %s\n" +
                    "   Shape: %s\n" +
                    "   区域: %s\n\n",
                    i + 1,
                    instance.getName(),
                    instance.getShape(),
                    instance.getRegion()
            ));
            
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(KeyboardBuilder.button(
                    String.format("💾 实例%d", i + 1),
                    "occupy_memory_instance:" + i
            ));
            keyboard.add(row);
        }
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("🚀 批量占用所有实例", "occupy_memory_all")
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "select_config:" + ociCfgId)
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
        return "memory_occupy:";
    }
}

/**
 * Occupy memory for selected instance
 */
@Slf4j
@Component
class OccupyMemoryInstanceHandler extends AbstractCallbackHandler {
    
    private static final String MEMORY_OCCUPY_SCRIPT = 
            "#!/bin/bash\n" +
            "# Occupy 25% memory to prevent Oracle reclamation\n" +
            "TOTAL_MEM=$(free -m | awk '/^Mem:/{print $2}')\n" +
            "TARGET_MEM=$((TOTAL_MEM * 25 / 100))\n" +
            "echo \"Total Memory: ${TOTAL_MEM}MB\"\n" +
            "echo \"Target 25%: ${TARGET_MEM}MB\"\n" +
            "\n" +
            "# Kill existing memory occupy process\n" +
            "pkill -f 'stress.*--vm-bytes' || true\n" +
            "\n" +
            "# Install stress if not exists\n" +
            "if ! command -v stress &> /dev/null; then\n" +
            "    if command -v apt-get &> /dev/null; then\n" +
            "        sudo apt-get update && sudo apt-get install -y stress\n" +
            "    elif command -v yum &> /dev/null; then\n" +
            "        sudo yum install -y stress\n" +
            "    fi\n" +
            "fi\n" +
            "\n" +
            "# Occupy memory\n" +
            "nohup stress --vm 1 --vm-bytes ${TARGET_MEM}M --vm-keep > /dev/null 2>&1 &\n" +
            "echo \"Memory occupy started. PID: $!\"\n";
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int instanceIndex = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, instanceIndex);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (instance == null) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 实例不存在",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildCancelRow()))
            );
        }
        
        StringBuilder message = new StringBuilder();
        message.append("【执行内存占用】\n\n");
        message.append(String.format("实例: %s\n", instance.getName()));
        message.append(String.format("区域: %s\n\n", instance.getRegion()));
        message.append("⚠️ 请手动执行以下脚本:\n\n");
        message.append("```bash\n");
        message.append(MEMORY_OCCUPY_SCRIPT);
        message.append("```\n\n");
        message.append("💡 说明:\n");
        message.append("• 脚本会占用25%内存防止回收\n");
        message.append("• 使用 stress 工具实现\n");
        message.append("• 进程会持续运行直到手动停止\n\n");
        message.append("🛑 停止占用:\n");
        message.append("```bash\n");
        message.append("pkill -f 'stress.*--vm-bytes'\n");
        message.append("```\n");
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("◀️ 返回", "memory_occupy:" + ociCfgId)
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "occupy_memory_instance:";
    }
}

/**
 * Occupy memory for all instances
 */
@Slf4j
@Component
class OccupyMemoryAllHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        String ociCfgId = storage.getConfigContext(chatId);
        List<SysUserDTO.CloudInstance> instances = storage.getInstanceCache(chatId);
        
        if (CollectionUtil.isEmpty(instances)) {
            return buildEditMessage(
                    callbackQuery,
                    "❌ 未找到实例",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildCancelRow()))
            );
        }
        
        StringBuilder message = new StringBuilder();
        message.append("【批量内存占用脚本】\n\n");
        message.append(String.format("共 %d 个实例\n\n", instances.size()));
        message.append("请为每个实例执行以下脚本:\n\n");
        
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            message.append(String.format("━━ %d. %s ━━\n", i + 1, instance.getName()));
            message.append(String.format("SSH登录后执行:\n"));
            message.append("```bash\n");
            message.append("curl -sSL https://raw.githubusercontent.com/your-repo/scripts/memory_occupy.sh | bash\n");
            message.append("```\n\n");
        }
        
        message.append("💡 或使用内置脚本 (见单个实例详情)\n");
        
        return buildEditMessage(
                callbackQuery,
                message.toString(),
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("◀️ 返回", "memory_occupy:" + ociCfgId)
                        ),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "occupy_memory_all";
    }
}
