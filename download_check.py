import urllib.request
import zipfile
import io

url = "https://repo1.maven.org/maven2/com/oracle/oci/sdk/oci-java-sdk-core/3.45.2/oci-java-sdk-core-3.45.2-sources.jar"
print(f"Downloading {url}...")
req = urllib.request.urlopen(url)
content = req.read()
print("Downloaded, extracting UpdateInstanceDetails.java and UpdateLaunchOptions.java...")

with zipfile.ZipFile(io.BytesIO(content)) as z:
    for name in z.namelist():
        if "UpdateInstanceDetails.java" in name:
            print("--- " + name + " ---")
            code = z.read(name).decode('utf-8')
            for line in code.split('\n'):
                if "import" not in line and "Launch" in line:
                    print(line.strip())
                elif "import" not in line and "Shape" in line:
                    print(line.strip())
        elif "UpdateLaunchOptions" in name:
            print("Found: " + name)
