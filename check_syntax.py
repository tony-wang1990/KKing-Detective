import re
import os

files = [
    ('KeyboardBuilder.java', 'src/main/java/com/tony/kingdetective/telegram/builder/KeyboardBuilder.java'),
    ('TrafficStatisticsHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/TrafficStatisticsHandler.java'),
    ('ConfigPageNavigationHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigPageNavigationHandler.java'),
    ('SystemMetricsHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/SystemMetricsHandler.java'),
    ('CreateInstanceHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java'),
    ('ConfigListHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/ConfigListHandler.java'),
    ('InstanceManagementHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java'),
    ('CallbackHandler.java', 'src/main/java/com/tony/kingdetective/telegram/handler/CallbackHandler.java'),
]

for name, path in files:
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    issues = []
    for i, line in enumerate(lines, 1):
        stripped = line.rstrip()
        # Remove escaped sequences before counting quotes
        temp = stripped.replace('\\\\', '')
        temp = temp.replace('\\"', '')
        # Count remaining double quotes
        count = temp.count('"')
        if count % 2 != 0:
            issues.append((i, stripped[:120]))
    if issues:
        print(f'\n{name}: {len(issues)} suspicious lines')
        for ln, content in issues[:15]:
            print(f'  L{ln}: {repr(content)}')
    else:
        print(f'{name}: syntactically OK')
