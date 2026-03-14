"""
SURGICAL FIX: Fix the specific line patterns that are broken.

The problem is ternary expressions like:
  isSelected ? "checked" : "unchecked"
Got corrupted to:
  isSelected ?" "" : "?    (orphan quotes, broken strings)

Fix patterns:
1. ?\" \"\" : \"? -> ? "Y" : "N"   (ternary with emoji checkboxes)
2. \"?\"?? -> "?" xx  (broken keyboard button labels)
3. joinChannelBroadcast ? \"?????? : \"?? -> proper ternary
"""
import sys, io, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

files_to_fix = {
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java': {
        # All lines with: isSelected ?" "" : "?  -> isSelected ? "[Y]" : "[N]"
        # Pattern: String.format("%s ??%d", isSelected ?" "" : "?, i + 1)
        131: ('String.format("%s ??%d", isSelected ?" "" : "?, i + 1),',
              'String.format("%s %d", isSelected ? "[v]" : "[ ]", i + 1),'),
        137: ('String.format("%s ??%d", isSelected ?" "" : "?, i + 1),',
              'String.format("%s %d", isSelected ? "[v]" : "[ ]", i + 1),'),
        145: ('KeyboardBuilder.button("?"??, "select_all_instances"),',
              'KeyboardBuilder.button("[v] Select All", "select_all_instances"),'),
        146: ('KeyboardBuilder.button("?"??, "deselect_all_instances")',
              'KeyboardBuilder.button("[ ] Deselect All", "deselect_all_instances")'),
        162: ('KeyboardBuilder.button("? ???????\"?, "confirm_terminate_instances")',
              'KeyboardBuilder.button("[!] Terminate Selected", "confirm_terminate_instances")'),
        312: ('String.format("%s ??%d", isSelected ?" "" : "?, i + 1),',
              'String.format("%s %d", isSelected ? "[v]" : "[ ]", i + 1),'),
        318: ('String.format("%s ??%d", isSelected ?" "" : "?, i + 1),',
              'String.format("%s %d", isSelected ? "[v]" : "[ ]", i + 1),'),
        326: ('KeyboardBuilder.button("?"??, "select_all_instances"),',
              'KeyboardBuilder.button("[v] Select All", "select_all_instances"),'),
        327: ('KeyboardBuilder.button("?"??, "deselect_all_instances")',
              'KeyboardBuilder.button("[ ] Deselect All", "deselect_all_instances")'),
        343: ('KeyboardBuilder.button("? ???????\"?, "confirm_terminate_instances")',
              'KeyboardBuilder.button("[!] Terminate Selected", "confirm_terminate_instances")'),
        389: ('.text(String.format("???\"?%d ?, instances.size()))',
              '.text(String.format("Selected %d instances", instances.size()))'),
        551: ('String.format("%s ??%d", isSelected ?" "" : "?, i + 1),',
              'String.format("%s %d", isSelected ? "[v]" : "[ ]", i + 1),'),
        557: ('String.format("%s ??%d", isSelected ?" "" : "?, i + 1),',
              'String.format("%s %d", isSelected ? "[v]" : "[ ]", i + 1),'),
        565: ('KeyboardBuilder.button("?"??, "select_all_instances"),',
              'KeyboardBuilder.button("[v] Select All", "select_all_instances"),'),
        566: ('KeyboardBuilder.button("?"??, "deselect_all_instances")',
              'KeyboardBuilder.button("[ ] Deselect All", "deselect_all_instances")'),
        582: ('KeyboardBuilder.button("? ???????\"?, "confirm_terminate_instances")',
              'KeyboardBuilder.button("[!] Terminate Selected", "confirm_terminate_instances")'),
    },
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java': {
        410: ('String channelStatus = joinChannelBroadcast ? "?????? : "??;',
              'String channelStatus = joinChannelBroadcast ? "Channel: ON" : "Channel: OFF";'),
    },
}

for filepath, line_fixes in files_to_fix.items():
    print(f'\n=== {filepath.split("/")[-1]} ===')
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    changed = False
    for line_num, (old_pattern, new_content) in line_fixes.items():
        idx = line_num - 1
        if idx < len(lines):
            actual = lines[idx].rstrip()
            # Get the indentation
            indent = len(actual) - len(actual.lstrip())
            indent_str = actual[:indent]
            print(f'  L{line_num} current: {repr(actual)}')
            lines[idx] = indent_str + new_content.strip() + '\n'
            print(f'  L{line_num} fixed:   {repr(lines[idx].rstrip())}')
            changed = True
    
    if changed:
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.writelines(lines)
        print(f'  SAVED.')

print('\nDone with InstanceManagementHandler and CreateInstanceHandler.')
print('\nNow need to check other files...')

# Check what other files still have issues
# Show a few key lines from other problem files
other_files = [
    ('src/main/java/com/tony/kingdetective/telegram/service/InstanceCreationService.java', [43,44,45,46,47,48]),
    ('src/main/java/com/tony/kingdetective/telegram/handler/impl/ShowCreatePlansHandler.java', [38,39,40,41,42,43,44]),
    ('src/main/java/com/tony/kingdetective/config/ws/MetricsWebSocketHandler.java', [89,90,91,92,104,105,106,107,108]),
    ('src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaCodeHandler.java', [66,67,68,69,96,97,98,99,116,117,118,119]),
    ('src/main/java/com/tony/kingdetective/enums/OciRegionsEnum.java', list(range(18,30))),
    ('src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java', [134,135,136,137,145,146,147,197,198,199,200,313,314,315,316,319,320,321,437,438,439,448,449,450,490,491,492,505,506,507,625,626,627,643,644,645,859,860,861,862,872,873,874,935,936,937,997,998,999,1000,1001,1002]),
]

for filepath, lnums in other_files:
    print(f'\n--- {filepath.split("/")[-1]} ---')
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    for ln in lnums:
        if 1 <= ln <= len(lines):
            print(f'L{ln}: {repr(lines[ln-1].rstrip()[:120])}')
