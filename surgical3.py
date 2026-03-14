"""
DIRECT LINE REPLACEMENT: Fix specific broken lines by line number.
Based on actual file inspection, replace the broken lines with correct content.
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def fix_line_in_file(filepath, line_num_0indexed, new_content_with_newline):
    """Replace a specific line (0-indexed) in a file."""
    with open(filepath, 'rb') as f:
        raw = f.read()
    
    text = raw.decode('utf-8')
    lines = text.splitlines(keepends=True)
    
    print(f'  Line {line_num_0indexed+1} OLD: {repr(lines[line_num_0indexed][:100])}')
    lines[line_num_0indexed] = new_content_with_newline
    print(f'  Line {line_num_0indexed+1} NEW: {repr(lines[line_num_0indexed][:100])}')
    
    new_text = ''.join(lines)
    with open(filepath, 'w', encoding='utf-8', newline='') as f:
        f.write(new_text)


# --- InstanceCreationService.java ---
print('\n=== InstanceCreationService.java ===')
fix_line_in_file(
    'src/main/java/com/tony/kingdetective/telegram/service/InstanceCreationService.java',
    44,  # L45 (0-indexed = 44)
    '                        .text("Instance creation task submitted!")\r\n'
)

# --- MfaCodeHandler.java ---
print('\n=== MfaCodeHandler.java ===')
# Need to check lines 68, 98, 118 first - load file
with open('src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaCodeHandler.java', 'rb') as f:
    mfa_text = f.read().decode('utf-8')
mfa_lines = mfa_text.splitlines(keepends=True)
print(f'L68: {repr(mfa_lines[67][:120])}')
print(f'L98: {repr(mfa_lines[97][:120])}')
print(f'L118: {repr(mfa_lines[117][:120])}')

# --- MetricsWebSocketHandler.java ---
print('\n=== MetricsWebSocketHandler.java ===')
with open('src/main/java/com/tony/kingdetective/config/ws/MetricsWebSocketHandler.java', 'rb') as f:
    mwsh_text = f.read().decode('utf-8')
mwsh_lines = mwsh_text.splitlines(keepends=True)
print(f'L91: {repr(mwsh_lines[90][:120])}')
print(f'L106: {repr(mwsh_lines[105][:120])}')

# --- OciRegionsEnum.java ---
print('\n=== OciRegionsEnum.java ---')
with open('src/main/java/com/tony/kingdetective/enums/OciRegionsEnum.java', 'rb') as f:
    ore_text = f.read().decode('utf-8')
ore_lines = ore_text.splitlines(keepends=True)
for i in [19, 20, 25, 28, 30, 31, 32, 33, 36, 37, 44, 46, 48, 49, 50, 52, 55, 57, 58, 59, 60]:
    if i < len(ore_lines):
        print(f'L{i+1}: {repr(ore_lines[i][:130])}')

# --- OciServiceImpl.java ---
print('\n=== OciServiceImpl.java (key lines) ===')
with open('src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java', 'rb') as f:
    oci_text = f.read().decode('utf-8')
oci_lines = oci_text.splitlines(keepends=True)
for i in [135, 146, 188, 198, 199, 314, 320, 438, 449, 491, 506, 626, 644, 860, 873, 936, 998, 999, 1000, 1001]:
    if i < len(oci_lines):
        print(f'L{i+1}: {repr(oci_lines[i][:130])}')
