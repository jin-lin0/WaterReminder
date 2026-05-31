package com.waterreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class NotificationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "NotificationModule"
        private const val CHANNEL_ID = "water_reminder_channel"
        private const val CHANNEL_NAME = "兔兔喝水啦"
        private var notificationId = 1000
    }

    override fun getName(): String = "NotificationModule"

    @ReactMethod
    fun scheduleReminder(intervalMinutes: Int, timeRangesJson: String, promise: Promise) {
        try {
            Log.e(TAG, "scheduleReminder: ${intervalMinutes}min, ranges=$timeRangesJson")

            val prefs = reactApplicationContext.getSharedPreferences("water_reminder", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("enabled", true)
                .putInt("interval", intervalMinutes)
                .putString("timeRanges", timeRangesJson)
                .apply()

            requestIgnoreBatteryOptimizations()
            ReminderService.start(reactApplicationContext, intervalMinutes, timeRangesJson)
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "scheduleReminder error", e)
            promise.reject("ERROR", e.message)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            val pm = reactApplicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(reactApplicationContext.packageName)) {
                Log.e(TAG, "Requesting battery optimization exemption")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${reactApplicationContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                reactApplicationContext.startActivity(intent)
            } else {
                Log.e(TAG, "Already exempt from battery optimization")
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestIgnoreBatteryOptimizations error", e)
        }
    }

    @ReactMethod
    fun cancelReminder(promise: Promise) {
        try {
            Log.e(TAG, "cancelReminder")

            val prefs = reactApplicationContext.getSharedPreferences("water_reminder", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("enabled", false).apply()

            ReminderService.stop(reactApplicationContext)
            TtsSingleton.stop()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "cancelReminder error", e)
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun testReminder(promise: Promise) {
        try {
            createNotificationChannel()
            wakeUpScreen()
            TtsSingleton.speak(reactApplicationContext, "该喝水了！请喝一杯水保持健康。")
            showNotificationInternal()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "testReminder error", e)
            promise.reject("ERROR", e.message)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "兔兔喝水啦通知"
                enableVibration(true)
                setSound(null, null)
                setBypassDnd(true)
            }
            val manager =
                reactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun wakeUpScreen() {
        try {
            val pm =
                reactApplicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "WaterReminder:WakeLock"
            )
            wakeLock.acquire(5000)
        } catch (e: Exception) {
            Log.e(TAG, "wakeUpScreen error", e)
        }
    }

    private fun showNotificationInternal() {
        val notificationManager =
            reactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val currentId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        val notification = NotificationCompat.Builder(reactApplicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\uD83D\uDCA7 兔兔喝水啦")
            .setContentText("该喝水了！请喝一杯水保持健康。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(currentId, notification)
    }

    @ReactMethod
    fun showNotification(title: String, message: String) {
        createNotificationChannel()
        wakeUpScreen()

        val notificationManager =
            reactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val currentId = notificationId++

        val notification = NotificationCompat.Builder(reactApplicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\uD83D\uDCA7 兔兔喝水啦")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(currentId, notification)
    }
}
