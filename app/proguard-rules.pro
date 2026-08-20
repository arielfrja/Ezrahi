# Stack-trace fidelity for exception logging (spec todo-fix-6 §5.7)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep domain models and Room entities safe for deserialization
-keepclassmembers class com.arielfaridja.ezrahi.domain.model.** { *; }
-keepclassmembers class com.arielfaridja.ezrahi.data.local.** { *; }

# Keep exception class names readable in stack traces
-keepnames class * extends java.lang.Throwable