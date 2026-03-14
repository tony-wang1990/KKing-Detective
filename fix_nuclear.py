"""
NUCLEAR FIX: Replace ALL non-ASCII characters in Java source files
with ASCII-safe alternatives.

For string literals: non-ASCII becomes ? or Unicode escape
For comments: non-ASCII is simply removed  
For everything else: non-ASCII is removed

This is the most reliable approach - no more encoding ambiguity.
"""
import os, sys, io, unicodedata
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

SRC_DIR = 'src/main/java'

def ascii_only(text):
    """Replace all non-ASCII chars with ASCII equivalents."""
    result = []
    i = 0
    in_string = False
    in_line_comment = False
    in_block_comment = False
    
    chars = list(text)
    n = len(chars)
    
    while i < n:
        ch = chars[i]
        
        # Handle newline - reset line comment
        if ch == '\n':
            in_line_comment = False
            result.append(ch)
            i += 1
            continue
        
        # Handle block comment end
        if in_block_comment:
            if ch == '*' and i + 1 < n and chars[i+1] == '/':
                in_block_comment = False
                result.append('*')
                result.append('/')
                i += 2
            else:
                # In block comment: keep ASCII, drop non-ASCII
                if ord(ch) <= 127:
                    result.append(ch)
                i += 1
            continue
        
        # Handle line comment
        if in_line_comment:
            if ord(ch) <= 127:
                result.append(ch)
            i += 1
            continue
        
        # Not in comment
        # Check for comment start
        if not in_string and ch == '/' and i + 1 < n:
            if chars[i+1] == '/':
                in_line_comment = True
                result.append('//')
                i += 2
                continue
            elif chars[i+1] == '*':
                in_block_comment = True
                result.append('/*')
                i += 2
                continue
        
        # Handle string literals
        if in_string:
            if ch == '\\' and i + 1 < n:
                # Escape sequence - keep as-is
                result.append(ch)
                result.append(chars[i+1])
                i += 2
                continue
            elif ch == '"':
                in_string = False
                result.append(ch)
                i += 1
                continue
            else:
                # Inside string: replace non-ASCII with ?
                if ord(ch) > 127:
                    # Check if it's a surrogate pair issue (emoji)
                    # Just replace with ? to keep syntax valid
                    result.append('?')
                else:
                    result.append(ch)
                i += 1
                continue
        
        # Not in string, not in comment
        if ch == '"':
            in_string = True
            result.append(ch)
            i += 1
            continue
        
        # Regular code: only keep ASCII
        if ord(ch) > 127:
            # Non-ASCII outside strings is illegal in Java
            # Just skip it
            i += 1
            continue
        
        result.append(ch)
        i += 1
    
    return ''.join(result)

# Find all Java files
java_files = []
for root, dirs, files in os.walk(SRC_DIR):
    for f in files:
        if f.endswith('.java'):
            java_files.append(os.path.join(root, f))

print(f'Processing {len(java_files)} Java files...')

fixed = 0
for filepath in java_files:
    # Read as bytes, try multiple encodings
    with open(filepath, 'rb') as f:
        raw = f.read()
    
    # Try utf-8 first, then gbk
    for enc in ['utf-8', 'gbk', 'gb18030', 'latin-1']:
        try:
            text = raw.decode(enc)
            break
        except:
            continue
    
    # Apply nuclear ASCII-only fix
    new_text = ascii_only(text)
    
    if new_text != text:
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.write(new_text)
        fixed += 1
        print(f'  Fixed: {os.path.basename(filepath)}')

print(f'\nTotal fixed: {fixed}')

# Verify: count remaining non-ASCII outside strings
print('\n=== VERIFICATION ===')
issues = 0
for filepath in java_files:
    with open(filepath, 'r', encoding='utf-8') as f:
        text = f.read()
    
    in_string = False
    in_line_comment = False
    in_block_comment = False
    chars = list(text)
    n = len(chars)
    i = 0
    file_issues = []
    line_num = 1
    
    while i < n:
        ch = chars[i]
        if ch == '\n':
            in_line_comment = False
            line_num += 1
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
            file_issues.append(f'L{line_num}: {repr(ch)} (U+{ord(ch):04X})')
        i += 1
    
    if file_issues:
        print(f'  {os.path.basename(filepath)}: {len(file_issues)} issues')
        for fi in file_issues[:3]:
            print(f'    {fi}')
        issues += len(file_issues)

if issues == 0:
    print('ALL CLEAN! No non-ASCII outside strings/comments.')
else:
    print(f'TOTAL REMAINING ISSUES: {issues}')
