import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

filepath = 'src/main/java/com/tony/kingdetective/telegram/handler/impl/BackupRestoreHandler.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix broken string
if '"?????????????""?' in content:
    content = content.replace('"? ?????"? + java.time', '"? ?????" + java.time')
    content = content.replace('"?????????????""?\n', '"?????????????"\n')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"Patched BackupRestoreHandler.java")
