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
                                .text("🚀 一键抢机")
                                .callbackData("config_list")
                                .build()
                ),
                
                // ==========  +  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("quick_start")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("check_alive")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("?????")
                                .callbackData("shape_change_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("account_management")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("???IP")
                                .callbackData("auto_ip_change_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("open_all_ports_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("SSH??")
                                .callbackData("ssh_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("IPv6??")
                                .callbackData("ipv6_config_select")
                                .build()
                ),
                
                // ==========  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("quota_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("cost_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("instance_resource_usage_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("memory_occupy_select")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("traffic_history")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("traffic_statistics")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Profile??")
                                .callbackData("profile_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("auto_region_expansion")
                                .build()
                ),
                
                // ========== ?+  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("instance_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("auto_restart_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("daily_report")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("task_management")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("security_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("MFA??")
                                .callbackData("mfa_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("??2FA")
                                .callbackData("clear_2fa_devices")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("?????")
                                .callbackData("disable_banned_accounts")
                                .build()
                ),
                
                // ========== ?==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("batch_email_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("subscription_info")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("version_info")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("log_query")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("VNC??")
                                .callbackData("vnc_config")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .callbackData("backup_restore")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("AI??")
                                .callbackData("ai_chat")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("????")
                                .url("https://t.me/Woci_detective")
                                .build()
                ),
                
                // ==========  ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("????")
                                .url("https://check.oci-helper.de5.net")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("?????????star??")
                                .url("https://github.com/tony-wang1990/king-detective")
                                .build()
                ),
                
                // 
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("?????")
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
                .text("? ?????")
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
                .text("???")
                .callbackData(confirmCallback)
                .build());
        row.add(InlineKeyboardButton.builder()
                .text("???")
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
                .text("? ?????")
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
                        .text("?????")
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
                        .text("? ?????")
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
                    .text("?????")
                    .callbackData(prevCallback)
                    .build());
        }
        
        row.add(InlineKeyboardButton.builder()
                .text(currentPage + "/" + totalPages)
                .callbackData("page_info")
                .build());
        
        if (currentPage < totalPages) {
            row.add(InlineKeyboardButton.builder()
                    .text("?????")
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
