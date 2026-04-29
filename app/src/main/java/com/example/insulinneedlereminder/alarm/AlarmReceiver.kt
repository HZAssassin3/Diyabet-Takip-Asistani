package com.example.insulinneedlereminder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.insulinneedlereminder.MainActivity
import com.example.insulinneedlereminder.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_DONE -> {
                val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
                NotificationManagerCompat.from(context).cancel(requestCode)
                if (requestCode != -1) {
                    AlarmScheduler.cancelFollowUp(context, requestCode)
                    AlarmScheduler.cancelSnooze(context, requestCode)
                }
                return
            }
            ACTION_SNOOZE -> {
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "İnsülin"
                val units = intent.getIntExtra(EXTRA_UNITS, 0)
                val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
                NotificationManagerCompat.from(context).cancel(requestCode)
                if (requestCode != -1) {
                    AlarmScheduler.cancelFollowUp(context, requestCode)
                    AlarmScheduler.scheduleSnooze(
                        context = context,
                        label = label,
                        units = units,
                        requestCode = requestCode
                    )
                }
                return
            }
        }

        val label = intent.getStringExtra(EXTRA_LABEL) ?: "İnsülin"
        val units = intent.getIntExtra(EXTRA_UNITS, 0)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
        val isFollowUp = intent.getBooleanExtra(EXTRA_IS_FOLLOW_UP, false)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 0)

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (units > 0)
            "$label iğnenizi ($units ünite) yapmayı unutmayın!"
        else
            "$label iğnenizi yapmayı unutmayın!"

        val markDoneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_REQUEST_CODE, requestCode)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode + 20000,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_UNITS, units)
            putExtra(EXTRA_REQUEST_CODE, requestCode)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode + 30000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isSnooze -> "⏰ Erteleme Sonu Hatırlatma"
            isFollowUp -> "⏰ Tekrar Hatırlatma"
            else -> "💉 İnsülin Hatırlatıcı"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .addAction(0, "10 dk Ertele", snoozePendingIntent)
            .addAction(0, "Alındı", markDonePendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 800, 500, 800))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(requestCode, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        if (!isFollowUp && !isSnooze && hour in 0..23 && minute in 0..59 && requestCode != -1) {
            AlarmScheduler.scheduleNextDay(
                context = context,
                hour = hour,
                minute = minute,
                label = label,
                units = units,
                requestCode = requestCode
            )
            AlarmScheduler.scheduleFollowUp(
                context = context,
                label = label,
                units = units,
                requestCode = requestCode
            )
        } else if (isFollowUp && requestCode != -1 && repeatCount < MAX_FOLLOW_UP_COUNT) {
            AlarmScheduler.scheduleFollowUp(
                context = context,
                label = label,
                units = units,
                requestCode = requestCode,
                repeatCount = repeatCount + 1
            )
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "İnsülin Hatırlatıcı",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Günlük insülin iğnesi bildirimleri"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 500, 800)
                setSound(
                    Settings.System.DEFAULT_ALARM_ALERT_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "insulin_channel"
        const val EXTRA_LABEL = "label"
        const val EXTRA_UNITS = "units"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_REQUEST_CODE = "request_code"
        const val EXTRA_IS_FOLLOW_UP = "is_follow_up"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_REPEAT_COUNT = "repeat_count"
        const val ACTION_MARK_DONE = "com.example.insulinneedlereminder.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.example.insulinneedlereminder.action.SNOOZE"
        private const val MAX_FOLLOW_UP_COUNT = 2
    }
}