package com.evertask.app.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.evertask.app.ui.MainActivity
import com.evertask.shared.data.repository.TaskRepository
import org.koin.java.KoinJavaComponent.getKoin

object WidgetActions {
    val PARAM_TASK_ID = ActionParameters.Key<String>("task_id")
    val PARAM_SUBTASK_ID = ActionParameters.Key<String>("subtask_id")
}

class LaunchAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class ToggleSubtaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[WidgetActions.PARAM_TASK_ID] ?: return
        val subtaskId = parameters[WidgetActions.PARAM_SUBTASK_ID] ?: return
        val repository = getKoin().get<TaskRepository>()
        try {
            repository.toggleSubtask(taskId, subtaskId)
        } catch (e: Exception) {
            Log.e("ToggleSubtaskAction", "Failed to toggle subtask", e)
        } finally {
            updateAllWidgets(context)
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAllWidgets(context)
    }
}

private suspend fun updateAllWidgets(context: Context) {
    GlanceAppWidgetManager(context).getGlanceIds(EverTaskWidget::class.java).forEach { id ->
        updateAppWidgetState(context, id) { }
        EverTaskWidget().update(context, id)
    }
}
