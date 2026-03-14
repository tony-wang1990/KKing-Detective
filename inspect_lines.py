"""
Inspect specific problematic lines in the files that still have errors.
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

files_lines = {
    'src/main/java/com/tony/kingdetective/telegram/service/InstanceCreationService.java': [43,44,45,46,47],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java': [129,130,131,132,133,136,137,138,143,144,145,146,160,161,162,163],
    'src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java': [134,135,136,137,145,146,147,148,859,860,861,862,863,872,873,874,875,997,998,999,1000,643,644,645,646,647],
    'src/main/java/com/tony/kingdetective/enums/OciRegionsEnum.java': [18,19,20,21,24,25,26,27],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ShowCreatePlansHandler.java': [38,39,40,41,42],
    'src/main/java/com/tony/kingdetective/config/ws/MetricsWebSocketHandler.java': [89,90,91,92,104,105,106,107],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaCodeHandler.java': [66,67,68,69,96,97,98,116,117,118,119],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java': [408,409,410,411],
}

for filepath, line_nums in files_lines.items():
    print(f'\n{"="*60}')
    print(f'FILE: {filepath.split("/")[-1]}')
    print(f'{"="*60}')
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        for ln in line_nums:
            if 1 <= ln <= len(lines):
                print(f'L{ln}: {repr(lines[ln-1].rstrip()[:150])}')
    except Exception as e:
        print(f'ERROR: {e}')
