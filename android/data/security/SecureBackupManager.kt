package com.evertask.data.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.evertask.data.dao.TaskDao
import com.evertask.data.database.TaskDatabase
import com.evertask.data.entity.TaskEntity
import com.evertask.data.model.Subtask
import com.evertask.data.preferences.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Secure backup manager for Ever Task Tools.
 *
 * SECURITY FEATURES:
 * - AES-256-GCM encryption using AndroidX Security library
 * - MasterKey stored in Android Keystore
 * - Backups stored in app-private external storage (not world-readable)
 * - Backward compatibility with legacy unencrypted backups
 *
 * PRIVACY-FIRST DESIGN:
 * - No cloud backup (android:allowBackup="false" in manifest)
 * - All data remains on-device
 * - Encrypted at rest using hardware-backed encryption when available
 *
 * @param context Application context
 * @param taskDao Data access object for task operations
 * @param dataStoreManager Preferences manager for backup metadata
 */
class SecureBackupManager private constructor(
    private val context: Context,
    private val taskDao: TaskDao,
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        private const val BACKUP_DIR_NAME = "backups"
        private const val BACKUP_FILE_NAME = "evertask_backup.enc"
        private const val BACKUP_FILE_NAME_DATED = "evertask_backup_%s.enc"
        private const val MAX_BACKUP_FILES = 10
        private const val LEGACY_BACKUP_DIR = "EverTask"
        private const val LEGACY_BACKUP_FILE = "backup.json"

        @Volatile
        private var INSTANCE: SecureBackupManager? = null

        /**
         * Gets singleton SecureBackupManager instance.
         */
        fun getInstance(context: Context): SecureBackupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createInstance(context).also { INSTANCE = it }
            }
        }

        private fun createInstance(context: Context): SecureBackupManager {
            val database = TaskDatabase.getInstance(context)
            return SecureBackupManager(
                context = context.applicationContext,
                taskDao = database.taskDao(),
                dataStoreManager = DataStoreManager.getInstance(context)
            )
        }

        /**
         * JSON serializer configuration for backup data.
         */
        private val backupJson = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    /**
     * Master key for encryption/decryption.
     * Uses AES256_GCM with Android Keystore.
     */
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Encrypted file handle for the main backup file.
     */
    private val encryptedFile: EncryptedFile by lazy {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR_NAME)
        backupDir.mkdirs()
        val file = File(backupDir, BACKUP_FILE_NAME)

        EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    /**
     * Result of a backup operation.
     */
    data class BackupResult(
        val success: Boolean,
        val filePath: String? = null,
        val tasksBackedUp: Int = 0,
        val errorMessage: String? = null,
        val isEncrypted: Boolean = true
    )

    /**
     * Result of a restore operation.
     */
    data class RestoreResult(
        val success: Boolean,
        val tasksRestored: Int = 0,
        val tasksSkipped: Int = 0,
        val errorMessage: String? = null,
        val migratedFromLegacy: Boolean = false
    )

    // ==================== EXPORT OPERATIONS ====================

    /**
     * Exports all active tasks to an encrypted backup file.
     * Saves to app-private external storage with AES-256-GCM encryption.
     *
     * @return BackupResult with file path and task count
     */
    suspend fun exportSecureBackup(): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            // Get all active tasks
            val tasks = taskDao.getAllActive().first()

            if (tasks.isEmpty()) {
                return@runCatching BackupResult(
                    success = true,
                    tasksBackedUp = 0,
                    errorMessage = "No tasks to backup",
                    isEncrypted = true
                )
            }

            // Convert to backup format
            val backupData = createBackupData(tasks)
            val jsonString = backupJson.encodeToString(backupData)

            // Write encrypted backup
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
            }

            // Also create dated backup for history
            createDatedEncryptedBackup(jsonString)

            // Update last backup timestamp
            dataStoreManager.setLastBackupTimestamp()

            // Cleanup old backups
            cleanupOldBackups()

            // Delete legacy unencrypted backup if it exists (security cleanup)
            deleteLegacyBackup()

            BackupResult(
                success = true,
                filePath = getBackupFile().absolutePath,
                tasksBackedUp = tasks.size,
                isEncrypted = true
            )
        }.getOrElse { error ->
            BackupResult(
                success = false,
                errorMessage = error.message ?: "Unknown backup error",
                isEncrypted = true
            )
        }
    }

    /**
     * Exports to a custom file path (for share/export functionality).
     * Creates an encrypted file at the specified location.
     */
    suspend fun exportToFile(customPath: String): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val tasks = taskDao.getAllActive().first()
            val backupData = createBackupData(tasks)
            val jsonString = backupJson.encodeToString(backupData)

            val file = File(customPath)
            file.parentFile?.mkdirs()

            // Create encrypted file at custom location
            val customEncryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            customEncryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
            }

            dataStoreManager.setLastBackupTimestamp()

            BackupResult(
                success = true,
                filePath = customPath,
                tasksBackedUp = tasks.size,
                isEncrypted = true
            )
        }.getOrElse { error ->
            BackupResult(
                success = false,
                errorMessage = error.message ?: "Export failed",
                isEncrypted = true
            )
        }
    }

    // ==================== IMPORT/RESTORE OPERATIONS ====================

    /**
     * Imports tasks from encrypted backup file.
     * Supports both new encrypted backups and legacy unencrypted backups.
     *
     * @param filePath Optional custom file path, uses default if null
     * @return RestoreResult with counts and migration status
     */
    suspend fun importSecureBackup(filePath: String? = null): RestoreResult = withContext(Dispatchers.IO) {
        runCatching {
            val backupFile = filePath?.let { File(it) } ?: getBackupFile()

            if (!backupFile.exists()) {
                // Try legacy backup for backward compatibility
                return@runCatching tryImportLegacyBackup()
            }

            // Read encrypted backup
            val jsonString = readEncryptedFile(backupFile)
                ?: return@runCatching RestoreResult(
                    success = false,
                    errorMessage = "Failed to decrypt backup file"
                )

            processBackupImport(jsonString)
        }.getOrElse { error ->
            RestoreResult(
                success = false,
                errorMessage = error.message ?: "Import failed"
            )
        }
    }

    /**
     * Attempts database recovery from backup.
     * Called when database corruption is detected.
     */
    suspend fun recoverFromBackup(): RestoreResult = withContext(Dispatchers.IO) {
        runCatching {
            val backupFile = getBackupFile()

            if (!backupFile.exists()) {
                // Try legacy backup for recovery
                return@runCatching tryRecoverLegacyBackup()
            }

            val jsonString = readEncryptedFile(backupFile)
                ?: return@runCatching RestoreResult(
                    success = false,
                    errorMessage = "Backup file corrupted or unreadable"
                )

            val backupData = parseBackupJson(jsonString)
                ?: return@runCatching RestoreResult(
                    success = false,
                    errorMessage = "Invalid backup file format"
                )

            var restoredCount = 0

            backupData.tasks.forEach { backupTask ->
                val taskEntity = backupTask.toTaskEntity()
                taskDao.insert(taskEntity)
                restoredCount++
            }

            RestoreResult(
                success = true,
                tasksRestored = restoredCount,
                tasksSkipped = 0
            )
        }.getOrElse { error ->
            RestoreResult(
                success = false,
                errorMessage = error.message ?: "Recovery failed"
            )
        }
    }

    // ==================== LEGACY BACKUP COMPATIBILITY ====================

    /**
     * Attempts to import from legacy unencrypted backup.
     * Used for backward compatibility during migration.
     */
    private suspend fun tryImportLegacyBackup(): RestoreResult {
        val legacyFile = getLegacyBackupFile()

        if (!legacyFile.exists()) {
            return RestoreResult(
                success = false,
                errorMessage = "No backup file found"
            )
        }

        // Read legacy unencrypted file
        val jsonString = legacyFile.readText()
        val result = processBackupImport(jsonString)

        // Migrate to encrypted backup
        if (result.success) {
            exportSecureBackup()
            deleteLegacyBackup()
        }

        return result.copy(migratedFromLegacy = true)
    }

    /**
     * Attempts recovery from legacy backup.
     */
    private suspend fun tryRecoverLegacyBackup(): RestoreResult {
        val legacyFile = getLegacyBackupFile()

        if (!legacyFile.exists()) {
            return RestoreResult(
                success = false,
                errorMessage = "No backup available for recovery"
            )
        }

        val jsonString = legacyFile.readText()
        val backupData = parseBackupJson(jsonString)
            ?: return@tryRecoverLegacyBackup RestoreResult(
                success = false,
                errorMessage = "Legacy backup file corrupted"
            )

        var restoredCount = 0

        backupData.tasks.forEach { backupTask ->
            val taskEntity = backupTask.toTaskEntity()
            taskDao.insert(taskEntity)
            restoredCount++
        }

        // Migrate to encrypted
        exportSecureBackup()
        deleteLegacyBackup()

        return RestoreResult(
            success = true,
            tasksRestored = restoredCount,
            migratedFromLegacy = true
        )
    }

    /**
     * Migrates legacy backup to encrypted format if exists.
     * Call this on app upgrade.
     */
    suspend fun migrateLegacyBackup(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val legacyFile = getLegacyBackupFile()
            if (!legacyFile.exists()) {
                return@runCatching false
            }

            // Import from legacy
            val jsonString = legacyFile.readText()
            val backupData = parseBackupJson(jsonString)
                ?: return@runCatching false

            // Export to encrypted
            val newJsonString = backupJson.encodeToString(backupData)
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(newJsonString.toByteArray(StandardCharsets.UTF_8))
            }

            // Delete legacy
            deleteLegacyBackup()
            true
        }.getOrDefault(false)
    }

    // ==================== UTILITY FUNCTIONS ====================

    /**
     * Checks if an encrypted backup file exists.
     */
    fun hasBackup(): Boolean {
        return getBackupFile().exists()
    }

    /**
     * Checks if legacy unencrypted backup exists.
     */
    fun hasLegacyBackup(): Boolean {
        return getLegacyBackupFile().exists()
    }

    /**
     * Gets the backup file size in bytes.
     */
    fun getBackupSize(): Long {
        return getBackupFile().length()
    }

    /**
     * Gets formatted backup file size for display.
     */
    fun getFormattedBackupSize(): String {
        val size = getBackupSize()
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    /**
     * Gets the last backup file modification date.
     */
    fun getLastBackupDate(): Date? {
        val file = getBackupFile()
        return if (file.exists()) Date(file.lastModified()) else null
    }

    /**
     * Deletes the encrypted backup file.
     */
    suspend fun deleteBackup(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            getBackupFile().delete()
        }.getOrDefault(false)
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Gets the main encrypted backup file location.
     */
    private fun getBackupFile(): File {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR_NAME)
        backupDir.mkdirs()
        return File(backupDir, BACKUP_FILE_NAME)
    }

    /**
     * Gets the legacy unencrypted backup file location.
     */
    private fun getLegacyBackupFile(): File {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val backupDir = File(downloadsDir, LEGACY_BACKUP_DIR)
        return File(backupDir, LEGACY_BACKUP_FILE)
    }

    /**
     * Reads and decrypts an encrypted file.
     */
    private fun readEncryptedFile(file: File): String? {
        return runCatching {
            // Create EncryptedFile handle for reading
            val fileToRead = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            fileToRead.openFileInput().use { inputStream ->
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    inputStream.copyTo(byteArrayOutputStream)
                    byteArrayOutputStream.toString(StandardCharsets.UTF_8.name())
                }
            }
        }.getOrNull()
    }

    /**
     * Creates a dated encrypted backup for history.
     */
    private fun createDatedEncryptedBackup(jsonString: String) {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR_NAME)
        backupDir.mkdirs()

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val datedFileName = String.format(BACKUP_FILE_NAME_DATED, dateFormat.format(Date()))
        val datedFile = File(backupDir, datedFileName)

        val datedEncryptedFile = EncryptedFile.Builder(
            context,
            datedFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        datedEncryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
        }
    }

    /**
     * Cleans up old dated backups, keeping only the most recent.
     */
    private fun cleanupOldBackups() {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR_NAME)

        val datedBackups = backupDir.listFiles { file ->
            file.name.startsWith("evertask_backup_") && file.name.endsWith(".enc")
        }?.sortedBy { it.lastModified() }?.toMutableList() ?: return

        // Remove oldest files if over limit
        while (datedBackups.size > MAX_BACKUP_FILES) {
            datedBackups.removeFirst().delete()
        }
    }

    /**
     * Deletes the legacy unencrypted backup for security.
     */
    private fun deleteLegacyBackup() {
        val legacyFile = getLegacyBackupFile()
        if (legacyFile.exists()) {
            legacyFile.delete()
        }

        // Also clean up legacy dated backups
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val legacyDir = File(downloadsDir, LEGACY_BACKUP_DIR)

        legacyDir.listFiles { file ->
            file.name.startsWith("evertask_backup_") && file.name.endsWith(".json")
        }?.forEach { it.delete() }
    }

    /**
     * Creates backup data structure from task entities.
     */
    private fun createBackupData(tasks: List<TaskEntity>): BackupData {
        return BackupData(
            version = 2, // Version 2 = encrypted backups
            exportDate = System.currentTimeMillis(),
            appVersion = "1.0.0", // Should match BuildConfig
            isEncrypted = true,
            tasks = tasks.map { BackupTask.fromEntity(it) }
        )
    }

    /**
     * Parses backup JSON string into data structure.
     */
    private fun parseBackupJson(jsonString: String): BackupData? {
        return runCatching {
            backupJson.decodeFromString<BackupData>(jsonString)
        }.getOrNull()
    }

    /**
     * Processes backup JSON and imports tasks.
     */
    private suspend fun processBackupImport(jsonString: String): RestoreResult {
        val backupData = parseBackupJson(jsonString)
            ?: return RestoreResult(
                success = false,
                errorMessage = "Invalid backup file format"
            )

        var restoredCount = 0
        var skippedCount = 0

        backupData.tasks.forEach { backupTask ->
            // Check if task already exists
            val existingTask = taskDao.getById(backupTask.id)
            if (existingTask != null) {
                skippedCount++
                return@forEach
            }

            // Convert and insert
            val taskEntity = backupTask.toTaskEntity()
            taskDao.insert(taskEntity)
            restoredCount++
        }

        return RestoreResult(
            success = true,
            tasksRestored = restoredCount,
            tasksSkipped = skippedCount
        )
    }
}

// ==================== BACKUP DATA CLASSES ====================

/**
 * Root structure of backup file.
 * Version 2 includes isEncrypted flag.
 */
@Serializable
data class BackupData(
    val version: Int,
    val exportDate: Long,
    val appVersion: String,
    val isEncrypted: Boolean = true,
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
