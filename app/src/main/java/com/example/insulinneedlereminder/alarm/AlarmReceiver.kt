package com.example.insulinneedlereminder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.insulinneedlereminder.MainActivity
import com.example.insulinneedlereminder.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "İnsülin"
        val units = intent.getIntExtra(EXTRA_UNITS, 0)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)

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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💉 İnsülin Hatırlatıcı")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(label.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        if (hour in 0..23 && minute in 0..59 && requestCode != -1) {
            AlarmScheduler.scheduleNextDay(
                context = context,
                hour = hour,
                minute = minute,
                label = label,
                units = units,
                requestCode = requestCode
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
    }
}