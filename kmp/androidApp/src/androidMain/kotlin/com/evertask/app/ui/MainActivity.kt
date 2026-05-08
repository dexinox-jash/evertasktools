package com.evertask.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.evertask.app.EverTaskApplication
import com.evertask.app.deeplink.DeepLinkHandler
import com.evertask.app.ui.screens.MainScreen
import com.evertask.app.ui.theme.EverTaskTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    companion object {
        private const val INTERNAL_ACTION_EXTRA = "evertask_internal_action"
        private const val INTERNAL_TOKEN_EXTRA = "evertask_internal_token"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled silently; notifications will work if granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            EverTaskTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TaskViewModel = koinViewModel()
                    MainScreen(viewModel = viewModel)
                }
            }
        }
        handleIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Could show a rationale dialog here before requesting
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data
        val isInternalNotification = intent.getBooleanExtra(INTERNAL_ACTION_EXTRA, false)
        val token = intent.getStringExtra(INTERNAL_TOKEN_EXTRA)
        val expectedToken = EverTaskApplication.notificationToken
        val isProtectedAction = action in listOf(
            "com.evertask.app.ACTION_COMPLETE",
            "com.evertask.app.ACTION_SKIP",
            "com.evertask.app.ACTION_SNOOZE"
        )
        if (isProtectedAction && (!isInternalNotification || token != expectedToken)) {
            return
        }
        if (isProtectedAction || (data != null && data.scheme == "evertask")) {
            DeepLinkHandler.handleDeepLink(this, intent)
        }
    }
}
