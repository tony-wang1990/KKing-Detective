import json

file_path = 'src/main/java/com/tony/kingdetective/telegram/builder/KeyboardBuilder.java'
try:
    with open(file_path, 'r', encoding='gbk') as f:
        text = f.read()
except:
    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
        text = f.read()

new_buttons = '''
                // ========== 新增Bot功能 ==========
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(" 快照管理")
                                .callbackData("snapshot_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(" SSH密钥")
                                .callbackData("ssh_keypair_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(" 全局汇总")
                                .callbackData("global_instance_summary")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(" 带宽调整")
                                .callbackData("bandwidth_adjust_select")
                                .build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(" 重建实例")
                                .callbackData("reimage_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(" 告警邮件")
                                .callbackData("alert_email_management")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(" 实例标签")
                                .callbackData("instance_tag_select")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(" 定时开关机")
                                .callbackData("scheduled_power_management")
                                .build()
                ),'''

if "// ========== 外部链接 ==========" in text:
    target = '// ========== 外部链接 =========='
    text = text.replace(target, new_buttons + '\n                ' + target)
elif "// ========== 外 ==========" in text: # handle possible garbled match
    pass # we'll find another way to insert
else:
    # insert before "关闭窗口"
    target = 'text(" 关闭窗口")'
    text = text.replace(target, new_buttons + '\n\n                        InlineKeyboardButton.builder()\n                                .text(" 关闭窗口")')

instance_manage_target = '''                        InlineKeyboardButton.builder()
                                .text(" 重启操作")
                                .callbackData("instance_reboot_select_action_v2:" + instanceId)
                                .build()
                ),'''
netboot_button = '''                        InlineKeyboardButton.builder()
                                .text(" 重启操作")
                                .callbackData("instance_reboot_select_action_v2:" + instanceId)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(" 救砖")
                                .callbackData("netboot_xyz_confirm:" + instanceId + ":" + userId)
                                .build()
                ),'''
if instance_manage_target in text:
    text = text.replace(instance_manage_target, netboot_button)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(text)
print("Finished modifying KeyboardBuilder.java")
