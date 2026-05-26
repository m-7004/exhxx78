import os

# 1. تحديث AndroidManifest بأمان
manifest = "app/src/main/AndroidManifest.xml"
with open(manifest, "r") as f:
    m_data = f.read()

if 'android:supportsPictureInPicture="true"' not in m_data:
    # نبحث عن نشاط MainActivity ونضيف الخصائص داخله
    m_data = m_data.replace('<activity', '<activity\n            android:supportsPictureInPicture="true"\n            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"', 1)
    with open(manifest, "w") as f:
        f.write(m_data)

# 2. حقن دالة PiP داخل MainActivity بدون مسح الكود القديم
activity = "app/src/main/java/com/pira/gnetp/MainActivity.kt"
with open(activity, "r") as f:
    a_data = f.read()

pip_code = """
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }
"""

# التأكد من عدم تكرار الحقن
if "onUserLeaveHint" not in a_data:
    # العثور على آخر قوس } في الملف وحقن الكود قبله
    last_brace_index = a_data.rfind('}')
    if last_brace_index != -1:
        new_data = a_data[:last_brace_index] + pip_code + a_data[last_brace_index:]
        with open(activity, "w") as f:
            f.write(new_data)
