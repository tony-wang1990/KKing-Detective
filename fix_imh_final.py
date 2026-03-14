"""
Comprehensive fix for InstanceManagementHandler.java.
Writes analysis to output file to avoid console encoding issues.
Then applies smart fixes.
"""
import re
import sys
import io

# Set UTF-8 output
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')


def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')


filepath = 'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java'

with open(filepath, 'r', encoding='utf-8') as f:
    lines = f.readlines()

print(f'Total lines: {len(lines)}')
bad_indices = [i for i, l in enumerate(lines) if count_quotes(l) % 2 != 0]
print(f'Bad lines: {len(bad_indices)}')

for idx in bad_indices:
    line = lines[idx].rstrip()
    print(f'\nL{idx+1} (q={count_quotes(lines[idx])}): {repr(line[:150])}')
    # Try to fix: look for all ? and \ufffd positions, try inserting " after each
    for ci, ch in enumerate(line):
        if ch in ('?', '\ufffd'):
            candidate = line[:ci+1] + '"' + line[ci+1:]
            if count_quotes(candidate) % 2 == 0:
                print(f'  Fix: insert " after pos {ci} -> {repr(candidate[:150])}')
                lines[idx] = candidate + '\n'
                break
    else:
        print(f'  No simple fix found')

with open(filepath, 'w', encoding='utf-8', newline='') as f:
    f.writelines(lines)

# Final check
bad = [i+1 for i, l in enumerate(lines) if count_quotes(l) % 2 != 0]
print(f'\nRemaining bad lines: {bad}')
print('Done!')
