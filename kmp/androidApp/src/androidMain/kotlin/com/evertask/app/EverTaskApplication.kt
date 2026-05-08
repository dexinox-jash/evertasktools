package com.evertask.app

import android.app.Application
import com.evertask.app.di.appModule
import com.evertask.shared.backup.BackupManager
import com.evertask.shared.data.repository.TaskDataSource
import com.evertask.shared.di.initKoinAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.loadKoinModules
import java.util.UUID

class EverTaskApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        val notificationToken: String = UUID.randomUUID().toString()
    }

    override fun onCreate() {
        super.onCreate()
        val koin = initKoinAndroid(this)
        loadKoinModules(appModule)

        val taskDataSource = koin.get<TaskDataSource>()
        val backupManager = koin.get<BackupManager>()

        appScope.launch {
            val tasks = taskDataSource.getAllTasks().first()
            if (tasks.isEmpty() && backupManager.getLatestBackupPath() != null) {
                runCatching { backupManager.restoreFromBackup() }
            }
        }
    }
}
