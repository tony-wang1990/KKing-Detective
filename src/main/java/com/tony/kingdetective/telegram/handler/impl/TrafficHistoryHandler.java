package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.*;
import com.oracle.bmc.monitoring.requests.SummarizeMetricsDataRequest;
import com.oracle.bmc.monitoring.responses.SummarizeMetricsDataResponse;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Traffic history handler - enhanced traffic statistics with time range selection
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class TrafficHistoryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        // Show time range selection
        return buildEditMessage(
                callbackQuery,
                "【流量历史查询】\n\n" +
                "请选择查询时间范围：",
                new InlineKeyboardMarkup(List.of(
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("📊 近1个月", "traffic_history:30")
                        ),
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("📊 近3个月", "traffic_history:90")
                        ),
                        new InlineKeyboardRow(
                                KeyboardBuilder.button("📊 近6个月", "traffic_history:180")
                        ),
                        KeyboardBuilder.buildBackToMainMenuRow(),
                        KeyboardBuilder.buildCancelRow()
                ))
        );
    }
    
    @Override
    public String getCallbackPattern() {
        return "traffic_history";
    }
}

/**
 * Traffic history query handler with specific time range
 */
@Slf4j
@Component
class TrafficHistoryQueryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String callbackData = callbackQuery.getData();
        int days = Integer.parseInt(callbackData.split(":")[1]);
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            if (CollectionUtil.isEmpty(users)) {
                return buildEditMessage(
                        callbackQuery,
                        "❌ 未找到任何 OCI 配置",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append(String.format("【流量历史 - 近%d天】\n\n", days));
            
            Date endTime = Date.from(Instant.now());
            Date startTime = Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
            
            for (SysUserDTO user : users) {
                try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                    message.append(String.format("📌 %s (%s)\n", 
                            user.getUsername(), 
                            user.getOciCfg().getRegion()));
                    
                    // Query traffic metrics
                    TrafficStats stats = queryTrafficMetrics(fetcher, startTime, endTime);
                    
                    if (stats.hasData) {
                        message.append(String.format("⬇ 入站: %s\n", formatBytes(stats.inboundBytes)));
                        message.append(String.format("⬆ 出站: %s\n", formatBytes(stats.outboundBytes)));
                        message.append(String.format("📊 总计: %s\n", formatBytes(stats.totalBytes)));
                    } else {
                        message.append("📊 暂无流量数据\n");
                    }
                    
                    message.append("\n");
                    
                } catch (Exception e) {
                    log.error("Failed to query traffic for user: {}", user.getUsername(), e);
                    message.append(String.format("❌ 查询失败: %s\n\n", e.getMessage()));
                }
            }
            
            message.append("━━━━━━━━━━━━━━━━\n");
            message.append("💡 数据来自 OCI Monitoring API");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            new InlineKeyboardRow(
                                    KeyboardBuilder.button("🔄 刷新", "traffic_history:" + days),
                                    KeyboardBuilder.button("◀️ 返回", "traffic_history")
                            ),
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to query traffic history", e);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 查询失败: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    /**
     * Query traffic metrics from OCI Monitoring
     */
    private TrafficStats queryTrafficMetrics(OracleInstanceFetcher fetcher, Date startTime, Date endTime) {
        TrafficStats stats = new TrafficStats();
        
        try {
            MonitoringClient monitoringClient = fetcher.getMonitoringClient();
            String compartmentId = fetcher.getCompartmentId();
            
            // Query network bytes in (inbound)
            SummarizeMetricsDataDetails inboundDetails = SummarizeMetricsDataDetails.builder()
                    .namespace("oci_vcn")
                    .query("VnicFromNetworkBytes[1m].sum()")
                    .startTime(startTime)
                    .endTime(endTime)
                    .resolution("1h")
                    .build();
            
            SummarizeMetricsDataRequest inboundRequest = SummarizeMetricsDataRequest.builder()
                    .compartmentId(compartmentId)
                    .summarizeMetricsDataDetails(inboundDetails)
                    .build();
            
            SummarizeMetricsDataResponse inboundResponse = monitoringClient.summarizeMetricsData(inboundRequest);
            
            // Query network bytes out (outbound)
            SummarizeMetricsDataDetails outboundDetails = SummarizeMetricsDataDetails.builder()
                    .namespace("oci_vcn")
                    .query("VnicToNetworkBytes[1m].sum()")
                    .startTime(startTime)
                    .endTime(endTime)
                    .resolution("1h")
                    .build();
            
            SummarizeMetricsDataRequest outboundRequest = SummarizeMetricsDataRequest.builder()
                    .compartmentId(compartmentId)
                    .summarizeMetricsDataDetails(outboundDetails)
                    .build();
            
            SummarizeMetricsDataResponse outboundResponse = monitoringClient.summarizeMetricsData(outboundRequest);
            
            // Calculate totals
            stats.inboundBytes = calculateTotal(inboundResponse.getItems());
            stats.outboundBytes = calculateTotal(outboundResponse.getItems());
            stats.totalBytes = stats.inboundBytes + stats.outboundBytes;
            stats.hasData = true;
            
        } catch (Exception e) {
            log.warn("Failed to query metrics: {}", e.getMessage());
            stats.hasData = false;
        }
        
        return stats;
    }
    
    /**
     * Calculate total bytes from metric data
     */
    private long calculateTotal(List<MetricData> metricDataList) {
        if (metricDataList == null || metricDataList.isEmpty()) {
            return 0;
        }
        
        long total = 0;
        for (MetricData metricData : metricDataList) {
            List<AggregatedDatapoint> datapoints = metricData.getAggregatedDatapoints();
            if (datapoints != null) {
                for (AggregatedDatapoint dp : datapoints) {
                    if (dp.getValue() != null) {
                        total += dp.getValue().longValue();
                    }
                }
            }
        }
        return total;
    }
    
    /**
     * Format bytes to human readable string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    @Override
    public String getCallbackPattern() {
        return "traffic_history:";
    }
    
    /**
     * Traffic statistics container
     */
    private static class TrafficStats {
        long inboundBytes = 0;
        long outboundBytes = 0;
        long totalBytes = 0;
        boolean hasData = false;
    }
}
