# منع التشفير من تغيير أو لمس أي كلاس بداخل كود البث والبروكسي
-keep class com.pira.gnetp.proxy.** { *; }
-keep class com.pira.gnetp.net.** { *; }
-keep class java.net.** { *; }
-keep class io.ktor.** { *; }

# قواعد الأمان العامة للواجهات والمكتبات الأساسية
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keep class * extends androidx.lifecycle.ViewModel { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn dagger.**
-dontwarn hilt_aggregated_deps.**
-dontwarn io.ktor.**

-repackageclasses ''
-allowaccessmodification
