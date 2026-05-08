package com.evertask.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.evertask.data.dao.TaskDao
import com.evertask.data.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Room Database for Ever Task Tools.
 * 
 * Database architecture:
 * - Single table: tasks
 * - JSON columns for complex data (subtasks)
 * - Soft delete via is_archived flag
 * - Automatic backup on version upgrade
 * 
 * Migration strategy:
 * - All migrations are additive (new columns have defaults)
 * - No data loss migrations
 * - Fallback to destructive only in development
 */
@Database(
    entities = [TaskEntity::class],
    version = TaskDatabase.DATABASE_VERSION,
    exportSchema = true
)
abstract class TaskDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    
    companion object {
        const val DATABASE_NAME = "evertask_database.db"
        const val DATABASE_VERSION = 2
        
        @Volatile
        private var INSTANCE: TaskDatabase? = null
        
        /**
         * Gets singleton database instance with thread-safe initialization.
         * Creates database with all migrations applied.
         */
        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        /**
         * Builds database with migrations and callbacks.
         * Production: Uses migrations. Development: Can use fallback.
         */
        private fun buildDatabase(context: Context): TaskDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TaskDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(
                MIGRATION_1_2
            )
            .addCallback(DatabaseCallback())
            // Uncomment for development only: .fallbackToDestructiveMigration()
            .build()
        }
        
        /**
         * Migration from version 1 to 2:
         * - Added is_archived column for soft delete
         * - Added template_id column for template tracking
         * - Added icon column for visual identification
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add is_archived column with default false
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0"
                )
                // Add template_id column (nullable)
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN template_id TEXT"
                )
                // Add icon column with default value
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN icon TEXT NOT NULL DEFAULT 'checkmark.circle'"
                )
            }
        }
        
        /**
         * Destroys current instance. Use for testing or recovery scenarios.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
    
    /**
     * Database callback for lifecycle events.
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Database created - could seed with sample data here if needed
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Database opened - good place for integrity checks
        }
    }
}

/**
 * Type converters for Room database.
 * Handles complex types that Room cannot persist natively.
 */
class TaskTypeConverters {
    // Currently using JSON serialization in entity, 
    // but these converters can be used for additional types
    
    /**
     * Converts comma-separated string to list.
     * Useful for simple string lists.
     */
    fun stringToList(value: String?): List<String> {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    
    /**
     * Converts list to comma-separated string.
     */
    fun listToString(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}

/**
 * Database recovery utilities for corruption scenarios.
 */
object DatabaseRecovery {
    
    /**
     * Attempts to recover from database corruption by:
     * 1. Closing existing connection
     * 2. Backing up corrupted file (if possible)
     * 3. Deleting corrupted database
     * 4. Rebuilding from scratch
     * 
     * Returns true if recovery succeeded.
     */
    suspend fun recoverFromCorruption(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            // Close existing instance
            TaskDatabase.destroyInstance()
            
            val dbFile = context.getDatabasePath(TaskDatabase.DATABASE_NAME)
            val backupFile = context.getDatabasePath("${TaskDatabase.DATABASE_NAME}.corrupted")
            
            // Try to backup corrupted file for analysis
            if (dbFile.exists()) {
                dbFile.copyTo(backupFile, overwrite = true)
                dbFile.delete()
            }
            
            // Delete associated journal/wal files
            context.getDatabasePath("${TaskDatabase.DATABASE_NAME}-journal").delete()
            context.getDatabasePath("${TaskDatabase.DATABASE_NAME}-shm").delete()
            context.getDatabasePath("${TaskDatabase.DATABASE_NAME}-wal").delete()
            
            // Rebuild will happen on next getInstance() call
            true
        }.getOrDefault(false)
    }
    
    /**
     * Checks if database file exists and appears valid.
     * Basic check - does not verify internal integrity.
     */
    fun isDatabaseValid(context: Context): Boolean {
        val dbFile = context.getDatabasePath(TaskDatabase.DATABASE_NAME)
        return dbFile.exists() && dbFile.length() > 0
    }
    
    /**
     * Gets database file size for monitoring.
     */
    fun getDatabaseSize(context: Context): Long {
        return context.getDatabasePath(TaskDatabase.DATABASE_NAME).length()
    }
}
