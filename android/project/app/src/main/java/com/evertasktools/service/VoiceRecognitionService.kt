package com.evertasktools.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class VoiceRecognitionService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
