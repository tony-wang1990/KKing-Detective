#!/usr/bin/env python3
"""
Fix Java encoding issues:
1. Restore 14 affected files from git commit b476b65 (the BOM-removal commit)
2. Remove any remaining UTF-8 BOM
3. Convert any GBK/GB18030 files to UTF-8
4. Normalize line endings to LF
"""
import os
import subprocess
import sys

REPO_DIR = os.path.dirname(os.path.abspath(__file__))
CLEAN_COMMIT = "b476b65"

AFFECTED_FILES = [
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
    "src/main/java/com/tony/kingdetective/service/impl/InstanceServiceImpl.java",
]

def restore_from_git(filepath, commit):
    """Restore a file from a specific git commit."""
    result = subprocess.run(
        ["git", "checkout", commit, "--", filepath],
        cwd=REPO_DIR,
        capture_output=True,
        text=True
    )
    if result.returncode != 0:
        print(f"  [WARN] git checkout failed for {filepath}: {result.stderr.strip()}")
        return False
    return True

def fix_encoding(filepath):
    """Remove BOM, convert GBK->UTF-8, normalize line endings."""
    abs_path = os.path.join(REPO_DIR, filepath.replace("/", os.sep))
    
    if not os.path.exists(abs_path):
        print(f"  [SKIP] File not found: {abs_path}")
        return
    
    with open(abs_path, 'rb') as f:
        raw = f.read()
    
    # Count original size
    orig_size = len(raw)
    
    # Remove UTF-8 BOM
    if raw.startswith(b'\xef\xbb\xbf'):
        raw = raw[3:]
        print(f"  [BOM] Removed UTF-8 BOM from {os.path.basename(filepath)}")
    
    # Detect and convert encoding
    try:
        text = raw.decode('utf-8')
        encoding_used = 'utf-8'
    except UnicodeDecodeError:
        try:
            text = raw.decode('gb18030')
            encoding_used = 'gb18030'
            print(f"  [ENC] Converted {os.path.basename(filepath)} from GB18030 to UTF-8")
        except UnicodeDecodeError:
            text = raw.decode('latin-1')
            encoding_used = 'latin-1 (fallback)'
            print(f"  [WARN] Used latin-1 fallback for {os.path.basename(filepath)}")
    
    # Normalize line endings to LF (Unix)
    text = text.replace('\r\n', '\n').replace('\r', '\n')
    
    # Write back as clean UTF-8 with LF line endings
    with open(abs_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    
    new_size = os.path.getsize(abs_path)
    print(f"  [OK]  {os.path.basename(filepath)} ({encoding_used}, {orig_size} -> {new_size} bytes)")

print("=" * 60)
print("Step 1: Restoring files from git commit", CLEAN_COMMIT)
print("=" * 60)

restored = []
failed = []
for filepath in AFFECTED_FILES:
    print(f"\nProcessing: {os.path.basename(filepath)}")
    if restore_from_git(filepath, CLEAN_COMMIT):
        restored.append(filepath)
        print(f"  [GIT] Restored from {CLEAN_COMMIT}")
    else:
        print(f"  [SKIP] Will use current version and just fix encoding")
        failed.append(filepath)

print("\n" + "=" * 60)
print("Step 2: Fixing encoding (BOM removal + UTF-8 normalization)")
print("=" * 60)

for filepath in AFFECTED_FILES:
    fix_encoding(filepath)

print("\n" + "=" * 60)
print(f"DONE! Restored: {len(restored)}, Encoding-fixed only: {len(failed)}")
print("=" * 60)

# Check InstanceServiceImpl.java line count to verify it's not corrupted
impl_path = os.path.join(REPO_DIR, "src", "main", "java", "com", "tony", 
                          "kingdetective", "service", "impl", "InstanceServiceImpl.java")
if os.path.exists(impl_path):
    with open(impl_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    print(f"\nInstanceServiceImpl.java line count: {len(lines)}")
    if len(lines) < 500:
        print("✓ File looks healthy (reasonable line count)")
    elif len(lines) < 1000:
        print("⚠ File might have some extra content, please verify")
    else:
        print("✗ WARNING: File still has too many lines, may still be corrupted!")
        print(f"  First 5 lines: {lines[:5]}")
