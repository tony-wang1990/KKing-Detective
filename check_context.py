def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')

with open('src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()
    
bad_lines = [(i+1, l) for i, l in enumerate(lines) if count_quotes(l) % 2 != 0]
print(f'Bad lines: {len(bad_lines)}')
for ln, line in bad_lines:
    # Show this line and surrounding context
    idx = ln - 1
    start = max(0, idx - 2)
    end = min(len(lines), idx + 3)
    print(f'\n--- Around L{ln} ---')
    for i in range(start, end):
        marker = '>>> ' if i == idx else '    '
        print(f'{marker}L{i+1}: {repr(lines[i].rstrip()[:150])}')
