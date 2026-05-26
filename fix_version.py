import os, re
paths = ["app/build.gradle", "app/build.gradle.kts"]
for path in paths:
    if os.path.exists(path):
        with open(path, "r") as f:
            data = f.read()
        
        # تصفير versionCode
        if path.endswith('.kts'):
            data = re.sub(r'versionCode\s*=\s*[0-9]+', 'versionCode = 1', data)
            data = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.0.0"', data)
        else:
            data = re.sub(r'versionCode\s+[0-9]+', 'versionCode 1', data)
            data = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.0.0"', data)
            
        with open(path, "w") as f:
            f.write(data)
