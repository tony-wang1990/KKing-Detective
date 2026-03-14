package com.tony.kingdetective.telegram.builder;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Telegram Bot ?
 *
 * @author Tony Wang
 */
public class KeyboardBuilder {

    /**
     * 4?
     *
     * @return ?
     */
    public static List<InlineKeyboardRow> buildMainMenu() {
        return Arrays.asList(
                // 
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🚀 一键抢?")
                                .callbackData("config_list")
                                .build()
                ),
                
                // ==========  +  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("快捷开?")
                                .callbackData("quick_start")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("一键测?")
                                .callbackData("check_alive")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("实例升降?")
                                .callbackData("shape_change_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("账户管理")
                                .callbackData("account_management")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("自动换IP")
                                .callbackData("auto_ip_change_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("开放端?")
                                .callbackData("open_all_ports_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("SSH管理")
                                .callbackData("ssh_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("IPv6管理")
                                .callbackData("ipv6_config_select")
                                .build()
                ),
                
                // ==========  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("配额查询")
                                .callbackData("quota_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("消费查询")
                                .callbackData("cost_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("资源占用")
                                .callbackData("instance_resource_usage_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("内存占用")
                                .callbackData("memory_occupy_select")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("流量历史")
                                .callbackData("traffic_history")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("流量统计")
                                .callbackData("traffic_statistics")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Profile管理")
                                .callbackData("profile_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("区域拓展")
                                .callbackData("auto_region_expansion")
                                .build()
                ),
                
                // ========== ?+  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("监控通知")
                                .callbackData("instance_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("监控自启")
                                .callbackData("auto_restart_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("每日日报")
                                .callbackData("daily_report")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("任务管理")
                                .callbackData("task_management")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("安全管理")
                                .callbackData("security_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("MFA管理")
                                .callbackData("mfa_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("清除2FA")
                                .callbackData("clear_2fa_devices")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("禁用被封?")
                                .callbackData("disable_banned_accounts")
                                .build()
                ),
                
                // ========== ?==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("批量查邮")
                                .callbackData("batch_email_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("订阅信息")
                                .callbackData("subscription_info")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("版本信息")
                                .callbackData("version_info")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("日志查询")
                                .callbackData("log_query")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("VNC配置")
                                .callbackData("vnc_config")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("备份恢复")
                                .callbackData("backup_restore")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("AI聊天")
                                .callbackData("ai_chat")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("通知频道")
                                .url("https://t.me/Woci_detective")
                                .build()
                ),
                
                // ==========  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("放货查询")
                                .url("https://check.oci-helper.de5.net")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("开源地址（帮忙点点star⭐）")
                                .url("https://github.com/tony-wang1990/king-detective")
                                .build()
                ),
                
                // 
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("?关闭窗口")
                                .callbackData("cancel")
                                .build()
                )
        );
    }

    /**
     * 
     *
     * @param accounts 
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup buildAccountSelectionKeyboard(List<String> accounts) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        for (String accountId : accounts) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(InlineKeyboardButton.builder()
                    .text(accountId)
                    .callbackData("account:" + accountId)
                    .build());
            keyboard.add(row);
        }

        // 
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text("« 返回主菜?")
                .callbackData("back_to_main")
                .build());
        keyboard.add(backRow);

        return new InlineKeyboardMarkup(keyboard);
    }

    /**
     * 
     *
     * @param confirmCallback 
     * @param cancelCallback  
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup buildConfirmationKeyboard(String confirmCallback, String cancelCallback) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text("?确认")
                .callbackData(confirmCallback)
                .build());
        row.add(InlineKeyboardButton.builder()
                .text("?取消")
                .callbackData(cancelCallback)
                .build());

        keyboard.add(row);
        return new InlineKeyboardMarkup(keyboard);
    }

    /**
     * 
     *
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup buildBackKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text("« 返回主菜?")
                .callbackData("back_to_main")
                .build());

        keyboard.add(row);
        return new InlineKeyboardMarkup(keyboard);
    }

    /**
     * ?
     *
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup buildEmptyKeyboard() {
        return new InlineKeyboardMarkup(new ArrayList<>());
    }

    /**
     * 
     *
     * @param rows ?
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup fromRows(List<InlineKeyboardRow> rows) {
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 
     *
     * @return InlineKeyboardRow
     */
    public static InlineKeyboardRow buildCancelRow() {
        return new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("?关闭窗口")
                        .callbackData("cancel")
                        .build()
        );
    }

    /**
     * 
     *
     * @return InlineKeyboardRow
     */
    public static InlineKeyboardRow buildBackToMainMenuRow() {
        return new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("« 返回主菜?")
                        .callbackData("cancel")
                        .build()
        );
    }

    /**
     * ?
     *
     * @param currentPage ?
     * @param totalPages ?
     * @param prevCallback ?
     * @param nextCallback ?
     * @return InlineKeyboardRow
     */
    public static InlineKeyboardRow buildPaginationRow(int currentPage, int totalPages, String prevCallback, String nextCallback) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        
        if (currentPage > 1) {
            row.add(InlineKeyboardButton.builder()
                    .text("◀?上一?")
                    .callbackData(prevCallback)
                    .build());
        }
        
        row.add(InlineKeyboardButton.builder()
                .text(currentPage + "/" + totalPages)
                .callbackData("page_info")
                .build());
        
        if (currentPage < totalPages) {
            row.add(InlineKeyboardButton.builder()
                    .text("下一?▶️")
                    .callbackData(nextCallback)
                    .build());
        }
        
        return row;
    }

    /**
     * ?
     *
     * @param text 
     * @param callbackData 
     * @return InlineKeyboardButton
     */
    public static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    /**
     * URL
     *
     * @param text 
     * @param url URL
     * @return InlineKeyboardButton
     */
    public static InlineKeyboardButton urlButton(String text, String url) {
        return InlineKeyboardButton.builder()
                .text(text)
                .url(url)
                .build();
    }
}
