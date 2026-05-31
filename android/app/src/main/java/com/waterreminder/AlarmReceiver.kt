package com.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e(TAG, "onReceive triggered!")
        context ?: return

        val pendingResult = goAsync()
        try {
            ReminderService.handleAlarm(context)
        } catch (e: Exception) {
            Log.e(TAG, "onReceive error", e)
        } finally {
            pendingResult.finish()
        }
    }
}
