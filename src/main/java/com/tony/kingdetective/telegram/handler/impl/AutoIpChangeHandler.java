package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.ComputeClient;
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
import java.util.List;

/**
 * Auto IP change monitoring handler - monitor and auto change public IP
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class AutoIpChangeHandler extends AbstractCallbackHandler {
    
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
            
            return buildAutoIpInstanceListMessage(callbackQuery, instances, ociCfgId, chatId);
            
        } catch (Exception e) {
            log.error("Failed to list instances for auto IP change", e);
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
    
    private BotApiMethod<? extends Serializable> buildAutoIpInstanceListMessage(
            CallbackQuery callbackQuery,
            List<SysUserDTO.CloudInstance> instances,
            String ociCfgId,
            long chatId) {
        
        StringBuilder message = new StringBuilder("???-???IP?\n\n");
        message.append(String.format("? %d ???????\n", instances.size()));
        message.append("??????IP???\n\n");
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        for (int i = 0; i < instances.size(); i++) {
            SysUserDTO.CloudInstance instance = instances.get(i);
            
            String publicIps = CollectionUtil.isEmpty(instance.getPublicIp())
                    ? "???IP"
                    : String.join(", ", instance.getPublicIp());
            
            message.append(String.format(
                    "%d. %s\n" +
                    "   ??IP: %s\n" +
                    "   ??: %s\n\n",
                    i + 1,
                    instance.getName(),
                    publicIps,
                    instance.getRegion()
            ));
            
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(KeyboardBuilder.button(
                    String.format("? ??%d", i + 1),
                    "change_ip_instance:" + i
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
        return "auto_ip_change:";
    }
}

/**
 * Change IP for selected instance
 */
@Slf4j
@Component
class ChangeIpInstanceHandler extends AbstractCallbackHandler {
    
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
                ComputeClient computeClient = fetcher.getComputeClient();
                
                // Get VNIC attachments
                ListVnicAttachmentsRequest vnicRequest = ListVnicAttachmentsRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .instanceId(instance.getOcId())
                        .build();
                
                ListVnicAttachmentsResponse vnicResponse = computeClient.listVnicAttachments(vnicRequest);
                
