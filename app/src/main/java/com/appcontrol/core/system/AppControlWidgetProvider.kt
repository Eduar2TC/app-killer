package com.appcontrol.core.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.appcontrol.R
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.feature.dashboard.MainActivity
import com.appcontrol.receiver.WidgetActionReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppControlWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.app_widget_layout)

        val protectionEnabled = runBlocking {
            try {
                PreferencesManager(context).nightProtectionEnabled.first()
            } catch (e: Exception) {
                false
            }
        }
        views.setTextViewText(
            R.id.widgetStatus,
            if (protectionEnabled) {
                context.getString(R.string.widget_protection_active)
            } else {
                context.getString(R.string.widget_protection_inactive)
            }
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetTitle, openPending)

        val statusIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = ACTION_APP_CONTROL_OPEN
        }
        val statusPending = PendingIntent.getBroadcast(
            context,
            1,
            statusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetStatus, statusPending)

        val processIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = ACTION_APP_CONTROL_TOGGLE
        }
        val processPending = PendingIntent.getBroadcast(
            context,
            2,
            processIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetProcessButton, processPending)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {
        const val ACTION_APP_CONTROL_TOGGLE = "com.appcontrol.action.APP_CONTROL_TOGGLE"
        const val ACTION_APP_CONTROL_OPEN = "com.appcontrol.action.APP_CONTROL_OPEN"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AppControlWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val provider = AppControlWidgetProvider()
            widgetIds.forEach { widgetId ->
                provider.updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}