# Keep Room generated implementations reachable when minifying.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
