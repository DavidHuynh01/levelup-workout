-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

-keepattributes Signature
-keepattributes *Annotation*

-dontwarn org.jetbrains.annotations.**
