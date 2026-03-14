def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')

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

import os
for f in all_files:
    name = os.path.basename(f)
    with open(f, 'rb') as fh:
        raw = fh.read()
    try:
        raw.decode('utf-8')
    except Exception as e:
        print(f'UTF-8 INVALID: {name}: {e}')
        continue
    
    with open(f, 'r', encoding='utf-8') as fh:
        lines = fh.readlines()
    bad = [(i+1, l.rstrip()[:120]) for i, l in enumerate(lines) if count_quotes(l) % 2 != 0]
    
    if bad:
        print(f'SYNTAX ISSUES: {name}')
        for ln, c in bad:
            print(f'  L{ln}: {repr(c)}')
    else:
        print(f'OK: {name}')
