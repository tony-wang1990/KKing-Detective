import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

files = {
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfirmTerminateHandler.java': [164, 165, 229, 230, 234, 235],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SshManagementHandler.java': [203, 204, 205, 206],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BootVolumeManagementHandler.java': [137, 138, 143, 144, 307, 308, 313, 314, 724, 725, 730, 731],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaManagementHandler.java': [199, 453],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/TaskManagementHandler.java': [52, 113, 130, 136, 154, 155, 159],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BackupRestoreHandler.java': [50, 100, 105, 144, 176, 221, 268, 319, 362],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ToggleTaskHandler.java': [83, 146, 163, 169, 187, 188]
}

def print_lines(filepath, line_nums):
    print(f"\n--- {filepath} ---")
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            # print unique lines with a context of +/- 1 line around the groups
            to_print = []
            for num in line_nums:
                idx = num - 1
                to_print.extend([idx-1, idx, idx+1])
            to_print = sorted(list(set(to_print)))
            for idx in to_print:
                if 0 <= idx < len(lines):
                    print(f"L{idx+1}: {repr(lines[idx])}")
    except Exception as e:
        print(e)
        
for file, nums in files.items():
    print_lines(file, nums)
