import sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# OciServiceImpl.java
def fix_oci(filepath):
    with open(filepath, 'rb') as f:
        raw = f.read()
    text = raw.decode('utf-8')
    lines = text.splitlines(keepends=True)

    changed = False
    for i, line in enumerate(lines):
        orig = line
        
        # L136
        if 'String.format("?"?s?,' in line:
            line = line.replace('String.format("?"?s?,', 'String.format("%s",')
        
        # L199
        if '",?);' in line:
            line = line.replace('",?);', '");')
            
        # L450
        if '"?????"?txt?ini?");' in line or '"?????"?txt?ini?");' in line or 'txt?ini?' in line:
             line = re.sub(r'"\?\?\?\?\?"\?txt\?ini\?\?\)?', '"must be txt or ini")', line)
             line = line.replace('throw new OciException(-1, "?????"?txt?ini?);', 'throw new OciException(-1, "Must be txt or ini");')
             
        # L645
        if 'String.format("?"?s?s?,' in line:
            line = line.replace('String.format("?"?s?s?,', 'String.format("%s %s",')
            
        # L861, 874
        if 'log.warn("?"?/9 ?);' in line:
            line = line.replace('log.warn("?"?/9 ?);', 'log.warn("Step 2/9");')
        if 'log.info("?"?/9 ?);' in line:
            line = line.replace('log.info("?"?/9 ?);', 'log.info("Step 2/9");')
            
        # L999, 1058
        if 'log.error("????????"?[{}],:[{}],:[{}],?[{}] ??(API||\\uD83D\\uDC7B),?,' in line:
            line = line.replace('log.error("????????"?[{}],:[{}],:[{}],?[{}] ??(API||\\uD83D\\uDC7B),?,', 'log.error("API Error [{}] [{}] [{}] [{}]",')
        
        # L1002, 1061
        if 'sysService.sendMessage(String.format("????????"?[%s],:[%s],:[%s],?[%s] ??(API||\\uD83D\\uDC7B),?,' in line:
            line = line.replace('sysService.sendMessage(String.format("????????"?[%s],:[%s],:[%s],?[%s] ??(API||\\uD83D\\uDC7B),?,', 'sysService.sendMessage(String.format("API Error [%s] [%s] [%s] [%s]",')
            
        # L1022
        if 'sysService.sendMessage(String.format("????????"?[%s],:[%s],:[%s],?[%s]  CPU :[%s] ?,' in line:
            line = line.replace('sysService.sendMessage(String.format("????????"?[%s],:[%s],:[%s],?[%s]  CPU :[%s] ?,', 'sysService.sendMessage(String.format("Error [%s] [%s] [%s] [%s] CPU [%s]",')
            
        # L1145
        if 'log.info("????????IP???"?[{}],:[{}],:[{}],IP,IP:{} ?,' in line:
            line = line.replace('log.info("????????IP???"?[{}],:[{}],:[{}],IP,IP:{} ?,', 'log.info("IP Changed [{}] [{}] [{}] IP: {}",')

        if line != orig:
            lines[i] = line
            changed = True
            
    if changed:
        with open(filepath, 'wb') as f:
            f.write(''.join(lines).encode('utf-8'))
        print(f"Fixed {filepath}")


# VncConfigHandler.java
def fix_vnc(filepath):
    with open(filepath, 'rb') as f:
        raw = f.read()
    text = raw.decode('utf-8')
    lines = text.splitlines(keepends=True)
    
    changed = False
    for i, line in enumerate(lines):
        orig = line
        
        if '"?? ??????"?,' in line:
            line = line.replace('"?? ??????"?,', '"Please enter configuration",')
        if '"?? ??????"?;' in line:
            line = line.replace('"?? ??????"?;', '"Please enter configuration";')
        if '"??"?/cancel ?;' in line:
            line = line.replace('"??"?/cancel ?;', '"/cancel";')
            
        if line != orig:
            lines[i] = line
            changed = True
            
    if changed:
        with open(filepath, 'wb') as f:
            f.write(''.join(lines).encode('utf-8'))
        print(f"Fixed {filepath}")

fix_oci('src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java')
fix_vnc('src/main/java/com/tony/kingdetective/telegram/handler/impl/VncConfigHandler.java')
