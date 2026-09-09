package com.appcontrol.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.appcontrol.core.common.Constants

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val monitoringChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_MONITORING,
            Constants.NOTIFICATION_CHANNEL_MONITORING_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for app monitoring events"
        }

        val generalChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_GENERAL,
            Constants.NOTIFICATION_CHANNEL_GENERAL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "General app notifications"
        }

        notificationManager.createNotificationChannel(monitoringChannel)
        notificationManager.createNotificationChannel(generalChannel)
    }

    fun showReappearanceNotification(
        packageName: String,
        appName: String,
        notificationId: Int = Constants.NOTIFICATION_ID_REAPPEARANCE_BASE + packageName.hashCode()
    ) {
        val reviewIntent = createReviewIntent(packageName)
        val processIntent = createProcessIntent(packageName)

        val reviewPendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_REVIEW + packageName.hashCode(),
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val processPendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_PROCESS + packageName.hashCode(),
            processIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_MONITORING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("App Reappeared")
            .setContentText("$appName ($packageName) has reappeared")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_info_details, "Open info", reviewPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Stop", processPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showSummaryNotification(reappearedApps: List<Pair<String, String>>) {
        if (reappearedApps.isEmpty()) return

        val summaryText = "${reappearedApps.size} app(s) reappeared"
        val contentText = reappearedApps.joinToString(", ") { it.first }

        val summaryIntent = createSummaryIntent()
        val summaryPendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_SUMMARY,
            summaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_MONITORING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("App Control Summary")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.InboxStyle().also { style ->
                reappearedApps.forEach { (appName, packageName) ->
                    style.addLine("$appName ($packageName)")
                }
                style.setSummaryText(summaryText)
            })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(summaryPendingIntent)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_ID_SUMMARY, notification)
    }

    fun revokeNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun revokeAllNotifications() {
        notificationManager.cancelAll()
    }

    fun showWidgetStopResult(stopped: Int, total: Int, failed: Int) {
        val text = when {
            total == 0 -> "No apps selected to stop."
            failed > 0 -> "Stopped $stopped of $total apps. $failed failed."
            stopped > 0 -> "Stopped $stopped of $total apps."
            else -> "Nothing to stop."
        }
        showGeneralNotification("Stop Apps", text)
    }

    fun showWidgetStopError(message: String) {
        showGeneralNotification("Stop Apps failed", "$message (tap for details)")
    }

    fun showGeneralNotification(title: String, text: String) {
        val contentIntent = createSummaryIntent()
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_SUMMARY,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(Constants.NOTIFICATION_ID_WIDGET_STOP, notification)
        } catch (e: SecurityException) {
            // notifications permission not granted; result stays silent
        }
    }

    private fun createReviewIntent(packageName: String): Intent {
        return Intent(context, com.appcontrol.receiver.NotificationReceiver::class.java).apply {
            action = com.appcontrol.receiver.NotificationReceiver.ACTION_REVIEW
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
        }
    }

    private fun createProcessIntent(packageName: String): Intent {
        return Intent(context, com.appcontrol.receiver.NotificationReceiver::class.java).apply {
            action = com.appcontrol.receiver.NotificationReceiver.ACTION_PROCESS
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
        }
    }

    private fun createSummaryIntent(): Intent {
        return context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent()
    }
}
