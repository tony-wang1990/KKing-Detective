package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.VirtualNetworkClient;
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
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Open all ports handler - one-click open all ingress/egress ports
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class OpenAllPortsHandler extends AbstractCallbackHandler {
    
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
                        "? ????????",
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "select_config:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
            storage.setInstanceCache(chatId, instances);
            
            return buildOpenPortsInstanceListMessage(callbackQuery, instances, ociCfgId, chatId);
            
        } catch (Exception e) {
            log.error("Failed to list instances for opening ports", e);
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
    
    private BotApiMethod<? extends Serializable> buildOpenPortsInstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId) {
        
        StringBuilder message = new StringBuilder("?????????\n\n");
        message.append("????????/????\n\n");
        message.append(String.format("? %d ???????\n", instances.size()));
        message.append("???????????\n\n");
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            
            message.append(String.format(
                    "%d. %s\n" +
                    "   ??: %s\n\n",
                    i + 1,
                    instance.getName(),
                    instance.getRegion()
            ));
            
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(KeyboardBuilder.button(
                    String.format("? ??%d", i + 1),
                    "open_ports_instance:" + i
            ));
            keyboard.add(row);
        }
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("? ????????", "open_ports_all")
        ));
        
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
        return "open_all_ports:";
    }
}

/**
 * Open ports for selected instance
 */
