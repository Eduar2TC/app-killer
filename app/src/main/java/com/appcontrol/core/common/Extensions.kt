package com.appcontrol.core.common

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Context.getAppName(packageName: String): String {
    return try {
        val pm = packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }
}

fun Context.isNotificationPermissionGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.areNotificationsEnabled()
    } else {
        true
    }
}

fun Context.vibrate(durationMs: Long = 200) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
}

fun Long.formatTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.formatTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> formatTimestamp()
    }
}

fun Long.isWithinNightSchedule(
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int
): Boolean {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@isWithinNightSchedule }
    val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val startMinutes = startHour * 60 + startMinute
    val endMinutes = endHour * 60 + endMinute

    return if (startMinutes <= endMinutes) {
        currentMinutes in startMinutes..endMinutes
    } else {
        currentMinutes >= startMinutes || currentMinutes <= endMinutes
    }
}

fun Int.padWithZero(): String = String.format(Locale.US, "%02d", this)

fun String.isValidPackageName(): Boolean {
    return this.contains(".") && this.split(".").size >= 2
}
