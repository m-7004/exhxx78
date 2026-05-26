import os
files_to_check = [
    "app/src/main/java/com/pira/gnetp/ui/home/HomeScreen.kt",
    "app/src/main/java/com/pira/gnetp/ui/about/AboutScreen.kt"
]

# كود التشفير المدمج
base64_code = 'String(android.util.Base64.decode("aHR0cHM6Ly90Lm1lL2V4aHh4Nzg=", android.util.Base64.DEFAULT))'

for file_path in files_to_check:
    if os.path.exists(file_path):
        with open(file_path, "r") as f:
            data = f.read()
        
        # استبدال الرابط الصريح بالكود المشفر
        data = data.replace('"https://t.me/exhxx78"', base64_code)
        
        with open(file_path, "w") as f:
            f.write(data)
