import zipfile
import re

jar_path = r"C:\Users\yatao\.m2\repository\com\oracle\oci\sdk\oci-java-sdk-shaded-full\3.45.2\oci-java-sdk-shaded-full-3.45.2.jar"
try:
    with zipfile.ZipFile(jar_path, 'r') as z:
        classes = z.namelist()
        updates = [c for c in classes if 'updateinstancedetails' in c.lower() or 'updatelaunchoptions' in c.lower()]
        for u in updates:
            print(u)
except Exception as e:
    print(e)
