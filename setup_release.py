import os

path = "app/build.gradle.kts"
if not os.path.exists(path):
    path = "app/build.gradle"

if os.path.exists(path):
    with open(path, "r") as f:
        content = f.read()
    
    # إضافة إعدادات التوقيع الرسمي (Keystore)
    signing_config = """
    signingConfigs {
        create("release") {
            storeFile = file("exhxx_release.jks")
            storePassword = "exhxx2026"
            keyAlias = "exhxx_alias"
            keyPassword = "exhxx2026"
        }
    }
"""
    
    if "signingConfigs {" not in content:
        content = content.replace("defaultConfig {", signing_config + "\n    defaultConfig {")
    
    # ربط التوقيع بنسخة الإصدار (Release)
    if 'getByName("release") {' in content:
        content = content.replace('getByName("release") {', 'getByName("release") {\n            signingConfig = signingConfigs.getByName("release")')
    elif 'release {' in content:
        content = content.replace('release {', 'release {\n            signingConfig signingConfigs.release')

    with open(path, "w") as f:
        f.write(content)
