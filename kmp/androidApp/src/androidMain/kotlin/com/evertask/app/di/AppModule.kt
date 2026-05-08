package com.evertask.app.di

import com.evertask.app.backup.AndroidBackupManager
import com.evertask.app.scheduler.AndroidReminderScheduler
import com.evertask.app.scheduler.ReminderScheduler
import com.evertask.app.ui.TaskViewModel
import com.evertask.app.widget.GlanceWidgetUpdater
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.backup.BackupManager
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { TaskViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<BackupManager> { AndroidBackupManager(get(), get(), get()) }
    single<ReminderScheduler> { AndroidReminderScheduler(get()) }
    single<WidgetUpdater> { GlanceWidgetUpdater(get()) }
}
