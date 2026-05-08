package com.evertasktools

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evertasktools.ui.MainScreen
import com.evertasktools.ui.TaskViewModel
import com.evertasktools.ui.theme.EverTaskToolsTheme
import com.evertask.data.preferences.DataStoreManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.filter { !it.value }.keys.toList()
        if (denied.isNotEmpty()) {
            permissionDeniedCallback?.invoke(denied)
        }
    }

    private var permissionDeniedCallback: ((List<String>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link intent
        handleDeepLink(intent)

        setContent {
            val dataStoreManager = remember { DataStoreManager.getInstance(this) }
            val themeMode by dataStoreManager.themeModeFlow().collectAsState(
                initial = DataStoreManager.ThemeMode.SYSTEM
            )

            val darkTheme = when (themeMode) {
                DataStoreManager.ThemeMode.LIGHT -> false
                DataStoreManager.ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            EverTaskToolsTheme(darkTheme = darkTheme) {
                val snackbarHostState = remember { SnackbarHostState() }
                val viewModel: TaskViewModel = viewModel { TaskViewModel(this@MainActivity) }
                val uiState by viewModel.uiState.collectAsState()

                // Collect events
                LaunchedEffect(viewModel.events) {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is TaskViewModel.TaskEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                            is TaskViewModel.TaskEvent.VoiceTimeout -> {
                                snackbarHostState.showSnackbar("Voice input timed out. Try typing instead.")
                            }
                            else -> { /* Handle other events */ }
                        }
                    }
                }

                // Permission rationale
                LaunchedEffect(Unit) {
                    permissionDeniedCallback = { denied ->
                        lifecycleScope.launch {
                            val message = when {
                                denied.contains(Manifest.permission.RECORD_AUDIO) ->
                                    "Microphone permission is needed for voice input."
                                denied.contains(Manifest.permission.POST_NOTIFICATIONS) ->
                                    "Notification permission is needed for task reminders."
                                else -> "Some permissions were denied."
                            }
                            val result = snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = "Open Settings"
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier.padding(innerPadding),
                        onRequestVoicePermission = { checkPermissions() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            when (uri.scheme) {
                "evertask" -> {
                    when (uri.host) {
                        "voice" -> {
                            val query = uri.getQueryParameter("q")
                            query?.let {
                                // Would trigger voice input or create task
                            }
                        }
                        "task" -> {
                            val taskId = uri.getQueryParameter("id")
                            taskId?.let {
                                // Would navigate to task
                            }
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
