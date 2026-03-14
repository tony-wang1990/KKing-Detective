package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.core.model.Instance;
import com.tony.kingdetective.bean.Tuple2;
import com.oracle.bmc.core.model.Vnic;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IInstanceService;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *   Handler
 *  Markdown 
 *
 * @author Tony Wang
 */
@Slf4j
@Component
public class GlobalInstanceSummaryHandler extends AbstractCallbackHandler {

    @Override
    public boolean canHandle(String callbackData) {
        return "global_instance_summary".equals(callbackData);
    }

    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        // 1. 
        buildEditMessage(callbackQuery, "🚀 正在并行扫描所有区域的实例，可能需要几十秒，请稍候...", null);

        // 2.  TelegramClient 
        CompletableFuture.runAsync(() -> doGlobalSummary(chatId, telegramClient));
        
        //  BotApiMethod
        return null;
    }

    private void doGlobalSummary(long chatId, TelegramClient telegramClient) {
        TimeInterval timer = DateUtil.timer();
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        IInstanceService instanceService = SpringUtil.getBean(IInstanceService.class);

        List<OciUser> users = userService.getEnabledOciUserList();
        if (users == null || users.isEmpty()) {
            sendMarkdownMessage(chatId, "❌ 暂无可用账户，请先添加 OCI 账户", telegramClient);
            return;
        }

        // 
        List<CompletableFuture<AccountSummary>> futures = users.stream()
            .map(user -> CompletableFuture.supplyAsync(() -> queryAccount(user, instanceService)))
            .collect(Collectors.toList());

        List<AccountSummary> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // 
        StringBuilder sb = new StringBuilder("🌍 *全局实例资产汇总*\n");
        sb.append(String.format("— 耗时: `%.1f 秒`\n\n", timer.intervalMs() / 1000.0));

        int totalAccounts = users.size();
        int errorAccounts = 0;
        int totalRunning = 0;
        int totalStopped = 0;

        for (AccountSummary summary : results) {
            sb.append("👤 *账户*: `").append(summary.user.getUsername()).append("` (").append(summary.user.getOciRegion()).append(")\n");
            
            if (summary.error != null) {
                sb.append("   ⚠️ `获取失败: ").append(truncateString(summary.error, 30)).append("`\n\n");
                errorAccounts++;
                continue;
            }

            if (summary.instances.isEmpty()) {
                sb.append("   暂无实例\n\n");
                continue;
            }

            for (Tuple2<Instance, String> dto : summary.instances) {
                String state = dto.getFirst().getLifecycleState().getValue();
                String ip = dto.getSecond() != null && !dto.getSecond().isEmpty() ? dto.getSecond() : "无公网";
                String shape = dto.getFirst().getShape();
                if (shape.contains("Micro")) shape = "ARM";
                else if (shape.contains("E4") || shape.contains("E3")) shape = "AMD";

                boolean isRunning = "RUNNING".equals(state);
                if (isRunning) totalRunning++;
                else totalStopped++;

                sb.append("   ").append(isRunning ? "✅" : "⏸")
                  .append(" `").append(truncateString(dto.getFirst().getDisplayName(), 12)).append("`")
                  .append(" | `").append(ip).append("`")
                  .append(" | `").append(shape).append("`\n");
            }
            sb.append("\n");
        }

        sb.append("📊 *总计*\n");
        sb.append("账户数: `").append(totalAccounts).append("`");
        if (errorAccounts > 0) sb.append(" [⚠️").append(errorAccounts).append("失败]");
        sb.append("\n");
        sb.append("运行中: `").append(totalRunning).append("` 台\n");
        sb.append("已停止: `").append(totalStopped).append("` 台\n");

        sendMarkdownMessage(chatId, sb.toString(), telegramClient);
    }

    private AccountSummary queryAccount(OciUser user, IInstanceService instanceService) {
        AccountSummary summary = new AccountSummary(user);
        try {
            SysUserDTO dto = buildSysUserDTO(user);
            // IInstanceService  listRunningInstances 
            //  OracleInstanceFetcher
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
                List<Instance> instances = fetcher.getComputeClient()
                    .listInstances(com.oracle.bmc.core.requests.ListInstancesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .build())
                    .getItems();

                for (Instance instance : instances) {
                    if ("TERMINATED".equals(instance.getLifecycleState().getValue()) || 
                        "TERMINATING".equals(instance.getLifecycleState().getValue())) {
                        continue; // 
                    }
                    
                    // Public IP
                    String publicIp = "无公网";
                    try {
                        var vnicAttachments = fetcher.getComputeClient().listVnicAttachments(
                            com.oracle.bmc.core.requests.ListVnicAttachmentsRequest.builder()
                                .compartmentId(fetcher.getCompartmentId())
                                .instanceId(instance.getId())
                                .build()
                        ).getItems();
                        if (!vnicAttachments.isEmpty()) {
                            Vnic vnic = fetcher.getVirtualNetworkClient().getVnic(
                                com.oracle.bmc.core.requests.GetVnicRequest.builder()
                                    .vnicId(vnicAttachments.get(0).getVnicId())
                                    .build()
                            ).getVnic();
                            if (vnic.getPublicIp() != null) {
                                publicIp = vnic.getPublicIp();
                            }
                        }
                    } catch (Exception ignore) {}
                    
                    summary.instances.add(new Tuple2<>(instance, publicIp));
                }
            }
        } catch (Exception e) {
            log.error("Failed to query instance for account: {}", user.getUsername(), e);
            summary.error = e.getMessage();
        }
        return summary;
    }

    private void sendMarkdownMessage(long chatId, String text, TelegramClient telegramClient) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendMessage message = 
                org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(new InlineKeyboardMarkup(List.of(
                    new InlineKeyboardRow(KeyboardBuilder.buildBackToMainMenuRow())
                )))
                .build();
            telegramClient.execute(message);
        } catch (Exception e) {
            log.error("Failed to send global instance summary", e);
        }
    }

    private SysUserDTO buildSysUserDTO(OciUser user) {
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

    private String truncateString(String s, int maxLength) {
        if (s == null) return "N/A";
        return s.length() > maxLength ? s.substring(0, maxLength) + ".." : s;
    }

    private static class AccountSummary {
        OciUser user;
        String error;
        List<Tuple2<Instance, String>> instances = new ArrayList<>();

        AccountSummary(OciUser user) { this.user = user; }
    }
}
