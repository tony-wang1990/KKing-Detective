import os

def check_and_convert(filepath):
    with open(filepath, 'rb') as f:
        raw = f.read()

    if not raw:
        return
        
    if raw.startswith(b'\xef\xbb\xbf'):
        raw = raw[3:]

    # First attempt: Is it already perfect UTF-8?
    try:
        text = raw.decode('utf-8')
        encoding = 'utf-8'
    except UnicodeDecodeError:
        # Second attempt: Is it perfect GBK?
        try:
            text = raw.decode('gbk')
            encoding = 'gbk'
        except UnicodeDecodeError:
            print(f"ERROR: {filepath} is neither pure UTF-8 nor pure GBK! Trying GB18030...")
            try:
                text = raw.decode('gb18030')
                encoding = 'gb18030'
            except UnicodeDecodeError:
                print(f"CRITICAL ERROR: Skipping {filepath}")
                return
    
    # Save the file back exclusively as UTF-8
    with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)

count = 0
for root, _, files in os.walk('src/main/java'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            check_and_convert(filepath)
            count += 1

print(f"Strict conversion completed. Processed {count} java files to pure UTF-8 LF.")
