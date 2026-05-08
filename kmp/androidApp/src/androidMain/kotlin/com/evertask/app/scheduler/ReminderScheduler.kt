package com.evertask.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.evertask.app.receiver.TaskReminderReceiver
import com.evertask.shared.domain.model.Task

interface ReminderScheduler {
    fun scheduleReminder(task: Task, delayMs: Long = DEFAULT_REMINDER_DELAY_MS): Boolean
    fun cancelReminder(taskId: String)
    fun snoozeReminder(taskId: String): Boolean
    fun canScheduleExactAlarms(): Boolean
    fun openExactAlarmSettings()
}

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    private fun stableRequestCode(id: String): Int {
        return kotlin.math.abs(id.fold(0) { acc, c -> 31 * acc + c.code })
    }

    override fun scheduleReminder(task: Task, delayMs: Long): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = System.currentTimeMillis() + delayMs

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("taskId", task.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode(task.id),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        return true
    }

    override fun cancelReminder(taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("taskId", taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode(taskId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun snoozeReminder(taskId: String): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = System.currentTimeMillis() + SNOOZE_DELAY_MS

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("taskId", taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode(taskId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        return true
    }

    override fun canScheduleExactAlarms(): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private const val DEFAULT_REMINDER_DELAY_MS = 15 * 60 * 1000L // 15 minutes
private const val SNOOZE_DELAY_MS = 15 * 60 * 1000L // 15 minutes
