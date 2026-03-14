"""
Extended fix for Java string literal corruption patterns.
Handles the pattern: replacement_char + expression (needs closing quote before +)
"""

import re
import os


def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')


def fix_line_extended(line):
    stripped = line.rstrip('\n\r')
    
    # Pattern: replacement char (\ufffd) followed by " + " (concatenation)
    # E.g.: "获取实例列表失败\ufffd + e.getMessage()  ->  "获取实例列表失败\ufffd" + e.getMessage()
    m = re.search(r'(\ufffd)\s*\+\s*', stripped)
    if m:
        pos = m.start(1) + 1
        new_stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    # Pattern: ? followed by " + " (concatenation)
    m = re.search(r'(\?)\s*\+\s*', stripped)
    if m:
        pos = m.start(1) + 1
        new_stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    # Pattern: ends with ?) -> add quote before )
    m = re.search(r'(\?)\)(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1
        new_stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    # Pattern: replacement char ends line or followed by )
    m = re.search(r'(\ufffd)\)(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1
        new_stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    # Pattern: ends with ?, -> add quote before ,
    m = re.search(r'(\?),(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1
        new_stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    # Pattern: replacement char + , at line end
    m = re.search(r'(\ufffd),(\s*)$', stripped)
    if m:
        pos = m.start(1) + 1
        new_stripped = stripped[:pos] + '"' + stripped[pos:]
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    # Pattern: ends with ? and nothing after
    m = re.search(r'(\?)\s*$', stripped)
    if m:
        new_stripped = stripped.rstrip() + '"'
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    # Pattern: replacement char at end
    m = re.search(r'(\ufffd)\s*$', stripped)
    if m:
        new_stripped = stripped.rstrip() + '"'
        if count_quotes(new_stripped) % 2 == 0:
            return new_stripped + '\n'
    
    return stripped + '\n'


def fix_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    fixed_lines = []
    fix_count = 0
    unfixed_count = 0
    
    for i, line in enumerate(lines, 1):
        if count_quotes(line) % 2 != 0:
            new_line = fix_line_extended(line)
            if count_quotes(new_line) % 2 == 0:
                fixed_lines.append(new_line)
                fix_count += 1
                print(f'  Fixed L{i}: {repr(line.rstrip()[:80])}')
                print(f'         -> {repr(new_line.rstrip()[:80])}')
            else:
                fixed_lines.append(line)
                print(f'  CANNOT FIX L{i}: {repr(line.rstrip()[:100])}')
                unfixed_count += 1
        else:
            fixed_lines.append(line)
    
    with open(filepath, 'w', encoding='utf-8', newline='') as f:
        f.writelines(fixed_lines)
    
    return fix_count, unfixed_count


def main():
    files = [
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/builder/KeyboardBuilder.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/TrafficStatisticsHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigPageNavigationHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/SystemMetricsHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigListHandler.java',
        'src/main/java/com/tony/kingdetective/telegram/handler/CallbackHandler.java',
    ]
    
    total_fixed = 0
    total_unfixed = 0
    for f in files:
        name = os.path.basename(f)
        print(f'\n=== {name} ===')
        fixed, unfixed = fix_file(f)
        total_fixed += fixed
        total_unfixed += unfixed
        if fixed == 0 and unfixed == 0:
            print('  (no issues found)')
    
    print(f'\n=== SUMMARY ===')
    print(f'Total fixed: {total_fixed}')
    print(f'Total unfixed: {total_unfixed}')
    
    # Final validation
    print('\n=== FINAL VALIDATION ===')
    for f in files:
        name = os.path.basename(f)
        with open(f, 'r', encoding='utf-8') as fh:
            lines = fh.readlines()
        bad = [(i+1, l.rstrip()[:100]) for i, l in enumerate(lines) if count_quotes(l) % 2 != 0]
        if bad:
            print(f'BAD: {name} ({len(bad)} lines)')
            for ln, content in bad[:5]:
                print(f'  L{ln}: {repr(content)}')
        else:
            print(f'OK:  {name}')


if __name__ == '__main__':
    main()
