package com.davidhuynh.levelup.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.davidhuynh.levelup.data.local.dao.ExerciseDao
import com.davidhuynh.levelup.data.local.dao.ExerciseSetDao
import com.davidhuynh.levelup.data.local.dao.FriendDao
import com.davidhuynh.levelup.data.local.dao.PersonalRecordDao
import com.davidhuynh.levelup.data.local.dao.UserDao
import com.davidhuynh.levelup.data.local.dao.UserStatsDao
import com.davidhuynh.levelup.data.local.dao.WorkoutDao
import com.davidhuynh.levelup.data.local.entity.ExerciseEntity
import com.davidhuynh.levelup.data.local.entity.ExerciseSetEntity
import com.davidhuynh.levelup.data.local.entity.FriendRequestEntity
import com.davidhuynh.levelup.data.local.entity.FriendshipEntity
import com.davidhuynh.levelup.data.local.entity.PersonalRecordEntity
import com.davidhuynh.levelup.data.local.entity.UserEntity
import com.davidhuynh.levelup.data.local.entity.UserStatsEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutEntity
import com.davidhuynh.levelup.data.local.entity.WorkoutExerciseEntity

@Database(
    entities = [
        UserEntity::class,
        UserStatsEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class,
        PersonalRecordEntity::class,
        FriendshipEntity::class,
        FriendRequestEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LevelUpDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun friendDao(): FriendDao

    companion object {
        const val NAME = "levelup.db"

        fun build(context: Context, onOpened: (LevelUpDatabase) -> Unit): LevelUpDatabase {
            var instance: LevelUpDatabase? = null
            val callback = object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {

                    instance?.let(onOpened)
                }
            }
            return Room.databaseBuilder(context, LevelUpDatabase::class.java, NAME)
                .addCallback(callback)
                .build()
                .also { instance = it }
        }
    }
}
