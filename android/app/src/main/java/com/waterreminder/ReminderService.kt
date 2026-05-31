package com.waterreminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import org.json.JSONArray
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
            val prefs = context.getSharedPreferences("water_reminder", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("enabled", true)
                .putInt("interval", intervalMinutes)
                .putString("timeRanges", timeRangesJson)
                .apply()

            scheduleAlarm(context, intervalMinutes)

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
            cancelAlarm(context)

            val intent = Intent(context, ReminderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun scheduleAlarm(context: Context, intervalMinutes: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + intervalMinutes * 60 * 1000L

            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                )
                Log.e(TAG, "setAlarmClock at ${Calendar.getInstance().apply { timeInMillis = triggerTime }.time}")
            } catch (e: Exception) {
                Log.e(TAG, "scheduleAlarm error", e)
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        fun handleAlarm(context: Context) {
            val prefs = context.getSharedPreferences("water_reminder", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", false)
            if (!enabled) {
                Log.e(TAG, "disabled, skip")
                return
            }

            val interval = prefs.getInt("interval", 60)
            val timeRangesJson = prefs.getString("timeRanges", "[]") ?: "[]"

            scheduleAlarm(context, interval)

            if (isInTimeRange(timeRangesJson)) {
                Log.e(TAG, "In time range, trigger reminder")
                try {
                    wakeUpScreen(context)
                    TtsSingleton.speak(context, "该喝水了！请喝一杯水保持健康。")
                    launchReminderActivity(context)
                    showReminderNotification(context)
                } catch (e: Exception) {
                    Log.e(TAG, "handleAlarm error", e)
                }
            } else {
                Log.e(TAG, "Not in time range, skip")
            }
        }

        private fun isInTimeRange(timeRangesJson: String): Boolean {
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

        private fun wakeUpScreen(context: Context) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                    "WaterReminder:ScreenWakeLock"
                )
                wakeLock.acquire(10000)
            } catch (e: Exception) {
                Log.e(TAG, "wakeUpScreen error", e)
            }
        }

        private fun launchReminderActivity(context: Context) {
            try {
                val intent = Intent(context, ReminderActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.e(TAG, "ReminderActivity launched")
            } catch (e: Exception) {
                Log.e(TAG, "launchReminderActivity error", e)
            }
        }

        private fun showReminderNotification(context: Context) {
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
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.createNotificationChannel(channel)
                }

                val notification = Notification.Builder(context, CHANNEL_ID_REMIND)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("\uD83D\uDCA7 喝水闹钟")
                    .setContentText("该喝水了！请喝一杯水保持健康。")
                    .setAutoCancel(true)
                    .build()

                val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(id, notification)
                Log.e(TAG, "Notification shown")
            } catch (e: Exception) {
                Log.e(TAG, "showReminderNotification error", e)
            }
        }
    }

    private var intervalMinutes = 60
    private var timeRangesJson = "[]"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                intervalMinutes = intent.getIntExtra("interval", 60)
                timeRangesJson = intent.getStringExtra("timeRanges") ?: "[]"
                Log.e(TAG, "START interval=${intervalMinutes}min ranges=$timeRangesJson")
                startForeground(NOTIFICATION_ID, createServiceNotification())
            }
            ACTION_STOP -> {
                Log.e(TAG, "STOP")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun createServiceNotification(): Notification {
        createServiceNotificationChannel()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\uD83D\uDCA7 喝水闹钟")
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
}
