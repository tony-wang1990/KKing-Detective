import re

with open('src/main/java/com/tony/kingdetective/controller/OciController.java', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

for match in re.findall(r'path\s*=\s*"([^"]+)"', text):
    print(match)
