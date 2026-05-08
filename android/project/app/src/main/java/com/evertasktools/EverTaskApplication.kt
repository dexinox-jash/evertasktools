package com.evertasktools

import android.app.Application
import android.util.Log
import com.evertask.data.database.DatabaseRecovery
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EverTaskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Eagerly initialize TaskRepository on a background thread
        GlobalScope.launch {
            com.evertask.data.repository.TaskRepository.getInstance(this@EverTaskApplication)
            val recovered = DatabaseRecovery.recoverFromCorruption(this@EverTaskApplication)
            if (recovered) {
                Log.w("EverTaskApplication", "Database corruption detected and recovered from on startup")
            }
        }
    }
}
