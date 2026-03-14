def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')

files = [
    ('SystemMetricsHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/SystemMetricsHandler.java'),
    ('CreateInstanceHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java'),
]

for name, path in files:
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    print(f'\n{name}:')
    for i, line in enumerate(lines, 1):
        if count_quotes(line) % 2 != 0:
            print(f'  L{i}: {repr(line.rstrip()[:140])}')
