package com.homecontrol.app

import android.app.Application
import android.content.Context
import com.homecontrol.app.data.AppDatabase
import com.homecontrol.app.data.BackupManager
import com.homecontrol.app.data.HomeRepository
import com.homecontrol.app.data.SettingsRepository
import com.homecontrol.app.sms.SmsSender

class HomeControlApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Simple manual dependency container (no DI framework needed for an app this size). */
class AppContainer(val appContext: Context) {
    val database: AppDatabase = AppDatabase.build(appContext)
    val settings = SettingsRepository(appContext)
    val repository = HomeRepository(database)
    val smsSender = SmsSender(appContext)
    val backup = BackupManager(database, settings)
}
