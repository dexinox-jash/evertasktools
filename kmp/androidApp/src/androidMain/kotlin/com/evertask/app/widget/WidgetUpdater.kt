package com.evertask.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

interface WidgetUpdater {
    suspend fun updateAllWidgets()
}

class GlanceWidgetUpdater(private val context: Context) : WidgetUpdater {
    override suspend fun updateAllWidgets() {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(EverTaskWidget::class.java)
        glanceIds.forEach { id ->
            updateAppWidgetState(context, id) { }
            EverTaskWidget().update(context, id)
        }
    }
}
