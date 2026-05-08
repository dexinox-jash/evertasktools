package com.evertask.data.backup

import android.content.Context
import android.os.Environment
import com.evertask.data.dao.TaskDao
import com.evertask.data.database.TaskDatabase
import com.evertask.data.entity.TaskEntity
import com.evertask.data.model.Subtask
import com.evertask.data.preferences.DataStoreManager
import com.evertask.data.security.SecureBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * **DEPRECATED**: Use [SecureBackupManager] instead.
 *
 * This class is kept for backward compatibility and delegates all operations
 * to [SecureBackupManager] which provides AES-256-GCM encryption.
 *
 * SECURITY WARNING: The legacy methods in this class wrote backups to
 * `/Download/EverTask/backup.json` without encryption. This was a security
 * vulnerability (Issue AND-001). All operations now use encrypted storage.
 *
 * Migration path:
 * - Replace `BackupManager.getInstance(context)` with `SecureBackupManager.getInstance(context)`
 * - Replace `exportToJson()` with `exportSecureBackup()`
 * - Replace `importFromJson()` with `importSecureBackup()`
 */
@Deprecated(
    message = "Use SecureBackupManager for encrypted backups. This class is kept for backward compatibility only.",
    replaceWith = ReplaceWith("SecureBackupManager"),
    level = DeprecationLevel.WARNING
)
class BackupManager private constructor(
    private val context: Context,
    private val taskDao: TaskDao,
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        private const val BACKUP_DIR_NAME = "EverTask"
        private const val BACKUP_FILE_NAME = "backup.json"
        private const val BACKUP_FILE_NAME_DATED = "evertask_backup_%s.json"
        private const val MAX_BACKUP_FILES = 10

        @Volatile
        private var INSTANCE: BackupManager? = null

        /**
         * Gets singleton BackupManager instance.
         *
         * **DEPRECATED**: Use [SecureBackupManager.getInstance] instead.
         */
        @Deprecated(
            message = "Use SecureBackupManager.getInstance() for encrypted backups",
            replaceWith = ReplaceWith(
                "SecureBackupManager.getInstance(context)",
                "com.evertask.data.security.SecureBackupManager"
            ),
            level = DeprecationLevel.WARNING
        )
        fun getInstance(context: Context): BackupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createInstance(context).also { INSTANCE = it }
            }
        }

        private fun createInstance(context: Context): BackupManager {
            val database = TaskDatabase.getInstance(context)
            return BackupManager(
                context = context,
                taskDao = database.taskDao(),
                dataStoreManager = DataStoreManager.getInstance(context)
            )
        }

        /**
         * JSON serializer configuration for backup files.
         */
        private val backupJson = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    // Delegate to secure backup manager
    private val secureBackupManager by lazy {
        SecureBackupManager.getInstance(context)
    }

    /**
     * Result of a backup operation.
     */
    data class BackupResult(
        val success: Boolean,
        val filePath: String? = null,
        val tasksBackedUp: Int = 0,
        val errorMessage: String? = null
    )

    /**
     * Result of a restore operation.
     */
    data class RestoreResult(
        val success: Boolean,
        val tasksRestored: Int = 0,
        val tasksSkipped: Int = 0,
        val errorMessage: String? = null
    )

    // ==================== EXPORT OPERATIONS (DELEGATED) ====================

    /**
     * **DEPRECATED**: Use [SecureBackupManager.exportSecureBackup] instead.
     *
     * Exports all active tasks to an encrypted backup file.
     * Now delegates to [SecureBackupManager] for security.
     *
     * Previously saved to Downloads/EverTask/backup.json (unencrypted).
     * Now saves to app-private encrypted storage.
     */
    @Deprecated(
        message = "Use SecureBackupManager.exportSecureBackup() for encrypted backups",
        replaceWith = ReplaceWith(
            "SecureBackupManager.getInstance(context).exportSecureBackup()",
            "com.evertask.data.security.SecureBackupManager"
        ),
        level = DeprecationLevel.WARNING
    )
    suspend fun exportToJson(): BackupResult = withContext(Dispatchers.IO) {
        val result = secureBackupManager.exportSecureBackup()

        // Migrate legacy backup if it exists
        migrateLegacyIfNeeded()

        BackupResult(
            success = result.success,
            filePath = result.filePath,
            tasksBackedUp = result.tasksBackedUp,
            errorMessage = result.errorMessage
        )
    }

    /**
     * **DEPRECATED**: Use [SecureBackupManager.exportToFile] instead.
     *
     * Exports to a specific file path with encryption.
     */
    @Deprecated(
        message = "Use SecureBackupManager.exportToFile() for encrypted backups",
        replaceWith = ReplaceWith(
            "SecureBackupManager.getInstance(context).exportToFile(customPath)",
            "com.evertask.data.security.SecureBackupManager"
        ),
        level = DeprecationLevel.WARNING
    )
    suspend fun exportToFile(customPath: String): BackupResult = withContext(Dispatchers.IO) {
        val result = secureBackupManager.exportToFile(customPath)
        BackupResult(
            success = result.success,
            filePath = result.filePath,
            tasksBackedUp = result.tasksBackedUp,
            errorMessage = result.errorMessage
        )
    }

    // ==================== IMPORT OPERATIONS (DELEGATED) ====================

    /**
     * **DEPRECATED**: Use [SecureBackupManager.importSecureBackup] instead.
     *
     * Imports tasks from backup file.
     * Supports both encrypted and legacy unencrypted backups.
     */
    @Deprecated(
        message = "Use SecureBackupManager.importSecureBackup() for encrypted backups",
        replaceWith = ReplaceWith(
            "SecureBackupManager.getInstance(context).importSecureBackup(filePath)",
            "com.evertask.data.security.SecureBackupManager"
        ),
        level = DeprecationLevel.WARNING
    )
    suspend fun importFromJson(filePath: String? = null): RestoreResult = withContext(Dispatchers.IO) {
        val result = secureBackupManager.importSecureBackup(filePath)
        RestoreResult(
            success = result.success,
            tasksRestored = result.tasksRestored,
            tasksSkipped = result.tasksSkipped,
            errorMessage = result.errorMessage
        )
    }

    /**
     * **DEPRECATED**: Use [SecureBackupManager.recoverFromBackup] instead.
     *
     * Attempts database recovery from backup.
     */
    @Deprecated(
        message = "Use SecureBackupManager.recoverFromBackup() for encrypted backups",
        replaceWith = ReplaceWith(
            "SecureBackupManager.getInstance(context).recoverFromBackup()",
            "com.evertask.data.security.SecureBackupManager"
        ),
        level = DeprecationLevel.WARNING
    )
    suspend fun recoverFromBackup(): RestoreResult = withContext(Dispatchers.IO) {
        val result = secureBackupManager.recoverFromBackup()
        RestoreResult(
            success = result.success,
            tasksRestored = result.tasksRestored,
            tasksSkipped = result.tasksSkipped,
            errorMessage = result.errorMessage
        )
    }

    // ==================== UTILITY FUNCTIONS (DELEGATED) ====================

    /**
     * Checks if a backup file exists.
     */
    fun hasBackup(): Boolean {
        return secureBackupManager.hasBackup() || hasLegacyBackup()
    }

    /**
     * Gets the backup file size in bytes.
     */
    fun getBackupSize(): Long {
        return if (secureBackupManager.hasBackup()) {
            secureBackupManager.getBackupSize()
        } else {
            getLegacyBackupFile().length()
        }
    }

    /**
     * Gets formatted backup file size for display.
     */
    fun getFormattedBackupSize(): String {
        return if (secureBackupManager.hasBackup()) {
            secureBackupManager.getFormattedBackupSize()
        } else {
            val size = getLegacyBackupFile().length()
            when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            }
        }
    }

    /**
     * Gets the last backup file modification date.
     */
    fun getLastBackupDate(): Date? {
        return if (secureBackupManager.hasBackup()) {
            secureBackupManager.getLastBackupDate()
        } else {
            val file = getLegacyBackupFile()
            if (file.exists()) Date(file.lastModified()) else null
        }
    }

    /**
     * Deletes the backup file.
     */
    suspend fun deleteBackup(): Boolean = withContext(Dispatchers.IO) {
        secureBackupManager.deleteBackup() && deleteLegacyBackup()
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Gets the legacy backup file.
     */
    private fun getLegacyBackupFile(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val backupDir = File(downloadsDir, BACKUP_DIR_NAME)
        return File(backupDir, BACKUP_FILE_NAME)
    }

    /**
     * Checks if legacy backup exists.
     */
    private fun hasLegacyBackup(): Boolean {
        return getLegacyBackupFile().exists()
    }

    /**
     * Deletes legacy unencrypted backup.
     */
    private fun deleteLegacyBackup(): Boolean {
        val legacyFile = getLegacyBackupFile()
        return if (legacyFile.exists()) {
            legacyFile.delete()
        } else {
            true
        }
    }

    /**
     * Migrates legacy backup to encrypted if needed.
     */
    private suspend fun migrateLegacyIfNeeded() {
        if (hasLegacyBackup()) {
            secureBackupManager.migrateLegacyBackup()
        }
    }
}

