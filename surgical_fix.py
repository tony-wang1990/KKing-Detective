import os
import subprocess

files = [
    "src/main/java/com/tony/kingdetective/telegram/service/AiChatService.java",
    "src/main/java/com/tony/kingdetective/enums/OciCfgEnum.java",
    "src/main/java/com/tony/kingdetective/telegram/service/SshService.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/impl/TaskDetailsHandler.java",
    "src/main/java/com/tony/kingdetective/telegram/factory/CallbackHandlerFactory.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/impl/VersionInfoBaseHandler.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/impl/AiChatHandler.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/impl/CheckAliveHandler.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/impl/CancelHandler.java",
    "src/main/java/com/tony/kingdetective/task/OciTask.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/AbstractCallbackHandler.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/impl/LogQueryHandler.java",
    "src/main/java/com/tony/kingdetective/telegram/handler/impl/TaskPageNavigationHandler.java",
    "src/main/java/com/tony/kingdetective/service/impl/InstanceServiceImpl.java"
]

for filepath in files:
    subprocess.run(["git", "checkout", "7ed1cfd", "--", filepath], check=True)
    
    with open(filepath, 'rb') as f:
        raw = f.read()
        
    if raw.startswith(b'\xef\xbb\xbf'):
        print(f"Removed BOM from {filepath}")
        raw = raw[3:]
        
    try:
        text = raw.decode('utf-8')
    except UnicodeDecodeError:
        try:
            # GB18030 is Windows' native superset of GBK, mapping all characters safely
            text = raw.decode('gb18030')
            print(f"Converted {filepath} from GB18030 to UTF-8")
        except UnicodeDecodeError:
            # Absolute fallback to emulate IDE 'Convert' forcing
            text = raw.decode('gb18030', errors='replace')
            print(f"Forced converted {filepath} with replacement characters")
        
    with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)

print(f"Successfully surgically restored and converted {len(files)} files!")
