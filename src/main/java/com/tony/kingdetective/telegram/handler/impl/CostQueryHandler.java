package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.oracle.bmc.usageapi.UsageapiClient;
import com.oracle.bmc.usageapi.model.*;
import com.oracle.bmc.usageapi.requests.RequestSummarizedUsagesRequest;
import com.oracle.bmc.usageapi.responses.RequestSummarizedUsagesResponse;
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
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Cost query handler - shows spending for last 3 months
 * 
 * @author antigravity-ai
 */
@Slf4j
@Component
public class CostQueryHandler extends AbstractCallbackHandler {
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        long chatId = callbackQuery.getMessage().getChatId();
        
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        
        try {
            List<SysUserDTO> users = sysService.list();
            if (users == null || users.isEmpty()) {
                return buildEditMessage(
                        callbackQuery,
                        "? ????? OCI ??\n\n???? OCI ??",
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            // Use first config
            SysUserDTO user = users.get(0);
            
            StringBuilder message = new StringBuilder();
            message.append("??3??????\n\n");
            message.append(String.format("??: %s\n", user.getUsername()));
            message.append(String.format("??: %s\n\n", user.getOciCfg().getRegion()));
            message.append("????????????????\n\n");
            
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(user)) {
                UsageapiClient usageClient = UsageapiClient.builder()
                        .build(fetcher.getAuthenticationDetailsProvider());
                
                // Calculate date range (last 3 months) in UTC
                LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
                LocalDate startDate = endDate.minusMonths(3);
                
                // Query usage data
                RequestSummarizedUsagesDetails requestDetails = RequestSummarizedUsagesDetails.builder()
                        .tenantId(user.getOciCfg().getTenantId())
                        // OCI Usage API requires start/end time to be 00:00:00 UTC
                        .timeUsageStarted(Date.from(startDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                        .timeUsageEnded(Date.from(endDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                        .granularity(RequestSummarizedUsagesDetails.Granularity.Monthly)
                        .queryType(RequestSummarizedUsagesDetails.QueryType.Cost)
                        .groupBy(Arrays.asList("service"))
                        .build();
                
                RequestSummarizedUsagesRequest request = RequestSummarizedUsagesRequest.builder()
                        .requestSummarizedUsagesDetails(requestDetails)
                        .build();
                
                RequestSummarizedUsagesResponse response = usageClient.requestSummarizedUsages(request);
                
                List<UsageSummary> usageSummaries = response.getUsageAggregation().getItems();
                
                if (usageSummaries == null || usageSummaries.isEmpty()) {
                    message.append("? ?3????????\n\n");
                    message.append("? ?????\n");
                    message.append("  ? ????????\n");
                    message.append("  ? ???????\n");
                    message.append("  ? ??????\n");
                } else {
                    // Group by month
                    Map<String, Map<String, BigDecimal>> monthlyCosts = new TreeMap<>();
                    BigDecimal totalCost = BigDecimal.ZERO;
                    
                    for (UsageSummary summary : usageSummaries) {
                        String month = formatMonth(summary.getTimeUsageStarted());
                        String service = summary.getService() != null ? summary.getService() : "??";
                        BigDecimal cost = summary.getComputedAmount() != null 
                                ? summary.getComputedAmount() 
                                : BigDecimal.ZERO;
                        
                        monthlyCosts
                                .computeIfAbsent(month, k -> new LinkedHashMap<>())
                                .merge(service, cost, BigDecimal::add);
                        
                        totalCost = totalCost.add(cost);
                    }
                    
                    // Display monthly breakdown
                    for (Map.Entry<String, Map<String, BigDecimal>> monthEntry : monthlyCosts.entrySet()) {
                        String month = monthEntry.getKey();
                        Map<String, BigDecimal> services = monthEntry.getValue();
                        
                        BigDecimal monthTotal = services.values().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        message.append(String.format("? %s\n", month));
                        message.append(String.format("??: $%.2f\n\n", monthTotal));
                        
                        // Top 5 services
                        services.entrySet().stream()
                                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                                .limit(5)
                                .forEach(entry -> {
                                    message.append(String.format("  ? %s: $%.2f\n", 
                                            entry.getKey(), 
                                            entry.getValue()));
                                });
                        
                        message.append("\n");
                    }
                    
                    message.append("????????????????\n");
                    message.append(String.format("? 3????: $%.2f\n", totalCost));
                    message.append(String.format("? ????: $%.2f\n", 
                            totalCost.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)));
                }
                
                usageClient.close();
                
            } catch (Exception e) {
                log.error("Failed to query cost data", e);
                return buildEditMessage(
                        callbackQuery,
                        "? ??????\n\n" + e.getMessage(),
                        new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
                );
            }
            
            message.append("\n? ???? OCI Usage API");
            
            return buildEditMessage(
                    callbackQuery,
                    message.toString(),
                    new InlineKeyboardMarkup(List.of(
                            KeyboardBuilder.buildBackToMainMenuRow(),
                            KeyboardBuilder.buildCancelRow()
                    ))
            );
            
        } catch (Exception e) {
            log.error("Failed to handle cost query", e);
            return buildEditMessage(
                    callbackQuery,
                    "? ????: " + e.getMessage(),
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
    }
    
    /**
     * Format date to month string
     */
    private String formatMonth(Date date) {
        if (date == null) {
            return "??";
        }
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.format(DateTimeFormatter.ofPattern("yyyy?MM?"));
    }
    
    @Override
    public String getCallbackPattern() {
        return "cost_query";
    }
}
