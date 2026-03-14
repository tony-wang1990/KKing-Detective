import sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def get_le(lines):
    for line in lines[:5]:
        if '\r\n' in line: return '\r\n'
    return '\n'

def fix_line(line):
    original = line
    
    # 模式 A: "?"? + -> "Text" + 
    # e.g.: "??????"? + e.getMessage() -> "Error: " + e.getMessage()
    line = re.sub(r'("\?.*?")\?\s*\+', r'"Error: " +', line)
    
    # 模式 B: "?"?\n 或 "?"?;
    line = re.sub(r'("\?.*?")\?([;\n\r])', r'\1\2', line)
    
    # 模式 C: Ternary: isSelected ?" "" : "? -> isSelected ? "[v]" : "[ ]"
    # e.g.: String.format("%s ?"?d", isSelected ?" "" : "?, i + 1)
    if 'isSelected ?" "" : "?' in line:
         line = line.replace('isSelected ?" "" : "?', 'isSelected ? "[v]" : "[ ]"')
         line = line.replace('%s ?"?', '%s')
         line = line.replace('%s ??', '%s ')
    
    # 模式 D: volume.getAttached() ? "?? : "?, -> volume.getAttached() ? "Yes" : "No",
    if 'volume.getAttached() ?"?' in line or 'volume.getAttached() ? "?? : "?' in line:
        line = re.sub(r'volume\.getAttached\(\)\s*\?.+?:.*?\?,', r'volume.getAttached() ? "Yes" : "No",', line)
        
    # 模式 E: "text"? -> "text"
    line = re.sub(r'(".*?)\"\?', r'\1"', line)
    
    # 模式 F: "?"?? -> "text"  (如按钮名)
    if '"?"??' in line:
        line = line.replace('"?"??', '"Button"')
    
    # 模式 G: 字符串以 ? 结尾并在引号外，如 "??"?/cancel ?; -> "/cancel";
    if '"??"?/cancel ?;' in line:
        line = line.replace('"??"?/cancel ?;', '"/cancel";')
        
    # TelegramBotService: CollectionUtil.isEmpty(failNames) ?" "? : String.join(...)
    if 'CollectionUtil.isEmpty(failNames) ?" "? :' in line:
         line = line.replace('CollectionUtil.isEmpty(failNames) ?" "? :', 'CollectionUtil.isEmpty(failNames) ? " " :')
    
    # 各种奇怪的问号断崖，如 "?"? MFA...\n\n?, -> "Processing MFA...\n\n",
    if 'MFA...' in line and '?' in line:
         line = re.sub(r'".*?MFA.*?".*?,', r'"Processing MFA...",', line)
         
    # 修复孤星问号
    line = line.replace('"?"?', '"?"')
    line = line.replace('"? ', '" ')
         
    return line

files = [
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/VncConfigHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/service/TelegramBotService.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfirmTerminateHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SshManagementHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SelectConfigHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/BootVolumeManagementHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaManagementHandler.java'
]

for filepath in files:
    with open(filepath, 'rb') as f:
        raw = f.read()
    text = raw.decode('utf-8')
    lines = text.splitlines(keepends=True)
    
    changed_count = 0
    for i, line in enumerate(lines):
        fixed = fix_line(line)
        if fixed != line:
            # 二次过滤：如果还有孤儿引号或者外部问号，强行将其格式化为一个空字符串或者普通的标识符
            if fixed.count('"') % 2 != 0:
                 # 不平衡的引号，再强行补齐或删减（极少情况）
                 fixed = fixed.replace('?"\n', '?"\n') # just a trick
                 fixed = re.sub(r'\?("\s*)$', r'\1', fixed) 
                 fixed = re.sub(r'("\s*)\?$', r'\1', fixed)
            lines[i] = fixed
            changed_count += 1
            print(f"{filepath} L{i+1}:\n- {repr(line)}\n+ {repr(fixed)}")
            
    if changed_count > 0:
        with open(filepath, 'wb') as f:
            f.write(''.join(lines).encode('utf-8'))
        print(f"=> Saved {changed_count} lines in {filepath.split('/')[-1]}")
