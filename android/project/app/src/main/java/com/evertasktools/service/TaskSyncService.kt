package com.evertasktools.service

import android.app.job.JobParameters
import android.app.job.JobService

class TaskSyncService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }
}
