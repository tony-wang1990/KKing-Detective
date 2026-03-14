#!/usr/bin/env python3
"""
Fix Java source file encoding: convert GBK-encoded files to UTF-8.
The files were created on Windows with Chinese locale (GBK/CP936) 
but committed as-is, causing 'unmappable character for encoding UTF-8'
errors in Maven builds on Linux.
"""

import os
import sys
import chardet

def fix_file(filepath):
    """Try to re-encode a file from its detected encoding to UTF-8."""
    with open(filepath, 'rb') as f:
        raw = f.read()
    
    # Try to detect encoding
    detected = chardet.detect(raw)
    encoding = detected.get('encoding', 'utf-8')
    confidence = detected.get('confidence', 0)
    
    print(f"  Detected: {encoding} (confidence: {confidence:.2f})")
    
    # If it's already valid UTF-8, skip
    if encoding and encoding.lower() in ('utf-8', 'ascii'):
        try:
            raw.decode('utf-8')
            print(f"  Already valid UTF-8, skipping.")
            return False
        except UnicodeDecodeError:
            pass
    
    # Try decoding with detected encoding, then fallback encodings
    text = None
    tried = []
    for enc in [encoding, 'gbk', 'gb2312', 'gb18030', 'utf-8-sig', 'latin-1']:
        if enc is None:
            continue
        try:
            text = raw.decode(enc)
            tried.append(enc)
            print(f"  Successfully decoded with: {enc}")
            break
        except (UnicodeDecodeError, LookupError) as e:
            tried.append(f"{enc}(failed)")
    
    if text is None:
        print(f"  ERROR: Could not decode file with any encoding! Tried: {tried}")
        return False
    
    # Check if this file actually has non-ASCII (Chinese/emoji) content
    non_ascii = sum(1 for c in text if ord(c) > 127)
    print(f"  Non-ASCII chars: {non_ascii}")
    
    # Write back as UTF-8
    with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    print(f"  -> Saved as UTF-8")
    return True

def main():
    base_dir = os.path.join(os.path.dirname(__file__), 'src', 'main', 'java')
    
    # Files mentioned in the error messages
    target_files = [
        'com/tony/kingdetective/telegram/handler/impl/TrafficStatisticsHandler.java',
        'com/tony/kingdetective/telegram/handler/impl/ConfigPageNavigationHandler.java',
        'com/tony/kingdetective/telegram/handler/impl/SystemMetricsHandler.java',
        'com/tony/kingdetective/telegram/handler/impl/CreateInstanceHandler.java',
        'com/tony/kingdetective/telegram/handler/impl/ConfigListHandler.java',
        'com/tony/kingdetective/telegram/handler/impl/InstanceManagementHandler.java',
        'com/tony/kingdetective/telegram/handler/CallbackHandler.java',
        'com/tony/kingdetective/telegram/builder/KeyboardBuilder.java',
    ]
    
    fixed_count = 0
    error_count = 0
    
    for rel_path in target_files:
        filepath = os.path.join(base_dir, rel_path.replace('/', os.sep))
        if not os.path.exists(filepath):
            print(f"\nFile not found: {filepath}")
            error_count += 1
            continue
        
        print(f"\nProcessing: {rel_path}")
        try:
            if fix_file(filepath):
                fixed_count += 1
        except Exception as e:
            print(f"  ERROR: {e}")
            error_count += 1
    
    print(f"\n{'='*60}")
    print(f"Done! Fixed: {fixed_count}, Errors: {error_count}")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()
