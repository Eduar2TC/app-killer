package com.appcontrol.core.system

import android.os.Build

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val androidSdkInt: Int,
    val isEmulator: Boolean
) {
    companion object {
        fun detect(): DeviceInfo {
            return DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                androidSdkInt = Build.VERSION.SDK_INT,
                isEmulator = checkIsEmulator()
            )
        }

        private fun checkIsEmulator(): Boolean {
            return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator")
        }
    }
}

class DeviceCompatibilityChecker {

    private val deviceInfo = DeviceInfo.detect()

    fun getDeviceInfo(): DeviceInfo = deviceInfo

    fun isDeviceSupported(): Boolean {
        return deviceInfo.androidSdkInt >= Build.VERSION_CODES.N
    }

    fun isUsageStatsSupported(): Boolean {
        return true
    }

    fun isNotificationListenerSupported(): Boolean {
        return deviceInfo.androidSdkInt >= Build.VERSION_CODES.JELLY_BEAN_MR2
    }

    fun isExactAlarmSupported(): Boolean {
        return deviceInfo.androidSdkInt >= Build.VERSION_CODES.S
    }

    fun getDeviceWarnings(): List<String> {
        val warnings = mutableListOf<String>()

        if (deviceInfo.isEmulator) {
            warnings.add("Running on an emulator. Some features may not work correctly.")
        }

        if (!isDeviceSupported()) {
            warnings.add("Device Android version is below Nougat (7.0). Some features may be limited.")
        }

        val knownProblematicManufacturers = listOf("huawei", "xiaomi", "oppo", "vivo", "samsung")
        if (knownProblematicManufacturers.any { deviceInfo.manufacturer.lowercase().contains(it) }) {
            warnings.add("Device manufacturer (${deviceInfo.manufacturer}) may have battery optimization that could affect monitoring.")
        }

        return warnings
    }

    fun getKillingStrategy(): String {
        return when {
            deviceInfo.manufacturer.lowercase().contains("xiaomi") -> "xiaomi_optimized"
            deviceInfo.manufacturer.lowercase().contains("huawei") -> "huawei_optimized"
            deviceInfo.manufacturer.lowercase().contains("samsung") -> "samsung_optimized"
            deviceInfo.manufacturer.lowercase().contains("oppo") || deviceInfo.manufacturer.lowercase().contains("realme") -> "coloros_optimized"
            deviceInfo.manufacturer.lowercase().contains("vivo") -> "funtouch_optimized"
            else -> "generic"
        }
    }
}
