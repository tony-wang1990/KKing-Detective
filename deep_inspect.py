"""
Show raw bytes and content of specific problematic lines.
Focus on the files that keep failing.
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

problems = {
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java':
        [129,130,131,132,133,134,135,136,137,138,143,144,145,146,147,160,161,162,163,
         310,311,312,313,314,316,317,318,319,320,324,325,326,327,328,341,342,343,344,
         387,388,389,390,391,549,550,551,552,553,555,556,557,558,559,563,564,565,566,567,580,581,582,583],
    'src/main/java/com/tony/kingdetective/service/impl/OciServiceImpl.java':
        [134,135,136,137,145,146,147,148,187,188,189,190,197,198,199,200,201,
         313,314,315,316,319,320,321,322,437,438,439,440,448,449,450,451,
         490,491,492,493,505,506,507,508,625,626,627,628,643,644,645,646,647,
         859,860,861,862,863,872,873,874,875,876,935,936,937,938,997,998,999,1000,1001,1002,1003],
    'src/main/java/com/tony/kingdetective/enums/OciRegionsEnum.java':
        list(range(18, 70)),
    'src/main/java/com/tony/kingdetective/telegram/service/InstanceCreationService.java':
        [43,44,45,46,47,48],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/ShowCreatePlansHandler.java':
        [38,39,40,41,42,43],
    'src/main/java/com/tony/kingdetective/config/ws/MetricsWebSocketHandler.java':
        [89,90,91,92,93,104,105,106,107,108],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/MfaCodeHandler.java':
        [66,67,68,69,70,96,97,98,99,116,117,118,119,120],
    'src/main/java/com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java':
        [408,409,410,411,412],
}

for filepath, line_nums in problems.items():
    fname = filepath.split('/')[-1]
    print(f'\n{"="*60}')
    print(f'FILE: {fname}')
    print(f'{"="*60}')
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    for ln in line_nums:
        if 1 <= ln <= len(lines):
            line = lines[ln-1]
            non_ascii = [(i, hex(ord(c)), repr(c)) for i, c in enumerate(line) if ord(c) > 127]
            print(f'L{ln}: {repr(line.rstrip()[:200])}')
            if non_ascii:
                print(f'  NON-ASCII: {non_ascii[:10]}')
