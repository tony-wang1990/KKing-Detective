"""
COMPREHENSIVE SURGICAL FIX: Apply specific line replacements to all remaining broken files.
Based on direct inspection of file contents.
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def get_line_ending(lines, default='\r\n'):
    """Detect line ending used in file."""
    for line in lines[:10]:
        if line.endswith('\r\n'):
            return '\r\n'
        elif line.endswith('\n'):
            return '\n'
    return default

def fix_lines_in_file(filepath, fixes):
    """
    fixes: dict of {0-indexed-line-num: new_line_content_no_newline}
    """
    with open(filepath, 'rb') as f:
        raw = f.read()
    text = raw.decode('utf-8')
    lines = text.splitlines(keepends=True)
    le = get_line_ending(lines)
    
    changed = False
    for line_idx, new_content in fixes.items():
        if line_idx < len(lines):
            old = lines[line_idx]
            # Get indent from original line
            stripped = old.lstrip()
            indent = old[:len(old)-len(stripped)]
            new_line = indent + new_content.strip() + le
            print(f'  L{line_idx+1} OLD: {repr(old[:100])}')
            print(f'  L{line_idx+1} NEW: {repr(new_line[:100])}')
            lines[line_idx] = new_line
            changed = True
    
    if changed:
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.write(''.join(lines))
        print(f'  => SAVED')


# =============================================
# MfaCodeHandler.java - L68, L98, L118
# Content from inspection:
# L68: 'xxx.text("?????\"MFA????\"" + params.getMfaCode()).build()'  <- broken  
# L98: 'xxx.text("?????\"MFA????\"" + mfaCodeId).build()'            <- broken
# L118: starts with broken str
# Need to load and check actual content first
# =============================================

print('\n=== MfaCodeHandler.java ===')
with open('src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaCodeHandler.java', 'rb') as f:
    mfa_text = f.read().decode('utf-8')
mfa_lines = mfa_text.splitlines(keepends=True)
for i in [65,66,67,68,69,70, 94,95,96,97,98,99,100, 114,115,116,117,118,119,120]:
    if i < len(mfa_lines):
        print(f'L{i+1}: {repr(mfa_lines[i][:130])}')

print('\n=== OciRegionsEnum.java ===')
with open('src/main/java/com/tony/kingdetective/enums/OciRegionsEnum.java', 'rb') as f:
    ore_text = f.read().decode('utf-8')
ore_lines = ore_text.splitlines(keepends=True)
for i in range(17, 65):
    if i < len(ore_lines):
        print(f'L{i+1}: {repr(ore_lines[i][:130])}')

print('\n=== OciServiceImpl.java (key lines) ===')
with open('src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java', 'rb') as f:
    oci_text = f.read().decode('utf-8')
oci_lines = oci_text.splitlines(keepends=True)
for i in [133,134,135,136,137, 143,144,145,146,147, 186,187,188,189,190, 196,197,198,199,200,201,
          312,313,314,315,316, 318,319,320,321,322,
          437,438,439,440, 447,448,449,450,451,
          489,490,491,492,493, 504,505,506,507,508,
          624,625,626,627,628, 642,643,644,645,646,
          858,859,860,861,862, 871,872,873,874,875,
          934,935,936,937,938,
          996,997,998,999,1000,1001,1002,1003]:
    if i < len(oci_lines):
        print(f'L{i+1}: {repr(oci_lines[i][:130])}')
