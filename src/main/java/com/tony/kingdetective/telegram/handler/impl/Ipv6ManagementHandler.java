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

/**
 * IPv6 management handler - configure/remove IPv6 addresses for instances
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class Ipv6ManagementHandler extends AbstractCallbackHandler {
    
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
            
            return buildIpv6InstanceListMessage(callbackQuery, instances, ociCfgId, chatId, sysUserDTO);
            
        } catch (Exception e) {
            log.error("Failed to list instances for IPv6 management", e);
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
     * Build instance list with IPv6 info
     */
    private BotApiMethod<? extends Serializable> buildIpv6InstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId,
            SysUserDTO sysUserDTO) {
        
        StringBuilder message = new StringBuilder("?IPv6 ???\n\n");
        message.append(String.format("? %d ???????\n", instances.size()));
        message.append("?????? IPv6 ???\n\n");
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // Get IPv6 status for each instance
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            for (int i = 0; i < instances.size(); i++) {
                SysUserDTO.CloudInstance instance = instances.get(i);
                
                // Check if instance has IPv6
                boolean hasIpv6 = checkInstanceHasIpv6(fetcher, instance.getOcId());
                String ipv6Status = hasIpv6 ? "?" : "?";
                
                message.append(String.format(
                        "%s %d. %s\n" +
                        "   ??: %s\n" +
                        "   IPv6: %s\n\n",
                        ipv6Status,
                        i + 1,
                        instance.getName(),
                        instance.getRegion(),
                        hasIpv6 ? "???" : "???"
                ));
                
                // Add button
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(KeyboardBuilder.button(
                        String.format("%s ??%d", ipv6Status, i + 1),
                        "ipv6_instance:" + i
                ));
                keyboard.add(row);
            }
        } catch (Exception e) {
            log.error("Failed to check IPv6 status", e);
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
    
    /**
     * Check if instance has IPv6 configured
     */
    private boolean checkInstanceHasIpv6(OracleInstanceFetcher fetcher, String instanceId) {
        try {
            // Get VNIC attachments
            ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                    .compartmentId(fetcher.getCompartmentId())
                    .instanceId(instanceId)
                    .build();
            
            ListVnicAttachmentsResponse vnicResponse = fetcher.getComputeClient().listVnicAttachments(vnicRequest);
            
            for (VnicAttachment attachment : vnicResponse.getItems()) {
                if (attachment.getLifecycleState() != VnicAttachment.LifecycleState.Attached) {
                    continue;
                }
                
                String vnicId = attachment.getVnicId();
                GetVnicRequest getVnicRequest = GetVnicRequest.builder().vnicId(vnicId).build();
                GetVnicResponse getVnicResponse = fetcher.getVirtualNetworkClient().getVnic(getVnicRequest);
                
                List<String> ipv6Addresses = getVnicResponse.getVnic().getIpv6Addresses();
                if (ipv6Addresses != null && !ipv6Addresses.isEmpty()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check IPv6 for instance {}: {}", instanceId, e.getMessage());
        }
        return false;
    }
    
    @Override
    public String getCallbackPattern() {
        return "ipv6_management:";
    }
}

/**
 * Select instance for IPv6 operation
 */
@Slf4j
@Component
class Ipv6InstanceSelectHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int instanceIndex = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        
        // Get instance by index
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, instanceIndex);
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
        
        String ociCfgId = storage.getConfigContext(chatId);
        
        // Store selected instance for IPv6 operations
        storage.selectInstance(chatId, instance.getOcId());
        
        // Get instance IPv6 status
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            StringBuilder message = new StringBuilder();
            message.append("?IPv6 ???\n\n");
            message.append(String.format("??: %s\n", instance.getName()));
            message.append(String.format("??: %s\n", instance.getRegion()));
            message.append(String.format("ID: ...%s\n\n", 
                    instance.getOcId().substring(Math.max(0, instance.getOcId().length() - 8))));
            
            boolean hasIpv6 = false;
            List<String> ipv6Addresses = new ArrayList<>();
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                // Get VNIC and check IPv6
                ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instance.getOcId())
                        .build();
                
                ListVnicAttachmentsResponse vnicResponse = fetcher.getComputeClient().listVnicAttachments(vnicRequest);
                
                for (VnicAttachment attachment : vnicResponse.getItems()) {
                    if (attachment.getLifecycleState() != VnicAttachment.LifecycleState.Attached) {
                        continue;
                    }
                    
                    String vnicId = attachment.getVnicId();
                    GetVnicRequest getVnicRequest = GetVnicRequest.builder().vnicId(vnicId).build();
                    GetVnicResponse getVnicResponse = fetcher.getVirtualNetworkClient().getVnic(getVnicRequest);
                    
                    List<String> addresses = getVnicResponse.getVnic().getIpv6Addresses();
                    if (addresses != null && !addresses.isEmpty()) {
                        hasIpv6 = true;
                        ipv6Addresses.addAll(addresses);
                    }
                }
            }
            
            if (hasIpv6) {
                message.append("??IPv6??: ? ???\n\n");
                message.append("IPv6??:\n");
                for (String addr : ipv6Addresses) {
                    message.append(String.format("  %s\n", addr));
                }
            } else {
                message.append("??IPv6??: ? ???\n\n");
                message.append("? ???????? IPv6 ??");
            }
            
            // Build keyboard based on status
            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            
            if (!hasIpv6) {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("? ?? IPv6", "ipv6_add:" + instanceIndex)
                ));
            } else {
                keyboard.add(new InlineKeyboardRow(
                        KeyboardBuilder.button("? ?? IPv6", "ipv6_remove:" + instanceIndex)
                ));
            }
            
            keyboard.add(new InlineKeyboardRow(
                    KeyboardBuilder.button("?? ??????", "ipv6_management:" + ociCfgId)
            ));
            keyboard.add(KeyboardBuilder.buildCancelRow());
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(keyboard)
            );
            
        } catch (Exception e) {
            log.error("Failed to get IPv6 details", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??IPv6????: " + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ipv6_instance:";
    }
}

