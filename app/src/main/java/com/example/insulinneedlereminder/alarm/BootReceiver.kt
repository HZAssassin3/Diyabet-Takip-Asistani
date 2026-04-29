package com.example.insulinneedlereminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.insulinneedlereminder.util.PrefsManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PrefsManager(context)

            if (prefs.morningEnabled) {
                AlarmScheduler.schedule(
                    context, prefs.morningHour, prefs.morningMinute,
                    "Sabah", prefs.morningUnits, AlarmScheduler.RequestCode.MORNING
                )
            }
            if (prefs.noonEnabled) {
                AlarmScheduler.schedule(
                    context, prefs.noonHour, prefs.noonMinute,
                    "Öğle", prefs.noonUnits, AlarmScheduler.RequestCode.NOON
                )
            }
            if (prefs.eveningEnabled) {
                AlarmScheduler.schedule(
                    context, prefs.eveningHour, prefs.eveningMinute,
                    "Akşam", prefs.eveningUnits, AlarmScheduler.RequestCode.EVENING
                )
            }
        }
    }
}