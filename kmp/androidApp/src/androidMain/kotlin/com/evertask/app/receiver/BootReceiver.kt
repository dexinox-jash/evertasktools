package com.evertask.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.evertask.app.scheduler.AndroidReminderScheduler
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = GlobalContext.get().get<TaskRepository>()
                val tasks = repository.getActiveTasks().first()
                tasks.forEach { task ->
                    AndroidReminderScheduler(context).scheduleReminder(task)
                }
                val widgetUpdater: WidgetUpdater = GlobalContext.get().get<WidgetUpdater>()
                widgetUpdater.updateAllWidgets()
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule reminders after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
