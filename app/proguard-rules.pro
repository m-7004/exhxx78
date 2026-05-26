# --- قواعد الأمان لضمان عمل الواجهات (Compose) ومكتبة (Hilt) ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keep class * extends androidx.lifecycle.ViewModel { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn dagger.**
-dontwarn hilt_aggregated_deps.**

# التعتيم القوي على باقي الكود (منع برامج مثل MT Manager من قراءة الكود)
-repackageclasses ''
-allowaccessmodification
