package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.enums.SysCfgTypeEnum;
import com.tony.kingdetective.service.IOciKvService;
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
 *   Handler
 * / KV 
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class ScheduledPowerHandler extends AbstractCallbackHandler {

    private static final String KV_KEY_PREFIX = "scheduled_power:";

    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && (
            callbackData.equals("scheduled_power_management") ||
            callbackData.startsWith("scheduled_power_list:") ||
            callbackData.startsWith("scheduled_power_config:") ||
            callbackData.startsWith("scheduled_power_set:") ||
            callbackData.startsWith("scheduled_power_custom:")
        );
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();

        if (data.equals("scheduled_power_management")) {
            return showAccountList(callbackQuery);
        } else if (data.startsWith("scheduled_power_list:")) {
            return showInstanceList(callbackQuery, data.substring("scheduled_power_list:".length()));
        } else if (data.startsWith("scheduled_power_config:")) {
            return showConfigPanel(callbackQuery, data.substring("scheduled_power_config:".length()));
        } else if (data.startsWith("scheduled_power_set:")) {
            return setConfig(callbackQuery, data.substring("scheduled_power_set:".length()), telegramClient);
        } else if (data.startsWith("scheduled_power_custom:")) {
            return startCustomTimeInput(callbackQuery, data.substring("scheduled_power_custom:".length()));
        }
        return buildEditMessage(callbackQuery, "? ????");
    }

    private BotApiMethod<? extends Serializable> showAccountList(CallbackQuery callbackQuery) {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciUser> users = userService.getEnabledOciUserList();
        if (users == null || users.isEmpty()) return buildEditMessage(callbackQuery, "? ??????");

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (OciUser user : users) {
            rows.add(new InlineKeyboardRow(
                KeyboardBuilder.button("? " + user.getUsername(), "scheduled_power_list:" + user.getId())
            ));
        }
        rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
        return buildEditMessage(callbackQuery, "? *?????*\n\n??????????", new InlineKeyboardMarkup(rows));
    }

    private BotApiMethod<? extends Serializable> showInstanceList(CallbackQuery callbackQuery, String userId) {
        try {
            IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
            OciUser user = userService.getById(userId);
            if (user == null) return buildEditMessage(callbackQuery, "? ?????");

            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);

            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(buildDto(user))) {
                var instances = fetcher.getComputeClient().listInstances(
                    com.oracle.bmc.core.requests.ListInstancesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .build()
                ).getItems();
                if (instances.isEmpty()) return buildEditMessage(callbackQuery, "? ?????????");

                StringBuilder sb = new StringBuilder("? *??????????*\n\n");
                List<InlineKeyboardRow> rows = new ArrayList<>();
                
                for (var inst : instances) {
                    if ("TERMINATED".equals(inst.getLifecycleState().getValue()) || "TERMINATING".equals(inst.getLifecycleState().getValue())) continue;
                    
                    String key = KV_KEY_PREFIX + inst.getId();
                    OciKv cfg = kvService.getOne(new LambdaQueryWrapper<OciKv>().eq(OciKv::getCode, key));
                    String status = cfg != null ? " [??]" : "";
                    
                    rows.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("? " + inst.getDisplayName() + status, "scheduled_power_config:" + userId + ":" + inst.getId())
                    ));
                }

                rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "scheduled_power_management")));
                rows.add(new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow()));
                return buildEditMessage(callbackQuery, sb.toString(), new InlineKeyboardMarkup(rows));
            }
        } catch (Exception e) {
            log.error("Failed to list instances for schedule", e);
            return buildEditMessage(callbackQuery, "? ???????" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> showConfigPanel(CallbackQuery callbackQuery, String params) {
        String[] parts = params.split(":");
        String userId = parts[0];
        String instanceId = parts[1];

        IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
        String key = KV_KEY_PREFIX + instanceId;
        OciKv cfg = kvService.getOne(new LambdaQueryWrapper<OciKv>().eq(OciKv::getCode, key));

        String currentCfg = cfg != null ? cfg.getValue() : "???";
        
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
            KeyboardBuilder.button("? ?1???8? (?7h)", "scheduled_power_set:" + userId + ":" + instanceId + ":01|08")
        ));
        rows.add(new InlineKeyboardRow(
            KeyboardBuilder.button("? ?2???9? (?7h)", "scheduled_power_set:" + userId + ":" + instanceId + ":02|09")
        ));
        rows.add(new InlineKeyboardRow(
            KeyboardBuilder.button("? ?23???7?", "scheduled_power_set:" + userId + ":" + instanceId + ":23|07")
        ));
        rows.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?? ????? (??)", "scheduled_power_custom:" + userId + ":" + instanceId)
        ));
        rows.add(new InlineKeyboardRow(
            KeyboardBuilder.button("?? ??????", "scheduled_power_set:" + userId + ":" + instanceId + ":clear")
        ));
        rows.add(new InlineKeyboardRow(KeyboardBuilder.button("? ??????", "scheduled_power_list:" + userId)));

        return buildEditMessage(callbackQuery, 
            "? *???????*\n\n" +
            "?????`" + currentCfg + "`\n\n" +
            "?????? UTC+8 ???????????????????? `HH|HH`?? `01|08` ?? 01:00 ???08:00 ????",
            new InlineKeyboardMarkup(rows)
        );
    }

    private BotApiMethod<? extends Serializable> setConfig(CallbackQuery callbackQuery, String params, TelegramClient telegramClient) {
        String[] parts = params.split(":");
        String userId = parts[0];
        String instanceId = parts[1];
        String action = parts[2];

        try {
            IOciKvService kvService = SpringUtil.getBean(IOciKvService.class);
            String key = KV_KEY_PREFIX + instanceId;
            LambdaQueryWrapper<OciKv> wrapper = new LambdaQueryWrapper<OciKv>().eq(OciKv::getCode, key);
            
            if ("clear".equals(action)) {
                kvService.remove(wrapper);
                return buildEditMessage(callbackQuery, "? ????????",
                    KeyboardBuilder.fromRows(List.of(
                        new InlineKeyboardRow(KeyboardBuilder.button("? ??", "scheduled_power_config:" + userId + ":" + instanceId))
                    ))
                );
            } else {
                String[] times = action.split("\\|");
                String configStr = "STOP=" + times[0] + ":00,START=" + times[1] + ":00,USER=" + userId;
                
                OciKv cfg = kvService.getOne(wrapper);
                if (cfg != null) {
                    cfg.setValue(configStr);
                    kvService.updateById(cfg);
                } else {
                    cfg = new OciKv();
                    cfg.setId(IdUtil.getSnowflakeNextIdStr());
                    cfg.setCode(key);
                    cfg.setValue(configStr);
                    cfg.setType(SysCfgTypeEnum.SYS_INIT_CFG.getCode());
                    kvService.save(cfg);
                }

                return buildEditMessage(callbackQuery, "? ?????????\n\n????\n- ?? `" + times[0] + ":00` ??\n- ?? `" + times[1] + ":00` ??",
                    KeyboardBuilder.fromRows(List.of(
                        new InlineKeyboardRow(KeyboardBuilder.button("? ??", "scheduled_power_config:" + userId + ":" + instanceId))
                    ))
                );
            }
        } catch (Exception e) {
            log.error("Failed to set schedule", e);
            return buildEditMessage(callbackQuery, "? ?????" + e.getMessage());
        }
    }

    private BotApiMethod<? extends Serializable> startCustomTimeInput(CallbackQuery callbackQuery, String params) {
        long chatId = callbackQuery.getMessage().getChatId();
        String[] parts = params.split(":");
        String userId = parts[0];
        String instanceId = parts[1];

        com.tony.kingdetective.telegram.storage.ConfigSessionStorage storage =
            com.tony.kingdetective.telegram.storage.ConfigSessionStorage.getInstance();
        var data = new java.util.HashMap<String, Object>();
        data.put("userId", userId);
        data.put("instanceId", instanceId);
        storage.startCustomSession(chatId,
            com.tony.kingdetective.telegram.storage.ConfigSessionStorage.SessionType.SCHEDULED_POWER_INPUT, data);

        return buildEditMessage(callbackQuery,
            "? *????????*\n\n" +
            "?????????????`????|????`\n\n" +
            "???\n" +
            "`01|08` ? ?? 01:00 ???08:00 ??\n" +
            "`22|06` ? ?? 22:00 ???06:00 ??\n\n" +
            "? ??? UTC+8????? 00-23??? /cancel ???"
        );
    }

    private SysUserDTO buildDto(OciUser user) {
        return SysUserDTO.builder()
            .ociCfg(SysUserDTO.OciCfg.builder()
                .userId(user.getOciUserId())
                .tenantId(user.getOciTenantId())
                .region(user.getOciRegion())
                .fingerprint(user.getOciFingerprint())
                .privateKeyPath(user.getOciKeyPath())
                .build())
            .username(user.getUsername())
            .build();
    }

    @Override
    public String getCallbackPattern() {
        return "scheduledpower_";
    }
}