                if (vnicResponse.getItems().isEmpty()) {
                    return buildEditMessage(
                            callbackQuery,
                            "? ???????",
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "auto_ip_change:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                VnicAttachment vnicAttachment = vnicResponse.getItems().get(0);
                String vnicId = vnicAttachment.getVnicId();
                
                // Get VNIC to find public IP
                GetVnicRequest getVnicRequest = GetVnicRequest.builder().vnicId(vnicId).build();
                GetVnicResponse getVnicResponse = fetcher.getVirtualNetworkClient().getVnic(getVnicRequest);
                Vnic vnic = getVnicResponse.getVnic();
                
                String currentPublicIp = vnic.getPublicIp();
                
                // List public IPs attached to this VNIC
                com.oracle.bmc.core.requests.ListPublicIpsRequest listIpRequest = 
                        com.oracle.bmc.core.requests.ListPublicIpsRequest.builder()
                                .scope(com.oracle.bmc.core.requests.ListPublicIpsRequest.Scope.Region)
                                .compartmentId(fetcher.getCompartmentId())
                                .build();
                
                com.oracle.bmc.core.responses.ListPublicIpsResponse listIpResponse = 
                        fetcher.getVirtualNetworkClient().listPublicIps(listIpRequest);
                
                // Get PrivateIp object to get its OCID
                ListPrivateIpsRequest listPrivateIpsRequest = ListPrivateIpsRequest.builder()
                        .vnicId(vnic.getId())
                        .build();
                ListPrivateIpsResponse listPrivateIpsResponse = fetcher.getVirtualNetworkClient().listPrivateIps(listPrivateIpsRequest);
                
                if (listPrivateIpsResponse.getItems().isEmpty()) {
                     return buildEditMessage(
                            callbackQuery,
                            "? ?????IP??",
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "auto_ip_change:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                com.oracle.bmc.core.model.PrivateIp privateIpObj = listPrivateIpsResponse.getItems().get(0);
                String privateIpOcid = privateIpObj.getId();

                // Find public IP assigned to this private IP
                com.oracle.bmc.core.model.PublicIp targetPublicIp = null;
                for (com.oracle.bmc.core.model.PublicIp publicIp : listIpResponse.getItems()) {
                    if (publicIp.getAssignedEntityId() != null && 
                        publicIp.getAssignedEntityId().equals(privateIpOcid)) {
                        targetPublicIp = publicIp;
                        break;
                    }
                }

                if (targetPublicIp == null) {
                    return buildEditMessage(
                            callbackQuery,
                            String.format(
                                    "?? ?????IP??\n\n" +
                                    "??: %s\n" +
                                    "??IP: %s\n\n" +
                                    "? ???????????IP\n" +
                                    "? ???????IP",
                                    instance.getName(),
                                    currentPublicIp != null ? currentPublicIp : "?"
                            ),
                            new InlineKeyboardMarkup(List.of(
                                    new InlineKeyboardRow(
                                            KeyboardBuilder.button("?? ??", "auto_ip_change:" + ociCfgId)
                                    ),
                                    KeyboardBuilder.buildCancelRow()
                            ))
                    );
                }
                
                // Unassign and reassign to get new IP
                // First, unassign
                com.oracle.bmc.core.requests.UpdatePublicIpRequest unassignRequest = 
                        com.oracle.bmc.core.requests.UpdatePublicIpRequest.builder()
                                .publicIpId(targetPublicIp.getId())
                                .updatePublicIpDetails(
                                        com.oracle.bmc.core.model.UpdatePublicIpDetails.builder()
                                                .privateIpId(null)
                                                .build()
                                )
                                .build();
                
                fetcher.getVirtualNetworkClient().updatePublicIp(unassignRequest);
                
                // Wait a moment
                Thread.sleep(2000);
                
                // Reassign
                com.oracle.bmc.core.requests.UpdatePublicIpRequest reassignRequest = 
                        com.oracle.bmc.core.requests.UpdatePublicIpRequest.builder()
                                .publicIpId(targetPublicIp.getId())
                                .updatePublicIpDetails(
                                        com.oracle.bmc.core.model.UpdatePublicIpDetails.builder()
                                                .privateIpId(vnic.getPrivateIp())
                                                .build()
                                )
                                .build();
                
                fetcher.getVirtualNetworkClient().updatePublicIp(reassignRequest);
                
                // Get new IP
                Thread.sleep(1000);
                GetVnicResponse newVnicResponse = fetcher.getVirtualNetworkClient().getVnic(getVnicRequest);
                String newPublicIp = newVnicResponse.getVnic().getPublicIp();
                
                return buildEditMessage(
                        callbackQuery,
                        String.format(
                                "? IP?????\n\n" +
                                "??: %s\n" +
                                "?IP: %s\n" +
                                "?IP: %s\n\n" +
                                "? ?IP???",
                                instance.getName(),
                                currentPublicIp != null ? currentPublicIp : "?",
                                newPublicIp != null ? newPublicIp : "???..."
                        ),
                        new InlineKeyboardMarkup(List.of(
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("? ????", "change_ip_instance:" + instanceIndex)
                                ),
                                new InlineKeyboardRow(
                                        KeyboardBuilder.button("?? ??", "auto_ip_change:" + ociCfgId)
                                ),
                                KeyboardBuilder.buildCancelRow()
                        ))
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to change IP", e);
            return buildEditMessage(
                    callbackQuery,
                    "? IP????\n\n" + e.getMessage() + "\n\n? ??: ?????????IP",
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("?? ??", "auto_ip_change:" + ociCfgId)
                            ),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "change_ip_instance:";
    }
}

/**
 * Auto IP change config selector
 */
@Slf4j
@Component
class AutoIpChangeConfigSelectHandler extends AbstractCallbackHandler {
    
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
            message.append("???-???IP?\n\n");
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
                                "auto_ip_change:" + user.getOciCfg().getId()
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
        return "auto_ip_change_select";
    }
}