@Slf4j
@Component
class OpenPortsInstanceHandler extends AbstractCallbackHandler {
    
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
                    "? ?????",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildCancelRow()))
            );
        }
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                VirtualNetworkClient vnClient = fetcher.getVirtualNetworkClient();
                
                // Get VCN for the instance's subnet
                String subnetId = instance.getSubnetId();
                
                // Fallback: fetch subnet ID from VNIC if not present in instance object
                if (subnetId == null || subnetId.isEmpty()) {
                    ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                            .compartmentId(fetcher.getCompartmentId())
                            .instanceId(instance.getOcId())
                            .build();
                    ListVnicAttachmentsResponse vnicResponse = fetcher.getComputeClient().listVnicAttachments(vnicRequest);
                    
                    if (!vnicResponse.getItems().isEmpty()) {
                        String vnicId = vnicResponse.getItems().get(0).getVnicId();
                        GetVnicRequest getVnicRequest = GetVnicRequest.builder().vnicId(vnicId).build();
                        GetVnicResponse getVnicResponse = fetcher.getVirtualNetworkClient().getVnic(getVnicRequest);
                        subnetId = getVnicResponse.getVnic().getSubnetId();
                    }
                }
                
                if (subnetId == null || subnetId.isEmpty()) {
                    return buildEditMessage(
                            callbackQuery,
                            "? ??????? (???? VNIC ??)",
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "open_all_ports:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                GetSubnetRequest getSubnetReq = GetSubnetRequest.builder()
                        .subnetId(subnetId)
                        .build();
                GetSubnetResponse getSubnetResp = vnClient.getSubnet(getSubnetReq);
                String vcnId = getSubnetResp.getSubnet().getVcnId();
                
                // Get security lists
                ListSecurityListsRequest listSecReq = ListSecurityListsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .vcnId(vcnId)
                        .build();
                ListSecurityListsResponse listSecResp = vnClient.listSecurityLists(listSecReq);
                
                int updatedCount = 0;
                
                for (SecurityList secList : listSecResp.getItems()) {
                    // Create rule to allow all traffic
                    IngressSecurityRule ingressRule = IngressSecurityRule.builder()
                            .protocol("all")
                            .source("0.0.0.0/0")
                            .description("Allow all ingress traffic")
                            .build();
                    
                    EgressSecurityRule egressRule = EgressSecurityRule.builder()
                            .protocol("all")    
                            .destination("0.0.0.0/0")
                            .description("Allow all egress traffic")
                            .build();
                    
                    List<IngressSecurityRule> ingressRules = new ArrayList<>(secList.getIngressSecurityRules());
                    List<EgressSecurityRule> egressRules = new ArrayList<>(secList.getEgressSecurityRules());
                    
                    // Check if rules already exist
                    boolean hasIngressAll = ingressRules.stream()
                            .anyMatch(r -> "all".equals(r.getProtocol()) && "0.0.0.0/0".equals(r.getSource()));
                    boolean hasEgressAll = egressRules.stream()
                            .anyMatch(r -> "all".equals(r.getProtocol()) && "0.0.0.0/0".equals(r.getDestination()));
                    
                    if (!hasIngressAll) {
                        ingressRules.add(ingressRule);
                    }
                    if (!hasEgressAll) {
                        egressRules.add(egressRule);
                    }
                    
                    if (!hasIngressAll || !hasEgressAll) {
                        UpdateSecurityListRequest updateReq = UpdateSecurityListRequest.builder()
                                .securityListId(secList.getId())
                                .updateSecurityListDetails(UpdateSecurityListDetails.builder()
                                        .ingressSecurityRules(ingressRules)
                                        .egressSecurityRules(egressRules)
                                        .build())
                                .build();
                        
                        vnClient.updateSecurityList(updateReq);
                        updatedCount++;
                    }
                }
                
                return buildEditMessage(
                        callbackQuery,
                        String.format(
                                "? ???????\n\n" +
                                "??: %s\n" +
                                "??: %s\n\n" +
                                "???:\n" +
                                "? ?????? 0.0.0.0/0\n" +
                                "? ?????? 0.0.0.0/0\n" +
                                "? ??? %d ?????\n\n" +
                                "? ??TCP/UDP?????",
                                instance.getName(),
                                instance.getRegion(),
                                updatedCount
                        ),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "open_all_ports:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to open ports", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????\n\n" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "open_all_ports:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "open_ports_instance:";
    }
}

/**
 * Open all ports for ALL instances (Batch)
 */
@Slf4j
@Component
class OpenPortsAllHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        String ociCfgId = storage.getConfigContext(chatId);
        List<SysUserDTO.CloudInstance> instances = storage.getInstanceCache(chatId);
        
        if (CollectionUtil.isEmpty(instances)) {
             return buildEditMessage(
                    callbackQuery,
                    "? ??????????????",
                    new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildCancelRow()))
            );
        }

        // Send processing message
        try {
            telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("? ????????????...")
                    .build());
        } catch (Exception ignored) {}
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            int successCount = 0;
            int totalVcnUpdated = 0;
            java.util.Set<String> processedVcnIds = new java.util.HashSet<>();
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                VirtualNetworkClient vnClient = fetcher.getVirtualNetworkClient();
                
                for (SysUserDTO.CloudInstance instance : instances) {
                    try {
                        // 1. Get Subnet ID (Robust logic)
                        String subnetId = instance.getSubnetId();
                        if (subnetId == null || subnetId.isEmpty()) {
                            ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                                    .compartmentId(fetcher.getCompartmentId())
                                    .instanceId(instance.getOcId())
                                    .build();
                            ListVnicAttachmentsResponse vnicResponse = fetcher.getComputeClient().listVnicAttachments(vnicRequest);
                            if (!vnicResponse.getItems().isEmpty()) {
                                String vnicId = vnicResponse.getItems().get(0).getVnicId();
                                GetVnicRequest getVnicRequest = GetVnicRequest.builder().vnicId(vnicId).build();
                                GetVnicResponse getVnicResponse = fetcher.getVirtualNetworkClient().getVnic(getVnicRequest);
                                subnetId = getVnicResponse.getVnic().getSubnetId();
                            }
                        }
                        
                        if (subnetId == null) continue; // Skip if still null
                        
                        // 2. Get VCN ID
                        GetSubnetRequest getSubnetReq = GetSubnetRequest.builder().subnetId(subnetId).build();
                        String vcnId = vnClient.getSubnet(getSubnetReq).getSubnet().getVcnId();
                        
                        // Avoid re-processing same VCN
                        if (processedVcnIds.contains(vcnId)) {
                            successCount++;
                            continue;
                        }
                        
                        // 3. Update Security Lists for this VCN
                        ListSecurityListsRequest listSecReq = ListSecurityListsRequest.builder()
                                .compartmentId(fetcher.getCompartmentId())
                                .vcnId(vcnId)
                                .build();
                        ListSecurityListsResponse listSecResp = vnClient.listSecurityLists(listSecReq);
                        
                        boolean vcnUpdated = false;
                        for (SecurityList secList : listSecResp.getItems()) {
                             // Create rule to allow all traffic
                            IngressSecurityRule ingressRule = IngressSecurityRule.builder()
                                    .protocol("all")
                                    .source("0.0.0.0/0")
                                    .description("Allow all ingress traffic")
                                    .build();
                            
                            EgressSecurityRule egressRule = EgressSecurityRule.builder()
                                    .protocol("all")    
                                    .destination("0.0.0.0/0")
                                    .description("Allow all egress traffic")
                                    .build();
                            
                            List<IngressSecurityRule> ingressRules = new ArrayList<>(secList.getIngressSecurityRules());
                            List<EgressSecurityRule> egressRules = new ArrayList<>(secList.getEgressSecurityRules());
                            
                            boolean hasIngressAll = ingressRules.stream()
                                    .anyMatch(r -> "all".equals(r.getProtocol()) && "0.0.0.0/0".equals(r.getSource()));
                            boolean hasEgressAll = egressRules.stream()
                                    .anyMatch(r -> "all".equals(r.getProtocol()) && "0.0.0.0/0".equals(r.getDestination()));
                            
                            if (!hasIngressAll) {
                                ingressRules.add(ingressRule);
                            }
                            if (!hasEgressAll) {
                                egressRules.add(egressRule);
                            }
                            
                            if (!hasIngressAll || !hasEgressAll) {
                                UpdateSecurityListRequest updateReq = UpdateSecurityListRequest.builder()
                                        .securityListId(secList.getId())
                                        .updateSecurityListDetails(UpdateSecurityListDetails.builder()
                                                .ingressSecurityRules(ingressRules)
                                                .egressSecurityRules(egressRules)
                                                .build())
                                        .build();
                                vnClient.updateSecurityList(updateReq);
                                vcnUpdated = true;
                            }
                        }
                        
                        processedVcnIds.add(vcnId);
                        successCount++;
                        if (vcnUpdated) totalVcnUpdated++;
                        
                    } catch (Exception e) {
                        log.warn("Failed to process instance {} for open ports: {}", instance.getName(), e.getMessage());
                    }
                }
            }
            
            return buildEditMessage(
                    callbackQuery,
                    String.format(
                            "? **???????**\n\n" +
                            "????: %d ???\n" +
                            "????: %d ???\n" +
                            "????: %d ? VCN\n\n" +
                            "?????????????????????",
                            instances.size(),
                            successCount,
                            totalVcnUpdated
                    ),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ????", "open_all_ports:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
             log.error("Failed to batch open ports", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????\n\n" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "open_all_ports:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }

    @Override
    public String getCallbackPattern() {
        return "open_ports_all";
    }
}

/**
 * Open all ports config selector
 */
@Slf4j
@Component
class OpenAllPortsConfigSelectHandler extends AbstractCallbackHandler {
    
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
            message.append("??????? OCI ???\n\n");
            
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            for (SysUserDTO user : users) {
                message.append(String.format(
                        "? %s\n" +
                        "   ??: %s\n\n",
                        user.getUsername(),
                        user.getOciCfg().getRegion()
                ));
                
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button(
                                user.getUsername() + " (" + user.getOciCfg().getRegion() + ")",
                                "open_all_ports:" + user.getOciCfg().getId()
                        )
                ));
            }
            
            keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to list OCI configs", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "open_all_ports_select";
    }
}
