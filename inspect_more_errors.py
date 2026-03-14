import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

files = {
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/VncConfigHandler.java': [218],
    'src/main/java/com/tony/kingdetective/telegram/service/TelegramBotService.java': [75, 76, 95],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfirmTerminateHandler.java': [58, 115, 128, 165, 230, 235],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SshManagementHandler.java': [53, 77, 189, 203],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SelectConfigHandler.java': [40],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BootVolumeManagementHandler.java': [65, 122, 129, 137, 138, 143, 144, 151, 152, 292, 299, 307, 308, 313, 314, 321, 322, 452, 476, 526, 672, 709, 716, 724, 725, 730, 731, 738, 739, 766],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaManagementHandler.java': [64, 69, 86, 144, 199, 246, 247, 270, 290, 330, 413]
}

lines_to_print = {}
for filepath, lines in files.items():
    print(f'\n=== {filepath.split("/")[-1]} ===')
    with open(filepath, 'r', encoding='utf-8') as f:
        file_lines = f.readlines()
        for idx in lines:
            if idx <= len(file_lines):
                 print(f"L{idx}: {repr(file_lines[idx-1])}")
