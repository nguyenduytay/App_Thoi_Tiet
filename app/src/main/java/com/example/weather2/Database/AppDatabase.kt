package com.example.weather2.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.weather2.Model.Dao.Weather24hDao
import com.example.weather2.Model.Dao.Weather7dDao
import com.example.weather2.Model.Entity.Weather24h
import com.example.weather2.Model.Entity.Weather7d

    @Database(entities = [Weather24h::class, Weather7d::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun weather24hDao(): Weather24hDao
    abstract fun weather7dDao(): Weather7dDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database_new"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}