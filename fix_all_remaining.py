"""
FINAL COMPREHENSIVE SURGICAL FIX.
All patterns confirmed by direct byte inspection.

Pattern summary across all files:
- "string"? or "string"?? — broken where ? is outside string (should be removed or be part of closing)
- "?"?? — broken where string content is just ? and ?s are outside
- In ternary: isSelected ?" "" : "? — broken ternary strings
- OciRegionsEnum: "???"?, "??", "KIX"  — broken enum constructor args
- OciServiceImpl: log.error("??"?,  — broken logger call
"""
import sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def get_le(lines):
    for line in lines[:5]:
        if '\r\n' in line: return '\r\n'
    return '\n'

def fix_broken_strings_in_line(line):
    """
    Fix all known broken patterns in a single line.
    Returns (fixed_line, was_changed)
    """
    original = line
    
    # Pattern 1: "text"? -> "text" (? right after closing quote is removed)
    # e.g., 'log.error("some error"?, e);' -> 'log.error("some error", e);'
    line = re.sub(r'(")\?([,\s;)\n\r])', r'\1\2', line)
    line = re.sub(r'(")\?$', r'\1', line)  # at end of line
    
    # Pattern 2: "?"?? or "?"? — broken string with ? outside
    # e.g., '"?"??' -> '"?"' (remove trailing ?s after the closing quote)
    line = re.sub(r'(")\?+([,\s;)\n\r])', r'\1\2', line)
    
    # Pattern 3: ternary broken: isSelected ?" "" : "?  -> isSelected ? "Y" : "N"
    # This was already fixed in InstanceManagementHandler by surgical_fix2.py
    
    # Pattern 4: "text"?,  -> "text",  (? before comma)
    line = re.sub(r'(")\?,', r'\1,', line)
    
    # Pattern 5: "text"?; -> "text";  (? before semicolon)
    line = re.sub(r'(")\?;', r'\1;', line)
    
    # Pattern 6: "text"?)  -> "text")  (? before paren)  
    line = re.sub(r'(")\?\)', r'\1)', line)
    
    # Pattern 7: ?\\.n\\n? that's outside string (from InstanceCreationService was already fixed)
    
    return line, line != original


def process_file(filepath, show_changes=True):
    with open(filepath, 'rb') as f:
        raw = f.read()
    text = raw.decode('utf-8')
    lines = text.splitlines(keepends=True)
    le = get_le(lines)
    
    new_lines = []
    changed_count = 0
    
    for i, line in enumerate(lines):
        fixed, changed = fix_broken_strings_in_line(line)
        if changed:
            changed_count += 1
            if show_changes:
                print(f'  L{i+1}: {repr(line[:100])} -> {repr(fixed[:100])}')
        new_lines.append(fixed)
    
    if changed_count > 0:
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.write(''.join(new_lines))
        print(f'  => SAVED {changed_count} lines changed')
    else:
        print(f'  => No changes needed')
    
    return changed_count

# Process all problem files
problem_files = [
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaCodeHandler.java',
    'src/main/java/com/tony/kingdetective/config/ws/MetricsWebSocketHandler.java',
    'src/main/java/com/tony/kingdetective/enums/OciRegionsEnum.java',
    'src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java',
    # These were already fixed but re-run to be safe:
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ShowCreatePlansHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/service/InstanceCreationService.java',
]

for filepath in problem_files:
    fname = filepath.split('/')[-1]
    print(f'\n=== {fname} ===')
    try:
        process_file(filepath)
    except Exception as e:
        print(f'  ERROR: {e}')

# Final validation: check for remaining issues  
print('\n' + '='*60)
print('FINAL VALIDATION')
print('='*60)

import os
SRC_DIR = 'src/main/java'
java_files = []
for root, dirs, files in os.walk(SRC_DIR):
    for f in files:
        if f.endswith('.java'):
            java_files.append(os.path.join(root, f))

def has_illegal_chars_outside_strings(text):
    """Check for illegal chars outside strings/comments."""
    issues = []
    in_string = False
    in_line_comment = False  
    in_block_comment = False
    chars = list(text)
    n = len(chars)
    i = 0
    line = 1
    
    while i < n:
        ch = chars[i]
        if ch == '\n':
            in_line_comment = False
            line += 1
            i += 1
            continue
        if in_block_comment:
            if ch == '*' and i+1 < n and chars[i+1] == '/':
                in_block_comment = False
                i += 2
            else:
                i += 1
            continue
        if in_line_comment:
            i += 1
            continue
        if not in_string and ch == '/' and i+1 < n:
            if chars[i+1] == '/':
                in_line_comment = True
                i += 2
                continue
            elif chars[i+1] == '*':
                in_block_comment = True
                i += 2
                continue
        if in_string:
            if ch == '\\' and i+1 < n:
                i += 2
                continue
            elif ch == '"':
                in_string = False
            i += 1
            continue
        if ch == '"':
            in_string = True
            i += 1
            continue
        if ord(ch) > 127:
            issues.append(f'L{line}: {repr(ch)} (U+{ord(ch):04X})')
        i += 1
    return issues

all_issues = 0
for filepath in java_files:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            text = f.read()
        issues = has_illegal_chars_outside_strings(text)
        if issues:
            print(f'\n  {os.path.basename(filepath)}: {len(issues)} issues')
            for iss in issues[:5]:
                print(f'    {iss}')
            all_issues += len(issues)
    except UnicodeDecodeError as e:
        print(f'  ENCODING ERROR: {os.path.basename(filepath)}: {e}')
        all_issues += 1

if all_issues == 0:
    print('ALL CLEAN!')
else:
    print(f'\nTotal remaining issues: {all_issues}')
