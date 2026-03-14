import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

files = {
    'src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java': [
        136, 199, 450, 645, 861, 874, 999, 1000, 1001, 1002, 1003, 1004, 1022, 1023, 1024, 1058, 1059, 1060, 1061, 1062, 1063, 1145, 1146, 1147
    ],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/VncConfigHandler.java': [
        72, 101, 155
    ]
}

for filepath, lines in files.items():
    print(f'\n=== {filepath.split("/")[-1]} ===')
    with open(filepath, 'r', encoding='utf-8') as f:
        file_lines = f.readlines()
        for idx in lines:
            if idx <= len(file_lines):
                print(f"L{idx}: {repr(file_lines[idx-1])}")
