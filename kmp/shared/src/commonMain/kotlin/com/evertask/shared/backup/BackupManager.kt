package com.evertask.shared.backup

interface BackupManager {
    suspend fun createBackup(): Result<String>
    suspend fun restoreFromBackup(): Result<Boolean>
    suspend fun getLatestBackupPath(): String?
}
