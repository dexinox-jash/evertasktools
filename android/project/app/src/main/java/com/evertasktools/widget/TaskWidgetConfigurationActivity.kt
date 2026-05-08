package com.evertasktools.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.evertasktools.ui.theme.EverTaskToolsTheme

class TaskWidgetConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EverTaskToolsTheme {
                Text("Widget Configuration")
            }
        }
    }
}
