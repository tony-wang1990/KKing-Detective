"""
Final targeted fix for remaining unclosed string literals.
Handles patterns like "text?); and "1text?, where the string needs closing before ) or ,
"""
import re
import os


def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')


def fix_line_final(line):
    stripped = line.rstrip('\n\r')
    
    # Try all possible positions of ? or \ufffd and see which one fixes the quote count
    # by inserting " after it
    
    candidates = []
    for i, ch in enumerate(stripped):
        if ch in ('?', '\ufffd'):
            # Try inserting " after this position
            candidate = stripped[:i+1] + '"' + stripped[i+1:]
            if count_quotes(candidate) % 2 == 0:
                candidates.append((i, candidate))
    
    if candidates:
        # Pick the LAST occurrence (most likely the end of the string)
        best = candidates[-1][1]
        return best + '\n'
    
    return stripped + '\n'


def fix_file(filepath, verbose=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    fixed = []
    fix_count = 0
    unfixed = 0
    
    for i, line in enumerate(lines, 1):
        if count_quotes(line) % 2 != 0:
            new_line = fix_line_final(line)
            if count_quotes(new_line) % 2 == 0:
                fixed.append(new_line)
                fix_count += 1
                if verbose:
                    print(f'  FIXED L{i}: {repr(line.rstrip()[:80])}')
                    print(f'          -> {repr(new_line.rstrip()[:80])}')
            else:
                fixed.append(line)
                unfixed += 1
                if verbose:
                    print(f'  FAILED L{i}: {repr(line.rstrip()[:80])}')
        else:
            fixed.append(line)
    
    with open(filepath, 'w', encoding='utf-8', newline='') as f:
        f.writelines(fixed)
    
    return fix_count, unfixed


files = [
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SystemMetricsHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java',
]

for f in files:
    name = os.path.basename(f)
    print(f'\n=== {name} ===')
    fixed, unfixed = fix_file(f)
    print(f'Fixed: {fixed}, Failed: {unfixed}')

# Final validation on ALL files
print('\n=== FINAL VALIDATION (ALL FILES) ===')
all_files = [
    'src/main/java/com/tony/kingdetective/telegram/builder/KeyboardBuilder.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/TrafficStatisticsHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigPageNavigationHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/SystemMetricsHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigListHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java',
    'src/main/java/com/tony/kingdetective/telegram/handler/CallbackHandler.java',
]

all_ok = True
for f in all_files:
    name = os.path.basename(f)
    try:
        with open(f, 'rb') as fh:
            raw = fh.read()
        raw.decode('utf-8')  # check valid UTF-8
        
        with open(f, 'r', encoding='utf-8') as fh:
            lines = fh.readlines()
        bad = [(i+1, l.rstrip()[:80]) for i, l in enumerate(lines) if count_quotes(l) % 2 != 0]
        
        if bad:
            print(f'SYNTAX BAD: {name} ({len(bad)} lines)')
            for ln, c in bad[:3]:
                print(f'  L{ln}: {repr(c)}')
            all_ok = False
        else:
            print(f'OK: {name}')
    except Exception as e:
        print(f'ERROR: {name}: {e}')
        all_ok = False

print('\nResult:', 'ALL CLEAN!' if all_ok else 'SOME FILES STILL HAVE ISSUES')
