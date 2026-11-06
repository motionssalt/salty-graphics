# Keep data classes and models for serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep class com.motionssalt.saltygraphics.engine.model.** { *; }
-keep class com.motionssalt.saltygraphics.engine.effect.** { *; }
-keep class com.motionssalt.saltygraphics.engine.action.** { *; }
