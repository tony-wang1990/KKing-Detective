package com.tony.kingdetective.telegram.builder;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Telegram Bot 键盘构建器工具类
 * 
 * @author yohann
 */
public class KeyboardBuilder {
    
    /**
     * 构建主菜单键盘
     * 
     * @return 键盘行列表
     */
    public static List<InlineKeyboardRow> buildMainMenu() {
        return Arrays.asList(
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDD11 配置列表")
                                .callbackData("config_list")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCC3 任务管理")
                                .callbackData("task_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDECE 一键测活")
                                .callbackData("check_alive")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83C\uDF10 流量统计")
                                .callbackData("traffic_statistics")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCCA 资源监控")
                                .callbackData("system_metrics")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCCB 日志查询")
                                .callbackData("log_query")
                                .build()
                ),
                                                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83E\uDD16 AI 聊天")
                                .callbackData("ai_chat")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDD0C SSH 管理")
                                .callbackData("ssh_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDEE1\uFE0F 版本信息")
                                .callbackData("version_info")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDD10 MFA 管理")
                                .callbackData("mfa_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDD27 VNC 配置")
                                .callbackData("vnc_config")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCE6 备份恢复")
                                .callbackData("backup_restore")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("📊 配额查询")
                                .callbackData("quota_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🌐 IPv6管理")
                                .callbackData("ipv6_config_select")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("💰 花费查询")
                                .callbackData("cost_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📈 流量历史")
                                .callbackData("traffic_history")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🔄 实例升降级")
                                .callbackData("shape_change_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📧 批量邮箱")
                                .callbackData("batch_email_query")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("📋 订阅信息")
                                .callbackData("subscription_info")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("💾 占用内存")
                                .callbackData("memory_occupy_select")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🔐 清除2FA")
                                .callbackData("clear_2fa_devices")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🚀 快捷开机")
                                .callbackData("quick_start")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🚫 禁用被封户")
                                .callbackData("disable_banned_accounts")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🔔 监控通知")
                                .callbackData("instance_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📅 每日报告")
                                .callbackData("daily_report")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🌐 拓展区域")
                                .callbackData("auto_region_expansion")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🔔 实例自启")
                                .callbackData("auto_restart_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🔓 开放端口")
                                .callbackData("open_all_ports_select")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("📊 资源占用")
                                .callbackData("instance_resource_usage_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🛡 安全管理")
                                .callbackData("security_management")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCE2 通知频道")
                                .url("https://t.me/Woci_detective")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDD0D 放货查询")
                                .url("https://check.oci-helper.de5.net")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCBB 开源地址（帮忙点点star⭐）")
                                .url("https://github.com/tony-wang1990/king-detective")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("❌ 关闭窗口")
                                .callbackData("cancel")
                                .build()
                )
        );
    }
    
    /**
     * 构建返回主菜单按钮行
     * 
     * @return 键盘行
     */
    public static InlineKeyboardRow buildBackToMainMenuRow() {
        return new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("◀️ 返回主菜单")
                        .callbackData("back_to_main")
                        .build()
        );
    }
    
    /**
     * 构建取消按钮行
     * 
     * @return 键盘行
     */
    public static InlineKeyboardRow buildCancelRow() {
        return new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("❌ 关闭窗口")
                        .callbackData("cancel")
                        .build()
        );
    }
    
    /**
     * 构建带有返回主菜单和取消按钮的键盘行
     * 
     * @param rows 现有的行
     * @return 带有导航按钮的行
     */
    public static List<InlineKeyboardRow> withNavigation(List<InlineKeyboardRow> rows) {
        List<InlineKeyboardRow> result = new ArrayList<>(rows);
        result.add(buildBackToMainMenuRow());
        result.add(buildCancelRow());
        return result;
    }
    
    /**
     * 构建按钮
     * 
     * @param text 按钮文本
     * @param callbackData 回调数据
     * @return 内联键盘按钮
     */
    public static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
    
    /**
     * 构建 URL 按钮
     * 
     * @param text 按钮文本
     * @param url 链接
     * @return 内联键盘按钮
     */
    public static InlineKeyboardButton urlButton(String text, String url) {
        return InlineKeyboardButton.builder()
                .text(text)
                .url(url)
                .build();
    }
    
    /**
     * 构建分页按钮行
     * 
     * @param currentPage 当前页（从0开始）
     * @param totalPages 总页数
     * @param prevCallback 上一页回调数据
     * @param nextCallback 下一页回调数据
     * @return 分页按钮行
     */
    public static InlineKeyboardRow buildPaginationRow(int currentPage, int totalPages, 
                                                       String prevCallback, String nextCallback) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        
        // 上一页按钮
        if (currentPage > 0) {
            row.add(button("⬅️ 上一页", prevCallback));
        } else {
            row.add(button("　", "noop")); // 占位按钮
        }
        
        // 页码显示
        row.add(button(String.format("%d/%d", currentPage + 1, totalPages), "noop"));
        
        // 下一页按钮
        if (currentPage < totalPages - 1) {
            row.add(button("下一页 ➡️", nextCallback));
        } else {
            row.add(button("　", "noop")); // 占位按钮
        }
        
        return row;
    }
}
