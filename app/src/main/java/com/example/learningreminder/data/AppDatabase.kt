package com.example.learningreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChildProfile::class, LearningTask::class, CompletionRecord::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun recordDao(): RecordDao
    abstract fun childDao(): ChildDao

    companion object {
        @Volatile
        private var INSTANCE: LearningDatabase? = null

        fun get(context: Context): LearningDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LearningDatabase::class.java,
                    "learning_reminder.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE learning_task ADD COLUMN adjustedReminderTime TEXT")
                db.execSQL("ALTER TABLE learning_task ADD COLUMN targetValue TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE completion_record ADD COLUMN evidenceText TEXT")
                db.execSQL("ALTER TABLE completion_record ADD COLUMN selfInitiated INTEGER")
                db.execSQL("ALTER TABLE completion_record ADD COLUMN exceededTarget INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE learning_task ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE completion_record ADD COLUMN backfilled INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
