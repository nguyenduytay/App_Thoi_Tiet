    package com.example.weather2.Database

    import android.content.Context
    import androidx.room.Database
    import androidx.room.Room
    import androidx.room.RoomDatabase
    import com.example.weather2.Model.Dao.NotificationDao
    import com.example.weather2.Model.Dao.TimerDao
    import com.example.weather2.Model.Dao.WarningDao
    import com.example.weather2.Model.Entity.Notification
    import com.example.weather2.Model.Entity.Timer
    import com.example.weather2.Model.Entity.Warning

    @Database(entities = [Warning::class, Timer::class, Notification::class], version = 2 )
    abstract class AppDatabase : RoomDatabase() {
        abstract fun warningDao(): WarningDao
        abstract fun timerDao(): TimerDao
        abstract fun notificationDao(): NotificationDao

        companion object {
            @Volatile
            private var INSTANCE: AppDatabase? = null

            fun getDatabase(context: Context): AppDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    ).build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
//
//    @Database(
//        entities = [Warning::class, Timer::class, Notification::class],
//        version = 2, // Tăng version lên 2
//        exportSchema = false // Không cần export schema nếu dùng destructive migration
//    )
//    abstract class AppDatabase : RoomDatabase() {
//        abstract fun warningDao(): WarningDao
//        abstract fun timerDao(): TimerDao
//        abstract fun notificationDao(): NotificationDao
//
//        companion object {
//            @Volatile
//            private var INSTANCE: AppDatabase? = null
//
//            fun getDatabase(context: Context): AppDatabase {
//                return INSTANCE ?: synchronized(this) {
//                    val instance = Room.databaseBuilder(
//                        context.applicationContext,
//                        AppDatabase::class.java,
//                        "app_database"
//                    )
//                        .fallbackToDestructiveMigration() // XÓA TOÀN BỘ DB CŨ VÀ TẠO MỚI
//                        .build()
//                    INSTANCE = instance
//                    instance
//                }
//            }
//        }
//    }
