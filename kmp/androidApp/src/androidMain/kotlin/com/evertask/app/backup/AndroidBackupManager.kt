package com.evertask.app.backup

import android.content.Context
import com.evertask.database.EverTaskDatabase
import com.evertask.shared.backup.BackupManager
import com.evertask.shared.data.repository.TaskDataSource
import com.evertask.shared.domain.model.Task
import com.evertask.shared.domain.model.TaskTemplate
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AndroidBackupManager(
    private val context: Context,
    private val taskDataSource: TaskDataSource,
    private val database: EverTaskDatabase
) : BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val backupDir: File?
        get() = context.getExternalFilesDir(null)?.let { File(it, "EverTask") }

    private val backupFile: File?
        get() = backupDir?.let { File(it, "backup.json") }

    override suspend fun createBackup(): Result<String> {
        return runCatching {
            val tasks = taskDataSource.getAllTasks().first()
            val templates = taskDataSource.getTemplates()
            val backupData = BackupData(tasks = tasks, templates = templates)

            val file = backupFile ?: throw IllegalStateException("External files directory not available")
            backupDir?.mkdirs()
            file.writeText(json.encodeToString(backupData))
            file.absolutePath
        }
    }

    override suspend fun restoreFromBackup(): Result<Boolean> {
        return runCatching {
            val file = backupFile ?: return@runCatching false
            if (!file.exists()) return@runCatching false

            val backupData = json.decodeFromString<BackupData>(file.readText())

            database.taskQueries.deleteAllTasks()
            database.taskQueries.deleteAllSubtasks()

            backupData.tasks.forEach { task ->
                taskDataSource.insertTask(task)
            }
            backupData.templates.forEach { template ->
                taskDataSource.insertTemplate(template)
            }
            true
        }
    }

    override suspend fun getLatestBackupPath(): String? {
        val file = backupFile
        return if (file != null && file.exists()) file.absolutePath else null
    }
}

@Serializable
private data class BackupData(
    val tasks: List<Task> = emptyList(),
    val templates: List<TaskTemplate> = emptyList()
)
