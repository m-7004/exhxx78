import os
file_path = "app/src/main/java/com/pira/gnetp/MainActivity.kt"
with open(file_path, "r") as f:
    data = f.read()

pip_code = """
    // دالة النافذة العائمة (PiP)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }
"""

# الزرع الآمن: نضع الدالة فوق onCreate مباشرة لضمان بقائها داخل الكلاس الأساسي
if "onUserLeaveHint" not in data:
    data = data.replace("override fun onCreate", pip_code + "\n    override fun onCreate", 1)
    with open(file_path, "w") as f:
        f.write(data)
