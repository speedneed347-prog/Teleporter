package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ScriptEntity

@Database(
    entities = [ScriptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TeleprompterDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    companion object {
        @Volatile
        private var INSTANCE: TeleprompterDatabase? = null

        fun getInstance(context: Context): TeleprompterDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TeleprompterDatabase::class.java,
                    "smart_teleprompter_pro.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
