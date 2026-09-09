package com.appcontrol.data.local

import android.content.Context
import com.appcontrol.core.database.AppDatabase
import com.appcontrol.core.database.dao.ActivityEventDao
import com.appcontrol.core.database.dao.AppPolicyDao
import com.appcontrol.core.database.dao.ApplicationDao
import com.appcontrol.core.database.dao.HistoryDao
import com.appcontrol.core.database.dao.ProfileDao

class AppDatabaseProvider(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    val applicationDao: ApplicationDao by lazy { database.applicationDao() }
    val appPolicyDao: AppPolicyDao by lazy { database.appPolicyDao() }
    val profileDao: ProfileDao by lazy { database.profileDao() }
    val historyDao: HistoryDao by lazy { database.historyDao() }
    val activityEventDao: ActivityEventDao by lazy { database.activityEventDao() }
}