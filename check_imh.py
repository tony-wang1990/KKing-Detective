def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')

with open('src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()
for i, line in enumerate(lines, 1):
    if count_quotes(line) % 2 != 0:
        print(f'L{i}: {repr(line.rstrip()[:140])}')