// ==================== BACKUP DATA CLASSES ====================

/**
 * Root structure of backup JSON file.
 */
@Serializable
data class BackupData(
    val version: Int,
    val exportDate: Long,
    val appVersion: String,
    val tasks: List<BackupTask>
)

/**
 * Serializable task representation for backup.
 */
@Serializable
data class BackupTask(
    val id: String,
    val title: String,
    val subtasks: List<BackupSubtask>,
    val templateId: String? = null,
    val icon: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val completedAt: Long? = null,
    val estimatedMinutes: Int
) {
    companion object {
        fun fromEntity(entity: TaskEntity): BackupTask {
            return BackupTask(
                id = entity.id,
                title = entity.title,
                subtasks = entity.getSubtasks().map { BackupSubtask.fromSubtask(it) },
                templateId = entity.templateId,
                icon = entity.icon,
                isCompleted = entity.isCompleted,
                createdAt = entity.createdAt,
                completedAt = entity.completedAt,
                estimatedMinutes = entity.estimatedMinutes
            )
        }
    }

    fun toTaskEntity(): TaskEntity {
        return TaskEntity.create(
            id = id,
            title = title,
            subtasks = subtasks.map { it.toSubtask() },
            templateId = templateId,
            icon = icon,
            estimatedMinutes = estimatedMinutes
        ).copy(
            isCompleted = isCompleted,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }
}

/**
 * Serializable subtask representation for backup.
 */
@Serializable
data class BackupSubtask(
    val id: String,
    val text: String,
    val durationMinutes: Int,
    val isCompleted: Boolean
) {
    companion object {
        fun fromSubtask(subtask: Subtask): BackupSubtask {
            return BackupSubtask(
                id = subtask.id,
                text = subtask.text,
                durationMinutes = subtask.durationMinutes,
                isCompleted = subtask.isCompleted
            )
        }
    }

    fun toSubtask(): Subtask {
        return Subtask(
            id = id,
            text = text,
            durationMinutes = durationMinutes,
            isCompleted = isCompleted
        )
    }
}
