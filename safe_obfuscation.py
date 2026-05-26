import os, re

paths = ["app/build.gradle.kts", "app/build.gradle"]
for path in paths:
    if os.path.exists(path):
        with open(path, "r") as f:
            content = f.read()
        
        # تفعيل التشفير وتقليص الحجم
        content = re.sub(r'isMinifyEnabled\s*=\s*false', 'isMinifyEnabled = true', content)
        content = re.sub(r'minifyEnabled\s*=\s*false', 'minifyEnabled = true', content)
        content = re.sub(r'isShrinkResources\s*=\s*false', 'isShrinkResources = true', content)
        content = re.sub(r'shrinkResources\s*=\s*false', 'shrinkResources = true', content)
        content = re.sub(r'minifyEnabled\s+false', 'minifyEnabled true', content)
        content = re.sub(r'shrinkResources\s+false', 'shrinkResources true', content)
        
        # حقن التشفير إجبارياً داخل نسخة المصنع
        if "debug {" in content and "isMinifyEnabled" not in content and "minifyEnabled true" not in content:
            content = content.replace("debug {", "debug {\n            isMinifyEnabled = true\n            minifyEnabled = true\n            proguardFiles(getDefaultProguardFile(\"proguard-android-optimize.txt\"), \"proguard-rules.pro\")")
        
        if 'getByName("debug")' in content and "isMinifyEnabled" not in content:
            content = content.replace('getByName("debug") {', 'getByName("debug") {\n            isMinifyEnabled = true\n            proguardFiles(getDefaultProguardFile(\"proguard-android-optimize.txt\"), \"proguard-rules.pro\")')

        with open(path, "w") as f:
            f.write(content)
