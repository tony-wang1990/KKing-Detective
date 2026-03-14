"""
Fix corrupted Java string literals.

The corruption pattern: In lines like:
    .text("🚀 一键抢?)
    
The last bytes of a multi-byte UTF-8 character were corrupted into '?',
AND the closing quote '"' was also removed.

This script finds lines with an odd number of unescaped double quotes
and attempts to fix them by:
1. If the line ends with '?)' or '?,' or similar, add the closing quote before the last non-string char
2. For lines with \ufffd (replacement char) at end of what should be a string context

"""
import re
import os


def count_quotes(line):
    """Count unescaped double quotes in a line (outside comments is approximation)."""
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')


def fix_line(line):
    """
    Try to fix a line with odd number of quotes.
    
    Common patterns:
    - .text("some text?)   ->  .text("some text?")
    - .text("some text?)   ->  .text("some text?")  (same with replacement char)
    - "some text?          ->  "some text?"  (missing closing quote)
    """
    stripped = line.rstrip('\n\r')
    original = stripped
    
    # Pattern: string ends with '?' or '\ufffd' before closing paren/comma/semicolon
    # E.g.: .text("🚀 一键抢?)  ->  .text("🚀 一键抢?")
    # E.g.: "text?\n  ->  "text?"\n
    
    # Replace patterns like: ?")<eol> with ?")  (already has closing quote somehow)
    # Fix: pattern like ?) or ?. or ?,  where ? is inside unclosed string
    
    # Strategy: find last unescaped quote position and check if quote count is odd
    # If odd, we need to add a closing quote
    
    # Find position where we should insert the closing quote
    # It's usually just before the closing ) or , or ; at end of line
    
    # Pattern 1: line ends with ?) -> insert " before )
    m = re.search(r'(\?)\)(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1  # position after ?
        stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(stripped) % 2 == 0:
            return stripped + '\n'
    
    # Pattern 2: line ends with ?, -> insert " before ,
    m = re.search(r'(\?),(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1
        stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(stripped) % 2 == 0:
            return stripped + '\n'
    
    # Pattern 3: replacement char + ) -> insert " before )
    m = re.search(r'(\ufffd)\)(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1
        stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(stripped) % 2 == 0:
            return stripped + '\n'
    
    # Pattern 4: replacement char + , -> insert " before ,
    m = re.search(r'(\ufffd),(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1
        stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(stripped) % 2 == 0:
            return stripped + '\n'
    
    # Pattern 5: line ends with ? with nothing after (just whitespace)
    m = re.search(r'(\?)\s*$', stripped)
    if m:
        stripped = stripped.rstrip() + '"'
        if count_quotes(stripped) % 2 == 0:
            return stripped + '\n'
    
    # Pattern 6: replacement char at end
    m = re.search(r'(\ufffd)\s*$', stripped)
    if m:
        stripped = stripped.rstrip() + '"'
        if count_quotes(stripped) % 2 == 0:
            return stripped + '\n'
    
    # Couldn't fix automatically
    return original + '\n'


def fix_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    fixed_lines = []
    fix_count = 0
    
    for i, line in enumerate(lines, 1):
        if count_quotes(line) % 2 != 0:
            new_line = fix_line(line)
            if count_quotes(new_line) % 2 == 0:
                fixed_lines.append(new_line)
                fix_count += 1
                print(f'  Fixed L{i}: {repr(line.rstrip()[:80])} -> {repr(new_line.rstrip()[:80])}')
            else:
                fixed_lines.append(line)
                print(f'  COULD NOT FIX L{i}: {repr(line.rstrip()[:80])}')
        else:
            fixed_lines.append(line)
    
    with open(filepath, 'w', encoding='utf-8', newline='') as f:
        f.writelines(fixed_lines)
    
    return fix_count


def main():
    files = [
        'src/main/java/com/tony/kingdetective/telegram/builder/KeyboardBuilder.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/TrafficStatisticsHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigPageNavigationHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/SystemMetricsHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigListHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/CallbackHandler.java',
    ]
    
    total_fixed = 0
    for f in files:
        name = os.path.basename(f)
        print(f'\nFixing: {name}')
        count = fix_file(f)
        total_fixed += count
        print(f'  -> Fixed {count} lines')
    
    print(f'\nTotal fixes: {total_fixed}')
    
    # Final check
    print('\n--- Final validation ---')
    for f in files:
        name = os.path.basename(f)
        with open(f, 'r', encoding='utf-8') as fh:
            lines = fh.readlines()
        bad = [(i+1, l.rstrip()[:80]) for i, l in enumerate(lines) if count_quotes(l) % 2 != 0]
        if bad:
            print(f'STILL BAD {name}: {len(bad)} lines')
            for ln, content in bad[:5]:
                print(f'  L{ln}: {repr(content)}')
        else:
            print(f'OK: {name}')


if __name__ == '__main__':
    main()
