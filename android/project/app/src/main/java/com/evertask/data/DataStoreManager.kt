package com.evertask.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * DataStore manager for simple app preferences.
 * 
 * Stored preferences:
 * - last_used_template: ID of most recently used template
 * - theme_mode: 0=system, 1=light, 2=dark
 * - onboarding_completed: Whether user has seen onboarding
 * - backup_enabled: Whether auto-backup is enabled
 * - last_backup_timestamp: When last backup occurred
 * 
 * All operations are suspend functions with proper error handling.
 */
class DataStoreManager private constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val DATASTORE_NAME = "evertask_preferences"
        
        // Preference keys
        private val KEY_LAST_USED_TEMPLATE = stringPreferencesKey("last_used_template")
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
        private val KEY_LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        private val KEY_FIRST_LAUNCH_DATE = longPreferencesKey("first_launch_date")
        private val KEY_TASKS_CREATED_COUNT = intPreferencesKey("tasks_created_count")
        
        @Volatile
        private var INSTANCE: DataStoreManager? = null
        
        /**
         * Extension property for Context to get DataStore instance.
         */
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = DATASTORE_NAME
        )
        
        /**
         * Gets singleton DataStoreManager instance.
         */
        fun getInstance(context: Context): DataStoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataStoreManager(context.dataStore).also { INSTANCE = it }
            }
        }
    }
    
    // ==================== LAST USED TEMPLATE ====================
    
    /**
     * Saves the ID of the most recently used template.
     * Used for "quick repeat" functionality.
     */
    suspend fun setLastUsedTemplate(templateId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.edit { preferences ->
                preferences[KEY_LAST_USED_TEMPLATE] = templateId
            }
            Unit
        }
    }
    
    /**
     * Gets the last used template ID, or null if none stored.
     */
    suspend fun getLastUsedTemplate(): String? = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[KEY_LAST_USED_TEMPLATE]
        }.getOrNull()
    }
    
    /**
     * Flow of last used template for reactive UI.
     */
    fun lastUsedTemplateFlow(): Flow<String?> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[KEY_LAST_USED_TEMPLATE] }
            .flowOn(Dispatchers.IO)
    }
    
    // ==================== THEME MODE ====================
    
    /**
     * Theme mode values.
     */
    enum class ThemeMode(val value: Int) {
        SYSTEM(0),
        LIGHT(1),
        DARK(2);
        
        companion object {
            fun fromValue(value: Int): ThemeMode = values().find { it.value == value } ?: SYSTEM
        }
    }
    
    /**
     * Saves the selected theme mode.
     */
    suspend fun setThemeMode(mode: ThemeMode): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.edit { preferences ->
                preferences[KEY_THEME_MODE] = mode.value
            }
            Unit
        }
    }
    
    /**
     * Gets current theme mode, defaults to SYSTEM.
     */
    suspend fun getThemeMode(): ThemeMode = withContext(Dispatchers.IO) {
        runCatching {
            val value = dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[KEY_THEME_MODE]
            ThemeMode.fromValue(value ?: ThemeMode.SYSTEM.value)
        }.getOrDefault(ThemeMode.SYSTEM)
    }
    
    /**
     * Flow of theme mode for reactive UI.
     */
    fun themeModeFlow(): Flow<ThemeMode> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                val value = preferences[KEY_THEME_MODE]
                ThemeMode.fromValue(value ?: ThemeMode.SYSTEM.value)
            }
            .flowOn(Dispatchers.IO)
    }
    
    // ==================== ONBOARDING ====================
    
    /**
     * Marks onboarding as completed.
     */
    suspend fun setOnboardingCompleted(completed: Boolean = true): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[KEY_ONBOARDING_COMPLETED] = completed
                }
                Unit
            }
        }
    
    /**
     * Checks if onboarding has been completed.
     */
    suspend fun isOnboardingCompleted(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[KEY_ONBOARDING_COMPLETED] ?: false
        }.getOrDefault(false)
    }
    
    /**
     * Flow of onboarding status.
     */
    fun onboardingCompletedFlow(): Flow<Boolean> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[KEY_ONBOARDING_COMPLETED] ?: false }
            .flowOn(Dispatchers.IO)
    }
    
    // ==================== BACKUP SETTINGS ====================
    
    /**
     * Enables/disables automatic backup.
     */
    suspend fun setBackupEnabled(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.edit { preferences ->
                preferences[KEY_BACKUP_ENABLED] = enabled
            }
            Unit
        }
    }
    
    /**
     * Checks if automatic backup is enabled.
     */
    suspend fun isBackupEnabled(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[KEY_BACKUP_ENABLED] ?: true // Default to enabled
        }.getOrDefault(true)
    }
    
    /**
     * Updates the timestamp of last successful backup.
     */
    suspend fun setLastBackupTimestamp(timestamp: Long = System.currentTimeMillis()): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[KEY_LAST_BACKUP_TIMESTAMP] = timestamp
                }
                Unit
            }
        }
    
    /**
     * Gets timestamp of last backup, or null if never backed up.
     */
    suspend fun getLastBackupTimestamp(): Long? = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[KEY_LAST_BACKUP_TIMESTAMP]
        }.getOrNull()
    }
    
    /**
     * Checks if backup is overdue (more than 24 hours since last backup).
     */
    suspend fun isBackupOverdue(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val lastBackup = getLastBackupTimestamp()
            if (lastBackup == null) return@runCatching true
            
            val oneDayMillis = 24 * 60 * 60 * 1000L
            (System.currentTimeMillis() - lastBackup) > oneDayMillis
        }.getOrDefault(true)
    }
    
    // ==================== APP STATISTICS ====================
    
    /**
     * Records first launch date for app anniversary features.
     */
    suspend fun recordFirstLaunchIfNeeded(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.edit { preferences ->
                if (preferences[KEY_FIRST_LAUNCH_DATE] == null) {
                    preferences[KEY_FIRST_LAUNCH_DATE] = System.currentTimeMillis()
                }
            }
            Unit
        }
    }
    
    /**
     * Gets first launch date, or null if not recorded.
     */
    suspend fun getFirstLaunchDate(): Long? = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[KEY_FIRST_LAUNCH_DATE]
        }.getOrNull()
    }
    
    /**
     * Increments the task creation counter.
     */
    suspend fun incrementTasksCreated(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.edit { preferences ->
                val current = preferences[KEY_TASKS_CREATED_COUNT] ?: 0
                preferences[KEY_TASKS_CREATED_COUNT] = current + 1
            }
            Unit
        }
    }
    
    /**
     * Gets total number of tasks created (tracked in preferences).
     */
    suspend fun getTasksCreatedCount(): Int = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()[KEY_TASKS_CREATED_COUNT] ?: 0
        }.getOrDefault(0)
    }
    
    // ==================== UTILITY ====================
    
    /**
     * Clears all preferences. Use with caution.
     */
    suspend fun clearAll(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.edit { it.clear() }
            Unit
        }
    }
    
    /**
     * Gets all preferences as map for debugging/export.
     */
    suspend fun getAllPreferences(): Map<String, Any?> = withContext(Dispatchers.IO) {
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()
                .asMap()
                .mapKeys { it.key.name }
        }.getOrDefault(emptyMap())
    }
}
