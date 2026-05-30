package com.waterreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class ReminderService : Service() {

    companion object {
        private const val TAG = "ReminderService"
        private const val CHANNEL_ID = "water_reminder_service"
        private const val CHANNEL_NAME = "喝水闹钟服务"
        private const val CHANNEL_ID_REMIND = "water_reminder_channel"
        private const val NOTIFICATION_ID = 8888
        private const val ACTION_START = "com.waterreminder.START_SERVICE"
        private const val ACTION_STOP = "com.waterreminder.STOP_SERVICE"

        fun start(context: Context, intervalMinutes: Int, timeRangesJson: String) {
            val intent = Intent(context, ReminderService::class.java).apply {
                action = ACTION_START
                putExtra("interval", intervalMinutes)
                putExtra("timeRanges", timeRangesJson)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReminderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var intervalMinutes = 60
    private var timeRangesJson = "[]"
    private var running = false

    private val remindRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            Log.e(TAG, "remindRunnable triggered, interval=${intervalMinutes}min")
            try {
                if (isInTimeRange()) {
                    Log.e(TAG, "In time range, showing reminder")
                    wakeUpScreen()
                    TtsSingleton.speak(this@ReminderService, "该喝水了！请喝一杯水保持健康。")
                    launchReminderActivity()
                    showReminderNotification()
                } else {
                    Log.e(TAG, "Not in time range, skipping")
                }
            } catch (e: Exception) {
                Log.e(TAG, "remindRunnable error", e)
            }
            handler.postDelayed(this, intervalMinutes * 60 * 1000L)
            Log.e(TAG, "Next check in ${intervalMinutes}min")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                intervalMinutes = intent.getIntExtra("interval", 60)
                timeRangesJson = intent.getStringExtra("timeRanges") ?: "[]"
                Log.e(TAG, "START interval=${intervalMinutes}min ranges=$timeRangesJson")
                startForeground(NOTIFICATION_ID, createServiceNotification())
                startReminder()
            }
            ACTION_STOP -> {
                Log.e(TAG, "STOP")
                stopReminder()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startReminder() {
        running = true
        handler.removeCallbacks(remindRunnable)
        handler.postDelayed(remindRunnable, intervalMinutes * 60 * 1000L)
        Log.e(TAG, "Reminder started, first in ${intervalMinutes}min")
    }

    private fun stopReminder() {
        running = false
        handler.removeCallbacks(remindRunnable)
    }

    private fun isInTimeRange(): Boolean {
        try {
            val ranges = JSONArray(timeRangesJson)
            if (ranges.length() == 0) return true

            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            for (i in 0 until ranges.length()) {
                val range = ranges.getJSONObject(i)
                val startMinutes = range.getInt("startHour") * 60 + range.getInt("startMinute")
                val endMinutes = range.getInt("endHour") * 60 + range.getInt("endMinute")

                if (startMinutes <= endMinutes) {
                    if (currentMinutes in startMinutes..endMinutes) {
                        return true
                    }
                } else {
                    if (currentMinutes >= startMinutes || currentMinutes <= endMinutes) {
                        return true
                    }
                }
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "isInTimeRange error", e)
            return true
        }
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        stopReminder()
        super.onDestroy()
    }

    private fun wakeUpScreen() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "WaterReminder:ServiceWakeLock"
            )
            wakeLock.acquire(10000)
        } catch (e: Exception) {
            Log.e(TAG, "wakeUpScreen error", e)
        }
    }

    private fun launchReminderActivity() {
        try {
            val intent = Intent(this, ReminderActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.e(TAG, "ReminderActivity launched")
        } catch (e: Exception) {
            Log.e(TAG, "launchReminderActivity error", e)
        }
    }

    private fun createServiceNotification(): Notification {
        createServiceNotificationChannel()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💧 喝水闹钟")
            .setContentText("每${intervalMinutes}分钟提醒喝水")
            .setOngoing(true)
            .build()
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "喝水闹钟服务"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showReminderNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID_REMIND, "喝水闹钟", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "喝水提醒通知"
                    enableVibration(true)
                    setSound(null, null)
                    setBypassDnd(true)
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val notification = Notification.Builder(this, CHANNEL_ID_REMIND)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("💧 喝水闹钟")
                .setContentText("该喝水了！请喝一杯水保持健康。")
                .setAutoCancel(true)
                .build()

            val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(id, notification)
            Log.e(TAG, "Reminder notification shown")
        } catch (e: Exception) {
            Log.e(TAG, "showReminderNotification error", e)
        }
    }
}
