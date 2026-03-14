#!/usr/bin/env python3
"""
Comprehensive fix: scan ALL Java files.
Handle BOM, GB18030, latin-1 fallback, and mixed encodings.
"""
import os

REPO_DIR = r"C:\Users\yatao\.gemini\antigravity\scratch\king-detective"
SRC_DIR = os.path.join(REPO_DIR, "src")

fixed = []
already_ok = []

def process(path):
    with open(path, 'rb') as f:
        raw = f.read()

    changed = False

    # Remove UTF-8 BOM
    if raw.startswith(b'\xef\xbb\xbf'):
        raw = raw[3:]
        changed = True

    # Try encodings in order of preference
    text = None
    for enc in ('utf-8', 'gb18030', 'gbk', 'big5', 'latin-1'):
        try:
            text = raw.decode(enc)
            if enc != 'utf-8':
                changed = True
            break
        except (UnicodeDecodeError, LookupError):
            continue

    if text is None:
        # Absolute last resort: utf-8 with replacement
        text = raw.decode('utf-8', errors='replace')
        changed = True
        print(f"  [FORCE] {os.path.relpath(path, REPO_DIR)}")

    # Normalize line endings to LF
    norm = text.replace('\r\n', '\n').replace('\r', '\n')
    if norm != text:
        changed = True
    text = norm

    if changed:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
        fixed.append(os.path.relpath(path, REPO_DIR))
    else:
        already_ok.append(path)

# Walk all .java files
for root, dirs, files in os.walk(SRC_DIR):
    for fname in files:
        if fname.endswith('.java'):
            process(os.path.join(root, fname))

print(f"\n{'='*50}")
print(f"Already clean UTF-8 (no changes): {len(already_ok)}")
print(f"Fixed (encoding/BOM/CRLF): {len(fixed)}")
print(f"\nFixed files:")
for f in sorted(fixed):
    print(f"  {f}")
