package dev.ideonq.aetheros

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

data class DeviceSnapshot(
    val model: String,
    val version: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batterySaver: Boolean,
    val freeRamMb: Long,
    val totalRamMb: Long
) {
    val freeRamRatio: Float
        get() = if (totalRamMb > 0) (freeRamMb.toFloat() / totalRamMb).coerceIn(0f, 1f) else 0f
}

data class AppUsage(val packageName: String, val label: String, val minutes: Long)
data class LaunchableApp(val packageName: String, val title: String)

class DeviceRepository(private val context: Context) {
    fun snapshot(): DeviceSnapshot {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        val power = context.getSystemService(PowerManager::class.java)
        return DeviceSnapshot(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            version = "Android ${Build.VERSION.RELEASE} · SDK ${Build.VERSION.SDK_INT}",
            batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            batterySaver = power?.isPowerSaveMode == true,
            freeRamMb = info.availMem / (1024 * 1024),
            totalRamMb = info.totalMem / (1024 * 1024)
        )
    }

    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        return ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    fun usageLast24Hours(): List<AppUsage> {
        if (!hasUsageAccess()) return emptyList()
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val end = System.currentTimeMillis()
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            end - 24L * 60 * 60 * 1000,
            end
        ).orEmpty()
        val packageManager = context.packageManager
        return stats.groupBy { it.packageName }
            .mapNotNull { (pkg, buckets) ->
                val minutes = buckets.sumOf { it.totalTimeInForeground } / 60_000L
                if (minutes <= 0L || pkg == context.packageName) null else {
                    val label = runCatching {
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(pkg, 0)
                        ).toString()
                    }.getOrDefault(pkg.substringAfterLast('.'))
                    AppUsage(pkg, label, minutes)
                }
            }
            .sortedByDescending { it.minutes }
            .take(25)
    }

    @Suppress("DEPRECATION")
    fun launchableApps(): List<LaunchableApp> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(query, 0)
            .map { LaunchableApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.title.lowercase() }
    }

    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openSettings(action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStart(intent)
    }

    fun openAppDetails(packageName: String) {
        safeStart(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openAetherWallpaper() {
        safeStart(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, AetherWallpaperService::class.java)
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openWallpaper() = safeStart(
        Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

    fun openGoodLock() {
        val launch = context.packageManager.getLaunchIntentForPackage("com.samsung.android.goodlock")
        if (launch != null) {
            safeStart(launch)
        } else {
            safeStart(
                Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://galaxystore.samsung.com/detail/com.samsung.android.goodlock"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun safeStart(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                // No settings activity available; never crash the launcher.
            }
        }
    }
}
