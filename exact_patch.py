import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

replacements = {
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfirmTerminateHandler.java': {
        'preserveBootVolume ? "?? : "': 'preserveBootVolume ? "Yes" : "No"'
    },
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SshManagementHandler.java': {
        '"?"SSH ?,\n': '"SSH: Host=%s Port=%d User=%s",\n'
    },
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BootVolumeManagementHandler.java': {
        'String.format("%s ?"d", isSelected ? "" : ", i + 1),': 'String.format("[%s] %d", isSelected ? "x" : " ", i + 1),'
    },
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaManagementHandler.java': {
        '"?" MFA...\\n\\n?,': '"Processing MFA...\\n\\n",',
        '"????????????????"MFA?;': '"Please provide MFA code:";'
    },
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/TaskManagementHandler.java': {
        '"?"??,': '"Select All",',
        'isSelected ?" "" : "?,': 'isSelected ? "[x]" : "[ ]",',
        'String.format("%s ??%d", isSelected ?" "" : "?, taskNumber),': 'String.format("[%s] Task %d", isSelected ? "x" : " ", taskNumber),',
        'KeyboardBuilder.button("?"??, "select_all_tasks"),': 'KeyboardBuilder.button("Select All", "select_all_tasks"),',
        'KeyboardBuilder.button("?"??, "deselect_all_tasks")': 'KeyboardBuilder.button("Deselect All", "deselect_all_tasks")',
        'KeyboardBuilder.button("? ??????"?, "stop_selected_tasks")': 'KeyboardBuilder.button("Stop Selected", "stop_selected_tasks")'
    },
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BackupRestoreHandler.java': {
        '"?? ??????"?;': '"Backup Execute";',
        '"?????????????????????"?;': '"Please confirm backup";',
        'KeyboardBuilder.button("? ????"?, "backup_execute_plain"),': 'KeyboardBuilder.button("Execute Backup", "backup_execute_plain"),',
        '"?"?...\\n\\n?,': '"Processing backup...\\n\\n",',
        '"??"?/cancel ?;': '"/cancel";',
        'KeyboardBuilder.button("? ????"?, "restore_start")': 'KeyboardBuilder.button("Start Restore", "restore_start")',
        '"?????????????"?;': '"Processing restore...";'
    },
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ToggleTaskHandler.java': {
        '"?"??,': '"Select All",',
        'isSelected ?" "" : "?,': 'isSelected ? "[x]" : "[ ]",',
        'String.format("%s ??%d", isSelected ?" "" : "?, taskNumber),': 'String.format("[%s] Task %d", isSelected ? "x" : " ", taskNumber),',
        'KeyboardBuilder.button("?"??, "select_all_tasks"),': 'KeyboardBuilder.button("Select All", "select_all_tasks"),',
        'KeyboardBuilder.button("?"??, "deselect_all_tasks")': 'KeyboardBuilder.button("Deselect All", "deselect_all_tasks")'
    }
}

for filepath, file_replacements in replacements.items():
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    changed = False
    for old_str, new_str in file_replacements.items():
        if old_str in content:
            content = content.replace(old_str, new_str)
            changed = True
    
    if changed:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Patched {filepath.split('/')[-1]}")
    else:
        print(f"Warning: No replacements made in {filepath.split('/')[-1]}")
