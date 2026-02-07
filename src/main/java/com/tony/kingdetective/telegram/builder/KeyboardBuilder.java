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
                // Row 1: 账户与核心查询
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("👥 账户管理")
                                .callbackData("account_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📊 配额查询")
                                .callbackData("quota_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("💰 消费查询")
                                .callbackData("cost_query")
                                .build()
                ),
                // Row 2: 实例与配置
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🖥️ 客户端管理") // 对应实例管理/配置列表
                                .callbackData("config_list")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📝 Profile管理")
                                .callbackData("profile_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🌐 IPv6管理")
                                .callbackData("ipv6_config_select")
                                .build()
                ),
                // Row 3: 快捷操作与状态
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🚀 快捷开机")
                                .callbackData("quick_start")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🔍 一键测活")
                                .callbackData("check_alive")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🔄 实例升降级")
                                .callbackData("shape_change_select")
                                .build()
                ),
                // Row 4: 网络操作
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🌍 自动换IP")
                                .callbackData("auto_ip_change_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🔓 开放端口")
                                .callbackData("open_all_ports_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🔌 SSH 管理")
                                .callbackData("ssh_management")
                                .build()
                ),
                // Row 5: 资源与流量
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("📊 资源占用")
                                .callbackData("instance_resource_usage_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("💾 内存占用")
                                .callbackData("memory_occupy_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📈 流量历史")
                                .callbackData("traffic_history")
                                .build()
                ),
                // Row 6: 监控与自动化
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🔔 监控通知")
                                .callbackData("instance_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🤖 监控自启")
                                .callbackData("auto_restart_monitoring")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📅 每日日报")
                                .callbackData("daily_report")
                                .build()
                ),
                // Row 7: 安全与MFA
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🛡 安全管理")
                                .callbackData("security_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🔐 MFA 管理")
                                .callbackData("mfa_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🧹 清除2FA")
                                .callbackData("clear_2fa_devices")
                                .build()
                ),
                // Row 8: 系统维护
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🚫 禁用被封户")
                                .callbackData("disable_banned_accounts")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📧 批量查邮")
                                .callbackData("batch_email_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("ℹ️ 版本信息") // 含升级功能
                                .callbackData("version_info")
                                .build()
                ),
                // Row 9: 辅助功能
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("📋 订阅信息")
                                .callbackData("subscription_info")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("🌐 区域拓展")
                                .callbackData("auto_region_expansion")
                                .build(),
                         InlineKeyboardButton.builder()
                                .text("🤖 AI 聊天")
                                .callbackData("ai_chat")
                                .build()
                ),
                // Row 10: 日志与统计 (Optional extra row if needed, merging into others for compactness)
                 new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83C\uDF10 流量统计")
                                .callbackData("traffic_statistics")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCCB 日志查询")
                                .callbackData("log_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCC3 任务管理")
                                .callbackData("task_management")
                                .build()
                 ),
                 // Row 11: 更多工具
                 new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDD27 VNC 配置")
                                .callbackData("vnc_config")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDCE6 备份恢复")
                                .callbackData("backup_restore")
                                .build()
                 ),
                // Row 12: 外部链接
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
