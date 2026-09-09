package com.appcontrol.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.appcontrol.core.common.Constants
import com.appcontrol.core.database.dao.ActivityEventDao
import com.appcontrol.core.database.dao.AppPolicyDao
import com.appcontrol.core.database.dao.ApplicationDao
import com.appcontrol.core.database.dao.HistoryDao
import com.appcontrol.core.database.dao.ProfileDao
import com.appcontrol.core.database.entity.ActivityEventEntity
import com.appcontrol.core.database.entity.AppPolicyEntity
import com.appcontrol.core.database.entity.ApplicationInfoEntity
import com.appcontrol.core.database.entity.HistoryEventEntity
import com.appcontrol.core.database.entity.ProfileAppEntity
import com.appcontrol.core.database.entity.ProfileEntity

@Database(
    entities = [
        ApplicationInfoEntity::class,
        AppPolicyEntity::class,
        ProfileEntity::class,
        ProfileAppEntity::class,
        HistoryEventEntity::class,
        ActivityEventEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun applicationDao(): ApplicationDao
    abstract fun appPolicyDao(): AppPolicyDao
    abstract fun profileDao(): ProfileDao
    abstract fun historyDao(): HistoryDao
    abstract fun activityEventDao(): ActivityEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                Constants.DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
