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
                // 快捷功能（顶部）
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("🚀 一键抢机")
                                .callbackData("config_list")
                                .build()
                ),
                
                // 分类：实例管理
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 💼 实例管理 ━━━━━")
                                .callbackData("noop")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("快捷开机")
                                .callbackData("quick_start")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("一键测活")
                                .callbackData("check_alive")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("实例升降级")
                                .callbackData("shape_change_select")
                                .build()
                ),
                
                // 分类：网络管理
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 🌐 网络管理 ━━━━━")
                                .callbackData("noop")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("自动换IP")
                                .callbackData("auto_ip_change_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("开放端口")
                                .callbackData("open_all_ports_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("SSH管理")
                                .callbackData("ssh_management")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("IPv6管理")
                                .callbackData("ipv6_config_select")
                                .build()
                ),
                
                // 分类：资源监控
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 📊 资源监控 ━━━━━")
                                .callbackData("noop")
                                .build()
                ),
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
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("内存占用")
                                .callbackData("memory_occupy_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("流量历史")
                                .callbackData("traffic_history")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("流量统计")
                                .callbackData("traffic_statistics")
                                .build()
                ),
                
                // 分类：自动化任务
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 🤖 自动化任务 ━━━━━")
                                .callbackData("noop")
                                .build()
                ),
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
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("任务管理")
                                .callbackData("task_management")
                                .build()
                ),
                
                // 分类：安全管理
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 🔐 安全管理 ━━━━━")
                                .callbackData("noop")
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
                                .build()
                ),
                
                // 分类：账户配置
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 👤 账户配置 ━━━━━")
                                .callbackData("noop")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("账户管理")
                                .callbackData("account_management")
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
                
                // 分类：系统工具
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 🛠️ 系统工具 ━━━━━")
                                .callbackData("noop")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("禁用被封户")
                                .callbackData("disable_banned_accounts")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("批量查邮")
                                .callbackData("batch_email_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("订阅信息")
                                .callbackData("subscription_info")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("版本信息")
                                .callbackData("version_info")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("日志查询")
                                .callbackData("log_query")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("VNC配置")
                                .callbackData("vnc_config")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("备份恢复")
                                .callbackData("backup_restore")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("AI聊天")
                                .callbackData("ai_chat")
                                .build()
                ),
                
                // 分类：外部链接
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("━━━━━ 🔗 外部链接 ━━━━━")
                                .callbackData("noop")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("通知频道")
                                .url("https://t.me/Woci_detective")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("放货查询")
                                .url("https://check.oci-helper.de5.net")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("开源地址（帮忙点点star⭐）")
                                .url("https://github.com/tony-wang1990/king-detective")
                                .build()
                ),
                
                // 底部关闭按钮
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
