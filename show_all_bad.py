"""
Targeted fix for InstanceManagementHandler.java.
Shows ALL bad lines and their context, then applies targeted replacements.
"""

def count_quotes(line):
    temp = line.replace('\\\\', 'XX').replace('\\"', 'YY')
    return temp.count('"')


with open('src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java', 'r', encoding='utf-8') as f:
    content = f.read()
    lines = content.split('\n')

print('All lines with odd quote count:')
for i, line in enumerate(lines, 1):
    if count_quotes(line) % 2 != 0:
        print(f'L{i} (quotes={count_quotes(line)}): {repr(line[:150])}')

print('\n\nTotal bad lines:', sum(1 for l in lines if count_quotes(l) % 2 != 0))
