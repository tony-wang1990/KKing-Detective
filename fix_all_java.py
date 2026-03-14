"""
COMPREHENSIVE FIX: Scan ALL Java files, detect and fix:
1. Invalid UTF-8 byte sequences -> re-decode with replacement
2. Unclosed string literals (odd quote count) -> insert missing closing quote
3. Illegal chars (ufffd, u2611, ufe0f etc) OUTSIDE strings -> remove/replace
"""
import os
import sys
import io
import re

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

SRC_DIR = 'src/main/java'

def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')

def is_in_string(line, pos):
    """Rough check: is character at pos inside a string literal?"""
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    in_string = False
    for i, c in enumerate(temp):
        if i == pos:
            return in_string
        if c == '"':
            in_string = not in_string
    return False

def fix_closing_quote(line):
    """Try inserting a closing quote to fix odd quote count."""
    stripped = line.rstrip('\n\r')
    # Try each ? or \ufffd position
    for ci, ch in enumerate(stripped):
        if ch in ('?', '\ufffd'):
            candidate = stripped[:ci+1] + '"' + stripped[ci+1:]
            if count_quotes(candidate) % 2 == 0:
                return candidate + '\n'
    return stripped + '\n'  # couldn't fix

def fix_illegal_chars_outside_strings(line):
    """
    Remove/replace chars like \ufffd, \u2611, \ufe0f that appear
    OUTSIDE of string literals (causes 'illegal character' error in javac).
    """
    stripped = line.rstrip('\n\r')
    # These chars are illegal in Java source code OUTSIDE strings
    ILLEGAL_OUTSIDE = ['\ufffd', '\u2611', '\ufe0f', '\u2612']
    
    result = []
    in_string = False
    i = 0
    temp = stripped.replace('\\\\', 'XX').replace('\\"', 'YY')  # for tracking
    
    chars = list(stripped)
    temp_chars = list(temp)
    
    for i, ch in enumerate(chars):
        tch = temp_chars[i] if i < len(temp_chars) else ch
        if tch == '"':
            in_string = not in_string
        if not in_string and ch in ILLEGAL_OUTSIDE:
            # Replace with nothing (it was a replacement char for a missing char)
            pass
        else:
            result.append(ch)
    
    return ''.join(result) + '\n'

# Step 1: Find all Java files
java_files = []
for root, dirs, files in os.walk(SRC_DIR):
    for f in files:
        if f.endswith('.java'):
            java_files.append(os.path.join(root, f))

print(f'Found {len(java_files)} Java files')

# Step 2: Check and fix
invalid_utf8 = []
syntax_bad = []
illegal_char_bad = []
fixed_files = []

for filepath in java_files:
    with open(filepath, 'rb') as fh:
        raw = fh.read()
    
    # Fix invalid UTF-8 first
    try:
        text = raw.decode('utf-8')
        utf8_ok = True
    except UnicodeDecodeError:
        text = raw.decode('utf-8', errors='replace')
        utf8_ok = False
        invalid_utf8.append(filepath)
    
    lines = text.split('\n')
    changed = not utf8_ok
    
    # Fix unclosed string literals
    new_lines = []
    for line in lines:
        line_with_nl = line + '\n'
        if count_quotes(line_with_nl) % 2 != 0:
            fixed = fix_closing_quote(line_with_nl)
            if count_quotes(fixed) % 2 == 0:
                new_lines.append(fixed.rstrip('\n'))
                changed = True
            else:
                new_lines.append(line)
        else:
            new_lines.append(line)
    
    # Fix illegal chars outside string literals
    new_lines2 = []
    for line in new_lines:
        line_with_nl = line + '\n'
        ILLEGAL = ['\ufffd', '\u2611', '\ufe0f', '\u2612']
        has_illegal_outside = False
        in_string = False
        temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
        temp_chars = list(temp)
        for i, ch in enumerate(line):
            tch = temp_chars[i] if i < len(temp_chars) else ch
            if tch == '"':
                in_string = not in_string
            if not in_string and ch in ILLEGAL:
                has_illegal_outside = True
                break
        
        if has_illegal_outside:
            fixed_line = fix_illegal_chars_outside_strings(line_with_nl)
            new_lines2.append(fixed_line.rstrip('\n'))
            changed = True
        else:
            new_lines2.append(line)
    
    if changed:
        new_content = '\n'.join(new_lines2)
        with open(filepath, 'w', encoding='utf-8', newline='') as fh:
            fh.write(new_content)
        fixed_files.append(os.path.basename(filepath))

print(f'\nFixed {len(fixed_files)} files:')
for f in fixed_files:
    print(f'  - {f}')

print(f'\nInvalid UTF-8 (re-decoded): {len(invalid_utf8)}')
for f in invalid_utf8:
    print(f'  {os.path.basename(f)}')

# Final validation
print('\n=== FINAL VALIDATION ===')
remaining_issues = []
for filepath in java_files:
    with open(filepath, 'rb') as fh:
        raw = fh.read()
    try:
        text = raw.decode('utf-8')
    except UnicodeDecodeError as e:
        remaining_issues.append((filepath, f'INVALID UTF-8: {e}'))
        continue
    
    # Check for illegal chars outside strings
    lines = text.split('\n')
    ILLEGAL = ['\ufffd', '\u2611', '\ufe0f', '\u2612']
    for i, line in enumerate(lines, 1):
        in_string = False
        temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
        temp_chars = list(temp)
        for j, ch in enumerate(line):
            tch = temp_chars[j] if j < len(temp_chars) else ch
            if tch == '"':
                in_string = not in_string
            if not in_string and ch in ILLEGAL:
                remaining_issues.append((filepath, f'L{i}: illegal char {repr(ch)} outside string: {repr(line[:80])}'))
                break
    
    # Check odd quote count
    bad_quotes = [(i+1, l) for i, l in enumerate(lines) if count_quotes(l+'\n') % 2 != 0]
    for ln, l in bad_quotes:
        remaining_issues.append((filepath, f'L{ln}: unclosed string: {repr(l[:80])}'))

if remaining_issues:
    print(f'REMAINING ISSUES ({len(remaining_issues)}):')
    for path, issue in remaining_issues[:30]:
        print(f'  {os.path.basename(path)}: {issue}')
else:
    print('ALL CLEAN! No remaining issues.')
