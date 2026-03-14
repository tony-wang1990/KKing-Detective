import os
import chardet

def convert_to_utf8(filepath):
    with open(filepath, 'rb') as f:
        raw = f.read()

    if not raw:
        return
        
    # Check BOM first
    if raw.startswith(b'\xff\xfe'):
        text = raw[2:].decode('utf-16-le', errors='replace')
    elif raw.startswith(b'\xfe\xff'):
        text = raw[2:].decode('utf-16-be', errors='replace')
    elif raw.startswith(b'\xef\xbb\xbf'):
        text = raw[3:].decode('utf-8', errors='replace')
    else:
        # Check if it's purely ASCII or UTF-8 first (the fastest and safest fallback)
        try:
            text = raw.decode('utf-8')
        except UnicodeDecodeError:
            # If not pure UTF-8, run chardet
            result = chardet.detect(raw)
            encoding = result['encoding']
            
            if encoding:
                try:
                    text = raw.decode(encoding, errors='replace')
                except Exception:
                    # absolute worst case: treat as GBK and mask out errors
                    text = raw.decode('gbk', errors='replace')
            else:
                text = raw.decode('gbk', errors='replace')

    # Write it back strictly as UTF-8 with LF line endings
    with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)

count = 0
for root, _, files in os.walk('src/main/java'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            convert_to_utf8(filepath)
            count += 1

print(f"Successfully transcoded {count} java files to strict UTF-8 with LF.")
