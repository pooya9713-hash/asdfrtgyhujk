package com.homecontrol.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, DeviceEntity::class, CommandEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun historyDao(): HistoryDao

    companion object {
        val DEFAULT_CATEGORIES = listOf("روشنایی", "سرمایش و گرمایش", "امنیت", "سایر وسایل")

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "homecontrol.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DEFAULT_CATEGORIES.forEachIndexed { index, name ->
                            db.execSQL("INSERT INTO categories (name, sortOrder) VALUES ('$name', $index)")
                        }
                    }
                })
                .build()
    }
}