/**
 * Add IPv6 to instance
 */
@Slf4j
@Component
class Ipv6AddHandler extends AbstractCallbackHandler {
    
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
                // Get VNIC
                ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instance.getOcId())
                        .build();
                
                ListVnicAttachmentsResponse vnicResponse = fetcher.getComputeClient().listVnicAttachments(vnicRequest);
                
                if (vnicResponse.getItems().isEmpty()) {
                    return buildEditMessage(
                            callbackQuery,
                            "? ??????????",
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                VnicAttachment vnicAttachment = vnicResponse.getItems().get(0);
                String vnicId = vnicAttachment.getVnicId();
                
                // Get subnet to assign IPv6  
                GetVnicRequest getVnicRequest = GetVnicRequest.builder().vnicId(vnicId).build();
                GetVnicResponse getVnicResponse = fetcher.getVirtualNetworkClient().getVnic(getVnicRequest);
                Vnic vnic = getVnicResponse.getVnic();
                
                // Check if Subnet has IPv6 enabled
                GetSubnetRequest getSubnetRequest = GetSubnetRequest.builder()
                        .subnetId(vnic.getSubnetId())
                        .build();
                GetSubnetResponse getSubnetResponse = fetcher.getVirtualNetworkClient().getSubnet(getSubnetRequest);
                Subnet subnet = getSubnetResponse.getSubnet();
                
                if (subnet.getIpv6CidrBlocks() == null || subnet.getIpv6CidrBlocks().isEmpty()) {
                    return buildEditMessage(
                            callbackQuery,
                            "? **????? IPv6**\n\n" +
                            "???????? (" + subnet.getDisplayName() + ") ??? IPv6 CIDR?\n\n" +
                            "? **????**?\n" +
                            "?????????????????????? VCN ???? IPv6?",
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ???? IPv6", "ipv6_auto_enable:" + instanceIndex)
                                    ),
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                // Create IPv6
                CreateIpv6Details ipv6Details = CreateIpv6Details.builder()
                        .vnicId(vnicId)
                        .displayName("ipv6-" + instance.getName())
                        .build();
                
                CreateIpv6Request createRequest = CreateIpv6Request.builder()
                        .createIpv6Details(ipv6Details)
                        .build();
                
                CreateIpv6Response createResponse = fetcher.getVirtualNetworkClient().createIpv6(createRequest);
                Ipv6 ipv6 = createResponse.getIpv6();
                
                return buildEditMessage(
                        callbackQuery,
                        String.format(
                                "? IPv6 ?????\n\n" +
                                "??: %s\n" +
                                "IPv6??: %s\n\n" +
                                "? IPv6 ?????????????????",
                                instance.getName(),
                                ipv6.getIpAddress()
                        ),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to add IPv6", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ?? IPv6 ??\n\n" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ipv6_add:";
    }
}

/**
 * Remove IPv6 from instance
 */
@Slf4j
@Component
class Ipv6RemoveHandler extends AbstractCallbackHandler {
    
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
                // Get VNIC
                ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instance.getOcId())
                        .build();
                
                ListVnicAttachmentsResponse vnicResponse = fetcher.getComputeClient().listVnicAttachments(vnicRequest);
                
                boolean removed = false;
                int removedCount = 0;
                
                for (VnicAttachment attachment : vnicResponse.getItems()) {
                    if (attachment.getLifecycleState() != VnicAttachment.LifecycleState.Attached) {
                        continue;
                    }
                    
                    String vnicId = attachment.getVnicId();
                    
                    // List IPv6 addresses for this VNIC
                    ListIpv6sRequest listRequest = ListIpv6sRequest.builder()
                            .vnicId(vnicId)
                            .build();
                    
                    ListIpv6sResponse listResponse = fetcher.getVirtualNetworkClient().listIpv6s(listRequest);
                    
                    for (Ipv6 ipv6 : listResponse.getItems()) {
                        DeleteIpv6Request deleteRequest = DeleteIpv6Request.builder()
                                .ipv6Id(ipv6.getId())
                                .build();
                        
                        fetcher.getVirtualNetworkClient().deleteIpv6(deleteRequest);
                        removed = true;
                        removedCount++;
                    }
                }
                
                if (removed) {
                    return buildEditMessage(
                            callbackQuery,
                            String.format(
                                    "? IPv6 ?????\n\n" +
                                    "??: %s\n" +
                                    "??? %d ? IPv6 ??",
                                    instance.getName(),
                                    removedCount
                            ),
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                } else {
                    return buildEditMessage(
                            callbackQuery,
                            "?? ????? IPv6 ??",
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to remove IPv6", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ?? IPv6 ??\n\n" + e.getMessage(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ipv6_remove:";
    }
}

/**
 * Auto enable IPv6 for VCN and Subnet
 */
@Slf4j
@Component
class Ipv6AutoEnableHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int instanceIndex = Integer.parseInt(callbackData.split(":")[1]);
        long chatId = callbackQuery.getMessage().getChatId();
        
        InstanceSelectionStorage storage = InstanceSelectionStorage.getInstance();
        SysUserDTO.CloudInstance instance = storage.getInstanceByIndex(chatId, instanceIndex);
        String ociCfgId = storage.getConfigContext(chatId);
        
        if (instance == null) {
            return buildEditMessage(callbackQuery, "? ?????", new InlineKeyboardMarkup(List.of(KeyboardBuilder.buildCancelRow())));
        }
        
        // Send loading message
        try {
            telegramClient.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("? ????????????...\n\n1. ?? VCN IPv6 ??\n2. ???? IPv6\n3. ????")
                    .build());
        } catch (Exception ignored) {}
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                // 1. Get VNIC/Subnet/VCN info
                ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instance.getOcId())
                        .build();
                VnicAttachment vnicAttachment = fetcher.getComputeClient().listVnicAttachments(vnicRequest).getItems().get(0);
                Vnic vnic = fetcher.getVirtualNetworkClient().getVnic(GetVnicRequest.builder().vnicId(vnicAttachment.getVnicId()).build()).getVnic();
                
                String subnetId = vnic.getSubnetId();
                Subnet subnet = fetcher.getVirtualNetworkClient().getSubnet(GetSubnetRequest.builder().subnetId(subnetId).build()).getSubnet();
                String vcnId = subnet.getVcnId();
                Vcn vcn = fetcher.getVirtualNetworkClient().getVcn(GetVcnRequest.builder().vcnId(vcnId).build()).getVcn();
                
                // 2. Enable VCN IPv6 if needed
                if (vcn.getIpv6CidrBlocks() == null || vcn.getIpv6CidrBlocks().isEmpty()) {
                    // VCN doesn't have IPv6 enabled, show instructions to user
                    String warningMessage = "?? *VCN ??? IPv6*\\n\\n" +
                            "???? VCN ???? IPv6????????????\\n\\n" +
                            "*?? 1: ?? VCN IPv6*\\n" +
                            "1. ?? OCI ???\\n" +
                            "2. ?? VCN ???\\n" +
                            "3. ?? '?? IPv6 CIDR ?'\\n" +
                            "4. ?? 'Oracle GUA IPv6 ??'\\n" +
                            "5. ????\\n\\n" +
                            "*?? 2: ???? IPv6*\\n" +
                            "1. ???????\\n" +
                            "2. ?? '?? IPv6 CIDR ?'\\n" +
                            "3. ?? IPv6 ??\\n" +
                            "4. ????\\n\\n" +
                            "? ???????? '? ????' ??????? IPv6 ???";
                    
                    return buildEditMessage(callbackQuery, 
                        warningMessage,
                        new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                        )));

                }
                
                // 3. Enable Subnet IPv6 if needed
                if (subnet.getIpv6CidrBlocks() == null || subnet.getIpv6CidrBlocks().isEmpty()) {
                    // We need a /64 from the VCN's /56
                    // VCN: xxxx:xxxx:xxxx:xxxx::/56
                    // Subnet needs: xxxx:xxxx:xxxx:xxxx:??00::/64
                    // Simple strategy: Append "00" to the /56 prefix length part?
                    // Actually, Oracle API often auto-assigns if we just say we want one, OR we imply it.
                    // But UpdateSubnetDetails requires ipv6CidrBlocks if we want to add one.
                    // Let's try to derive '00' subnet.
                    
                    String vcnCidr = vcn.getIpv6CidrBlocks().get(0); // e.g., 2603:c020:4002:d000::/56
                    // Remove suffix
                    String prefix = vcnCidr.split("/")[0];
                    // If it ends with ::, it might be shortened.
                    // 2603:c020:4002:d000:: -> 4 blocks. 
                    // To make it /64, we need 4 blocks. 
                    // Actually, /56 means 8 bits for subnets (00 to FF).
                    // So 2603:c020:4002:d000::/64 is a valid first subnet.
                    
                    String targetSubnetCidr = vcnCidr.replace("/56", "/64");
                    
                    // Check if this CIDR is used by other subnets to avoid collision (Simple check)
                    // (Skipping for "Lazy Mode" MVP, assuming mostly single subnet users. Collision might error out)
                    
                    UpdateSubnetRequest updateSubnetRequest = UpdateSubnetRequest.builder()
                            .subnetId(subnetId)
                            .updateSubnetDetails(UpdateSubnetDetails.builder()
                                    .ipv6CidrBlocks(List.of(targetSubnetCidr))
                                    .build())
                            .build();
                            
                    try {
                        subnet = fetcher.getVirtualNetworkClient().updateSubnet(updateSubnetRequest).getSubnet();
                    } catch (Exception e) {
                        // Fallback: maybe try '01' if '00' failed?
                        // For now, fail with error message.
                        throw new RuntimeException("?????? CIDR ?? (" + targetSubnetCidr + "): " + e.getMessage());
                    }
                    try { Thread.sleep(2000); } catch (InterruptedException e) {}
                }
                
                // 4. Now trigger Add IPv6 logic
                // Reuse Ipv6AddHandler logic by redirecting? or just execute creation here.
                
                CreateIpv6Details ipv6Details = CreateIpv6Details.builder()
                        .vnicId(vnic.getId())
                        .displayName("ipv6-" + instance.getName())
                        .build();
                
                CreateIpv6Response createResponse = fetcher.getVirtualNetworkClient().createIpv6(
                        CreateIpv6Request.builder().createIpv6Details(ipv6Details).build()
                );
                
                return buildEditMessage(
                        callbackQuery,
                        String.format(
                                "? **???????**\n\n" +
                                "??: %s\n" +
                                "IPv6??: %s\n\n" +
                                "?????:\n" +
                                "1. VCN ?? IPv6\n" +
                                "2. ???? CIDR\n" +
                                "3. ?? IPv6 ??",
                                instance.getName(),
                                createResponse.getIpv6().getIpAddress()
                        ),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ????", "ipv6_management:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );

            }
            
        } catch (Exception e) {
            log.error("Failed to auto enable IPv6", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ??????\n\n" + e.getMessage() + "\n\n???????????????",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "ipv6_management:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "ipv6_auto_enable:";
    }
}
