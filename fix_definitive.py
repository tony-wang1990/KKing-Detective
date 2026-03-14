"""
DEFINITIVE FIX: 
The root cause is files with GBK-encoded content. When decoded with utf-8 errors='replace',
multi-byte GBK sequences become \ufffd chars. Our previous quote-insertion logic was broken.

NEW STRATEGY:
1. Re-read files with GBK encoding (for those that fail utf-8)
2. Write back as clean utf-8
3. For files that ARE valid utf-8 but have chars outside strings:
   - Remove ALL non-ASCII, non-printable chars that appear OUTSIDE string literals
   - These are replacement chars (\ufffd), emoji left outside strings by bad quote-insertion
"""
import os, sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

SRC_DIR = 'src/main/java'

# Characters that should NEVER appear outside string literals in Java code
# (i.e., chars > 127 that are not valid Java identifier chars)
def is_illegal_outside_string(ch):
    """Return True if this char would cause 'illegal character' error in javac."""
    if ord(ch) <= 127:
        return False
    # Valid Unicode identifier chars (letters/digits) can appear in identifiers/strings
    # But most CJK chars and emojis/fullwidth chars outside strings = illegal
    # Conservative: flag all non-ASCII outside strings
    return True

def fix_line_illegal_chars(line):
    """Remove non-ASCII chars that are OUTSIDE string literals."""
    result = []
    in_string = False
    in_char = False
    i = 0
    chars = list(line)
    
    while i < len(chars):
        ch = chars[i]
        
        # Handle escape sequences inside strings
        if in_string and ch == '\\' and i + 1 < len(chars):
            result.append(ch)
            result.append(chars[i+1])
            i += 2
            continue
        
        # Toggle string context
        if ch == '"' and not in_char:
            in_string = not in_string
            result.append(ch)
            i += 1
            continue
        
        if ch == "'" and not in_string:
            in_char = not in_char
            result.append(ch)
            i += 1
            continue
        
        # Check if char is illegal outside string
        if not in_string and not in_char and is_illegal_outside_string(ch):
            # Skip it (remove)
            i += 1
            continue
        
        result.append(ch)
        i += 1
    
    return ''.join(result)

def try_decode_file(filepath):
    """Try multiple encodings to decode a file."""
    with open(filepath, 'rb') as f:
        raw = f.read()
    
    # Try UTF-8 first
    try:
        return raw.decode('utf-8'), 'utf-8'
    except UnicodeDecodeError:
        pass
    
    # Try GBK (Windows Chinese)
    try:
        return raw.decode('gbk'), 'gbk'
    except UnicodeDecodeError:
        pass
    
    # Try GB18030
    try:
        return raw.decode('gb18030'), 'gb18030'
    except UnicodeDecodeError:
        pass
    
    # Last resort: utf-8 with replacement
    return raw.decode('utf-8', errors='replace'), 'utf-8-replace'

# Find all Java files
java_files = []
for root, dirs, files in os.walk(SRC_DIR):
    for f in files:
        if f.endswith('.java'):
            java_files.append(os.path.join(root, f))

print(f'Scanning {len(java_files)} Java files...')

fixed = 0
encoding_fixed = 0

for filepath in java_files:
    text, enc = try_decode_file(filepath)
    changed = enc != 'utf-8'
    
    if changed:
        encoding_fixed += 1
    
    # Apply line-by-line illegal char removal outside strings
    lines = text.splitlines(keepends=True)
    new_lines = []
    line_changed = False
    for line in lines:
        new_line = fix_line_illegal_chars(line)
        if new_line != line:
            line_changed = True
        new_lines.append(new_line)
    
    if changed or line_changed:
        new_text = ''.join(new_lines)
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.write(new_text)
        fixed += 1
        print(f'  Fixed ({enc}): {os.path.basename(filepath)}')

print(f'\nTotal fixed: {fixed} (encoding fixed: {encoding_fixed})')

# FINAL VALIDATION: check for any remaining issues
print('\n=== FINAL VALIDATION ===')
issues = []
for filepath in java_files:
    with open(filepath, 'rb') as f:
        raw = f.read()
    
    # 1. Must be valid UTF-8
    try:
        text = raw.decode('utf-8')
    except UnicodeDecodeError as e:
        issues.append(f'INVALID UTF-8: {os.path.basename(filepath)}: {e}')
        continue
    
    # 2. No illegal chars outside strings
    lines = text.splitlines()
    for i, line in enumerate(lines, 1):
        in_string = False
        j = 0
        chars = list(line)
        while j < len(chars):
            ch = chars[j]
            if in_string and ch == '\\' and j + 1 < len(chars):
                j += 2
                continue
            if ch == '"':
                in_string = not in_string
            elif not in_string and is_illegal_outside_string(ch):
                issues.append(f'{os.path.basename(filepath)}:L{i}: illegal char {repr(ch)} outside string: {repr(line[:80])}')
                break
            j += 1

if issues:
    print(f'REMAINING ISSUES ({len(issues)}):')
    for issue in issues[:20]:
        print(f'  {issue}')
else:
    print('ALL CLEAN! Ready to build.')
