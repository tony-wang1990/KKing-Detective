import json
files = {
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfirmTerminateHandler.java': [164, 165, 229, 230, 234, 235],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SshManagementHandler.java': [203, 204, 205, 206],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BootVolumeManagementHandler.java': [137, 138, 143, 144, 307, 308, 313, 314, 724, 725, 730, 731],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaManagementHandler.java': [199, 453],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/TaskManagementHandler.java': [52, 113, 130, 136, 154, 155, 159],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BackupRestoreHandler.java': [50, 100, 105, 144, 176, 221, 268, 319, 362],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ToggleTaskHandler.java': [83, 146, 163, 169, 187, 188]
}

output = {}
for file, nums in files.items():
    try:
        with open(file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            file_output = {}
            for num in nums:
                if 0 <= num - 1 < len(lines):
                    file_output[f"L{num}"] = lines[num-1].rstrip('\n\r')
            output[file.split('/')[-1]] = file_output
    except Exception as e:
        output[file] = str(e)

with open('inspect_rem_out.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, indent=2, ensure_ascii=False)
