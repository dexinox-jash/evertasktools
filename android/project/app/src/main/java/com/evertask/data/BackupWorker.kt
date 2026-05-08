package com.evertask.data.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.evertask.data.preferences.DataStoreManager
import com.evertask.data.security.SecureBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for automatic daily encrypted backups.
 *
 * Schedule characteristics:
 * - Runs daily (24 hour interval)
 * - 15-minute deferral for battery awareness
 * - Requires battery not low
 * - Requires storage not low
 * - No network required (offline-first app)
 * - Exponential backoff on failure (15 min initial, up to 2 hours)
 * - Uses AES-256-GCM encryption via SecureBackupManager
 *
 * SECURITY: All backups are encrypted using AndroidX Security library
 * with hardware-backed encryption when available.
 *
 * To schedule: BackupWorker.schedule(context)
 * To cancel: BackupWorker.cancel(context)
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val WORK_NAME = "evertask_daily_backup"
        private const val REPEAT_INTERVAL_HOURS = 24L
        private const val FLEX_INTERVAL_MINUTES = 15L
        private const val INITIAL_DELAY_MINUTES = 15L
        private const val BACKOFF_DELAY_MINUTES = 15L
        private const val MAX_BACKOFF_MINUTES = 120L

        /**
         * Schedules the daily encrypted backup worker.
         * Uses KEEP policy to preserve existing schedule if already set.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                // No network constraint - this is an offline app
                .build()

            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                REPEAT_INTERVAL_HOURS, TimeUnit.HOURS,
                FLEX_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                backupRequest
            )
        }

        /**
         * Cancels the scheduled backup worker.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Checks if backup worker is currently scheduled.
         */
        fun isScheduled(context: Context): Boolean {
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            return workInfos?.any {
                !it.state.isFinished
            } ?: false
        }

        /**
         * Runs an immediate one-time encrypted backup.
         * Use for manual backup trigger.
         */
        fun runImmediate(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val backupWork = androidx.work.OneTimeWorkRequestBuilder<BackupWorker>()
                .addTag("${WORK_NAME}_immediate")
                .build()
            workManager.enqueue(backupWork)
        }
    }

    /**
     * Main worker execution.
     * Runs on background thread - can use blocking calls.
     */
    override fun doWork(): Result {
        return runBlocking {
            performSecureBackup()
        }
    }

    /**
     * Performs the actual encrypted backup operation.
     */
    private suspend fun performSecureBackup(): Result = withContext(Dispatchers.IO) {
        runCatching {
            // Check if backup is enabled
            val dataStoreManager = DataStoreManager.getInstance(applicationContext)
            val backupEnabled = dataStoreManager.isBackupEnabled()

            if (!backupEnabled) {
                // Backup disabled, skip but mark as success
                return@withContext Result.success()
            }

            // Check if backup is overdue (avoid duplicate backups)
            val isOverdue = dataStoreManager.isBackupOverdue()
            if (!isOverdue) {
                // Backup not needed yet
                return@withContext Result.success()
            }

            // Perform encrypted backup using SecureBackupManager
            val secureBackupManager = SecureBackupManager.getInstance(applicationContext)

            // Migrate legacy backup if exists
            secureBackupManager.migrateLegacyBackup()

            // Export encrypted backup
            val result = secureBackupManager.exportSecureBackup()

            if (result.success) {
                // Update timestamp on success
                dataStoreManager.setLastBackupTimestamp()
                Result.success()
            } else {
                // Retry on failure
                Result.retry()
            }
        }.getOrElse {
            // Any exception triggers retry
            Result.retry()
        }
    }
}

/**
 * Utility class for managing backup scheduling.
 */
object BackupScheduler {

    /**
     * Initializes backup scheduling based on user preferences.
     * Call during app startup.
     *
     * Also migrates any legacy unencrypted backups to encrypted format.
     */
    fun initialize(context: Context) {
        val dataStoreManager = DataStoreManager.getInstance(context)

        // Use runBlocking for synchronous check during startup
        val backupEnabled = runBlocking {
            dataStoreManager.isBackupEnabled()
        }

        if (backupEnabled) {
            BackupWorker.schedule(context)
        }

        // Attempt to migrate legacy backup in background
        runBlocking {
            try {
                val secureBackupManager = SecureBackupManager.getInstance(context)
                secureBackupManager.migrateLegacyBackup()
            } catch (e: Exception) {
                // Migration failure should not block app startup
            }
        }
    }

    /**
     * Enables automatic encrypted backups and schedules worker.
     */
    fun enableBackups(context: Context) {
        val dataStoreManager = DataStoreManager.getInstance(context)
        runBlocking {
            dataStoreManager.setBackupEnabled(true)
        }
        BackupWorker.schedule(context)
    }

    /**
     * Disables automatic backups and cancels worker.
     */
    fun disableBackups(context: Context) {
        val dataStoreManager = DataStoreManager.getInstance(context)
        runBlocking {
            dataStoreManager.setBackupEnabled(false)
        }
        BackupWorker.cancel(context)
    }

    /**
     * Triggers an immediate manual encrypted backup.
     */
    fun backupNow(context: Context) {
        BackupWorker.runImmediate(context)
    }

    /**
     * Gets current backup status for UI display.
     * Shows whether backup is encrypted or legacy.
     */
    suspend fun getStatus(context: Context): BackupStatus {
        val dataStoreManager = DataStoreManager.getInstance(context)
        val secureBackupManager = SecureBackupManager.getInstance(context)

        val hasEncryptedBackup = secureBackupManager.hasBackup()
        val hasLegacyBackup = secureBackupManager.hasLegacyBackup()

        return BackupStatus(
            isEnabled = dataStoreManager.isBackupEnabled(),
            isScheduled = BackupWorker.isScheduled(context),
            lastBackupDate = secureBackupManager.getLastBackupDate(),
            backupSize = secureBackupManager.getFormattedBackupSize(),
            hasBackup = hasEncryptedBackup || hasLegacyBackup,
            isOverdue = dataStoreManager.isBackupOverdue(),
            isEncrypted = hasEncryptedBackup,
            hasLegacyBackup = hasLegacyBackup
        )
    }
}

/**
 * Data class for backup status UI.
 */
data class BackupStatus(
    val isEnabled: Boolean,
    val isScheduled: Boolean,
    val lastBackupDate: java.util.Date?,
    val backupSize: String,
    val hasBackup: Boolean,
    val isOverdue: Boolean,
    val isEncrypted: Boolean = false,
    val hasLegacyBackup: Boolean = false
)
