"""
Final surgical fix based on direct file inspection.
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Read InstanceCreationService.java line 45 raw bytes
with open('src/main/java/com/tony/kingdetective/telegram/service/InstanceCreationService.java', 'rb') as f:
    raw = f.read()

# Show bytes around line 45
text = raw.decode('utf-8')
lines = text.splitlines(keepends=True)
print(f'Total lines: {len(lines)}')
print(f'L44: {repr(lines[43])}')
print(f'L45: {repr(lines[44])}')
print(f'L46: {repr(lines[45])}')
print()
print(f'L45 bytes: {list(hex(b) for b in lines[44].encode("utf-8")[:50])}')
