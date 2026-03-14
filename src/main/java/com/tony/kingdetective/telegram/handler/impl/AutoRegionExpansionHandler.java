package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Region;
import com.oracle.bmc.identity.model.RegionSubscription;
import com.oracle.bmc.identity.requests.ListRegionSubscriptionsRequest;
import com.oracle.bmc.identity.requests.ListRegionsRequest;
import com.oracle.bmc.identity.responses.ListRegionSubscriptionsResponse;
import com.oracle.bmc.identity.responses.ListRegionsResponse;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.config.OracleInstanceFetcher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Auto region expansion handler
 * Automatically subscribe to new regions
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AutoRegionExpansionHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            if (CollectionUtil.isEmpty(users)) {
                return buildEditMessage(
                        callbackQuery,
                        "? ????? OCI ??",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("?????????\n\n");
            message.append("??????????\n\n");
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            for (SysUserDTO user : users) {
                message.append(String.format(
                        "? %s\n" +
                        "   ????: %s\n\n",
                        user.getUsername(),
                        user.getOciCfg().getRegion()
                ));
                
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button(
                                user.getUsername() + " (" + user.getOciCfg().getRegion() + ")",
                                "region_expand:" + user.getOciCfg().getId()
                        )
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("? ????", "region_expand_all")
            ));
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to list accounts for region expansion", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "auto_region_expansion";
    }
}

/**
 * Region expand for single account handler
 */
@Slf4j
@Component
class RegionExpandHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String ociCfgId = callbackQuery.getData().split(":")[1];
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            SysUserDTO user = sysService.getOciUser(ociCfgId);
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                IdentityClient identityClient = fetcher.getIdentityClient();
                String tenantId = user.getOciCfg().getTenantId();
                
                // Get all available regions
                ListRegionsRequest listRegionsReq = ListRegionsRequest.builder().build();
                ListRegionsResponse listRegionsResp = identityClient.listRegions(listRegionsReq);
                List<Region> allRegions = listRegionsResp.getItems();
                
                // Get subscribed regions
                ListRegionSubscriptionsRequest listSubReq = ListRegionSubscriptionsRequest.builder()
                        .tenancyId(tenantId)
                        .build();
                ListRegionSubscriptionsResponse listSubResp = identityClient.listRegionSubscriptions(listSubReq);
                List<String> subscribedRegions = listSubResp.getItems().stream()
                        .map(RegionSubscription::getRegionName)
                        .collect(Collectors.toList());
                
                // Find unsubscribed regions
                List<Region> unsubscribedRegions = allRegions.stream()
                        .filter(r -> !subscribedRegions.contains(r.getName()))
                        .collect(Collectors.toList());
                
                StringBuilder message = new StringBuilder();
                message.append(String.format("?%s - ?????\n\n", user.getUsername()));
                message.append(String.format("???: %d ???\n", subscribedRegions.size()));
                message.append(String.format("???: %d ???\n\n", unsubscribedRegions.size()));
                
                if (unsubscribedRegions.isEmpty()) {
                    message.append("? ?????????\n\n");
                    message.append("??????:\n");
                    for (String region : subscribedRegions) {
                        message.append(String.format("  ? %s\n", region));
                    }
                    
                    return buildEditMessage(
                            callbackQuery,
                            message.toString(),
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "auto_region_expansion")
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                message.append("??????:\n");
                for (Region region : unsubscribedRegions) {
                    message.append(String.format("  ? %s (%s)\n", region.getName(), region.getKey()));
                }
                
                message.append("\n?? ??:\n");
                message.append("? ???????????\n");
                message.append("? Oracle????????\n");
                message.append("? ??????\n");
                message.append("? ?????????\n\n");
                message.append("? Oracle????????????????");
                
                return buildEditMessage(
                        callbackQuery,
                        message.toString(),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("? ????", "region_expand:" + ociCfgId)
                                ),
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "auto_region_expansion")
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to expand regions", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_region_expansion")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "region_expand:";
    }
}

/**
 * Region expand for all accounts handler
 */
@Slf4j
@Component
class RegionExpandAllHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            
            StringBuilder message = new StringBuilder();
            message.append("??????????\n\n");
            
            int totalSubscribed = 0;
            int totalUnsubscribed = 0;
            
            for (SysUserDTO user : users) {
                if (Boolean.TRUE.equals(user.getOciCfg().getDeleted())) {
                    continue;
                }
                
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    IdentityClient identityClient = fetcher.getIdentityClient();
                    String tenantId = user.getOciCfg().getTenantId();
                    
                    // Get all regions
                    ListRegionsRequest listRegionsReq = ListRegionsRequest.builder().build();
                    ListRegionsResponse listRegionsResp = identityClient.listRegions(listRegionsReq);
                    int allRegionsCount = listRegionsResp.getItems().size();
                    
                    // Get subscribed regions
                    ListRegionSubscriptionsRequest listSubReq = ListRegionSubscriptionsRequest.builder()
                            .tenancyId(tenantId)
                            .build();
                    ListRegionSubscriptionsResponse listSubResp = identityClient.listRegionSubscriptions(listSubReq);
                    int subscribedCount = listSubResp.getItems().size();
                    int unsubscribedCount = allRegionsCount - subscribedCount;
                    
                    totalSubscribed += subscribedCount;
                    totalUnsubscribed += unsubscribedCount;
                    
                    String status = unsubscribedCount == 0 ? "?" : "??";
                    message.append(String.format(
                            "%s %s: %d/%d ???\n",
                            status,
                            user.getUsername(),
                            subscribedCount,
                            allRegionsCount
                    ));
                    
                } catch (Exception e) {
                    log.error("Failed to check regions for user: {}", user.getUsername(), e);
                    message.append(String.format("? %s: ????\n", user.getUsername()));
                }
            }
            
            message.append("\n????????????????\n");
            message.append(String.format("? %d ???\n", users.size()));
            message.append(String.format("????: %d\n", totalSubscribed));
            message.append(String.format("????: %d\n\n", totalUnsubscribed));
            
            if (totalUnsubscribed == 0) {
                message.append("? ?????????????");
            } else {
                message.append("? Oracle???????????\n");
                message.append("? ??????");
            }
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("? ??", "region_expand_all")
                            ),
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_region_expansion")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to expand all regions", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_region_expansion")
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "region_expand_all";
    }
}
