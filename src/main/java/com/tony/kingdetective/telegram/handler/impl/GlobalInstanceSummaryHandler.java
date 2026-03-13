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
 * 🌍 全局实例汇总 Handler
 * 功能：一次性查询所有账户下的所有在线实例，生成排版良好的 Markdown 汇总表格
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
        
        // 1. 发送提示消息：后台正在查询请稍候
        buildEditMessage(callbackQuery, "🚀 正在并行扫描所有区域的实例，可能需要几十秒，请稍候...", null);

        // 2. 异步执行扫瞄，并通过 TelegramClient 主动发消息更新结果
        CompletableFuture.runAsync(() -> doGlobalSummary(chatId, telegramClient));
        
        // 由于使用异步响应，这里不返回 BotApiMethod，只让编辑消息立刻生效
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

        // 并行查询每个账户的实例
        List<CompletableFuture<AccountSummary>> futures = users.stream()
            .map(user -> CompletableFuture.supplyAsync(() -> queryAccount(user, instanceService)))
            .collect(Collectors.toList());

        List<AccountSummary> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // 拼接报告
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

            for (InstanceCfgDTO dto : summary.instances) {
                String state = dto.getInstance().getLifecycleState().getValue();
                String ip = dto.getPublicIp() != null && !dto.getPublicIp().isEmpty() ? dto.getPublicIp() : "无公网";
                String shape = dto.getInstance().getShape();
                if (shape.contains("Micro")) shape = "ARM";
                else if (shape.contains("E4") || shape.contains("E3")) shape = "AMD";

                boolean isRunning = "RUNNING".equals(state);
                if (isRunning) totalRunning++;
                else totalStopped++;

                sb.append("   ").append(isRunning ? "✅" : "⏸")
                  .append(" `").append(truncateString(dto.getInstance().getDisplayName(), 12)).append("`")
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
            // IInstanceService 默认方法 listRunningInstances 其实会列出所有状态的实例，只是名字叫那个
            // 包装了 OracleInstanceFetcher
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(dto)) {
                List<Instance> instances = fetcher.getComputeClient()
                    .listInstances(com.oracle.bmc.core.requests.ListInstancesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .build())
                    .getItems();

                for (Instance instance : instances) {
                    if ("TERMINATED".equals(instance.getLifecycleState().getValue()) || 
                        "TERMINATING".equals(instance.getLifecycleState().getValue())) {
                        continue; // 过滤掉已终止的
                    }
                    
                    // 获取Public IP
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
