package com.evertask.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.evertask.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Task operations.
 * All queries return Flow for reactive UI updates.
 */
@Dao
interface TaskDao {
    
    // ==================== READ OPERATIONS ====================
    
    /**
     * Returns all non-archived tasks ordered by creation date (newest first).
     * Flow emits new values automatically when data changes.
     */
    @Query("SELECT * FROM tasks WHERE is_archived = 0 ORDER BY created_at DESC")
    fun getAllActive(): Flow<List<TaskEntity>>
    
    /**
     * Returns all tasks (active + completed/archived).
     */
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun getAll(): Flow<List<TaskEntity>>
    
    /**
     * Returns all completed but non-archived tasks.
     * Useful for showing "ready to archive" tasks.
     */
    @Query("SELECT * FROM tasks WHERE is_completed = 1 AND is_archived = 0 ORDER BY completed_at DESC")
    fun getAllCompleted(): Flow<List<TaskEntity>>
    
    /**
     * Returns all archived tasks (soft-deleted).
     * Can be used for restore functionality or cleanup.
     */
    @Query("SELECT * FROM tasks WHERE is_archived = 1 ORDER BY completed_at DESC")
    fun getAllArchived(): Flow<List<TaskEntity>>
    
    /**
    * Returns a single task by its unique ID.
     * Returns null if task not found.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: String): TaskEntity?
    
    /**
     * Returns tasks matching a specific template ID.
     * Useful for analytics or "similar tasks" feature.
     */
    @Query("SELECT * FROM tasks WHERE template_id = :templateId AND is_archived = 0 ORDER BY created_at DESC")
    fun getByTemplateId(templateId: String): Flow<List<TaskEntity>>
    
    /**
     * Searches tasks by title (case-insensitive partial match).
     */
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' AND is_archived = 0 ORDER BY created_at DESC")
    fun searchByTitle(query: String): Flow<List<TaskEntity>>
    
    /**
     * Returns count of active (non-archived) tasks.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE is_archived = 0")
    suspend fun getActiveCount(): Int
    
    /**
     * Returns count of completed tasks.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1 AND is_archived = 0")
    suspend fun getCompletedCount(): Int
    
    // ==================== WRITE OPERATIONS ====================
    
    /**
     * Inserts a new task. Replaces on conflict (should not happen with UUIDs).
     * Returns the row ID of inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long
    
    /**
     * Inserts multiple tasks in a single transaction.
     * More efficient than individual inserts for bulk operations.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>
    
    /**
     * Updates an existing task.
     * Must use complete TaskEntity - partial updates not supported.
     */
    @Update
    suspend fun update(task: TaskEntity): Int
    
    /**
     * Updates task completion status directly via SQL.
     * More efficient than full entity update for simple status changes.
     */
    @Query("UPDATE tasks SET is_completed = :isCompleted, completed_at = :completedAt WHERE id = :taskId")
    suspend fun updateCompletionStatus(
        taskId: String, 
        isCompleted: Boolean, 
        completedAt: Long?
    ): Int
    
    /**
     * Updates only the subtasks JSON for a task.
     * Used when toggling subtask completion without rewriting entire entity.
     */
    @Query("UPDATE tasks SET subtasks_json = :subtasksJson WHERE id = :taskId")
    suspend fun updateSubtasks(taskId: String, subtasksJson: String): Int
    
    /**
     * Marks a task as archived (soft delete).
     */
    @Query("UPDATE tasks SET is_archived = 1 WHERE id = :taskId")
    suspend fun archiveById(taskId: String): Int
    
    /**
     * Archives all completed tasks in one operation.
     * Returns number of tasks archived.
     */
    @Query("UPDATE tasks SET is_archived = 1 WHERE is_completed = 1 AND is_archived = 0")
    suspend fun archiveAllCompleted(): Int
    
    /**
     * Permanently deletes a task by ID.
     * Use with caution - prefer archive for user-facing delete.
     */
    @Delete
    suspend fun delete(task: TaskEntity): Int
    
    /**
     * Permanently deletes a task by ID directly.
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String): Int
    
    /**
     * Permanently deletes all archived tasks.
     * Use for cleanup operations.
     */
    @Query("DELETE FROM tasks WHERE is_archived = 1")
    suspend fun deleteAllArchived(): Int
    
    /**
     * Permanently deletes all tasks - USE WITH EXTREME CAUTION.
     * Primary use case: account deletion or data reset.
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAll(): Int
    
    // ==================== TRANSACTION OPERATIONS ====================
    
    /**
     * Transaction: Archives completed and returns the archived tasks.
     * Ensures atomic operation - either all archived or none.
     */
    @Transaction
    suspend fun archiveCompletedAndGet(): List<TaskEntity> {
        // First get the tasks that will be archived
        val toArchive = getCompletedSnapshot()
        // Then archive them
        archiveAllCompleted()
        return toArchive
    }
    
    /**
     * Gets a one-time snapshot of completed tasks (non-flow).
     * Used within transactions for consistent reads.
     */
    @Query("SELECT * FROM tasks WHERE is_completed = 1 AND is_archived = 0")
    suspend fun getCompletedSnapshot(): List<TaskEntity>
    
    /**
     * Transaction: Inserts task and returns the inserted entity.
     * Useful when you need immediate access to the created task.
     */
    @Transaction
    suspend fun insertAndGet(task: TaskEntity): TaskEntity? {
        insert(task)
        return getById(task.id)
    }
    
    /**
     * Transaction: Updates subtasks and marks complete if all done.
     * Atomic operation ensures consistency.
     */
    @Transaction
    suspend fun updateSubtasksAndCheckCompletion(
        taskId: String, 
        subtasksJson: String,
        allCompleted: Boolean
    ) {
        updateSubtasks(taskId, subtasksJson)
        if (allCompleted) {
            updateCompletionStatus(taskId, true, System.currentTimeMillis())
        }
    }
}
