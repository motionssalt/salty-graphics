# Preserve attributes for reflection, annotations, and stacktraces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Preserve all app code, models, viewmodels, and navigation
-keep class com.motionssalt.saltygraphics.** { *; }
-keepclassmembers class com.motionssalt.saltygraphics.** { *; }

# Preserve all engine code
-keep class com.motionssalt.saltygraphics.engine.** { *; }
-keepclassmembers class com.motionssalt.saltygraphics.engine.** { *; }

# Kotlinx Serialization Proguard rules
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <fields>;
    <methods>;
}
-keepclassmembers class * {
    *** Companion;
}
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
