package com.example.insulinneedlereminder.util

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("insulin_prefs", Context.MODE_PRIVATE)

    var morningEnabled: Boolean
        get() = prefs.getBoolean("morning_enabled", false)
        set(v) = prefs.edit().putBoolean("morning_enabled", v).apply()
    var morningHour: Int
        get() = prefs.getInt("morning_hour", 8)
        set(v) = prefs.edit().putInt("morning_hour", v).apply()
    var morningMinute: Int
        get() = prefs.getInt("morning_minute", 0)
        set(v) = prefs.edit().putInt("morning_minute", v).apply()
    var morningUnits: Int
        get() = prefs.getInt("morning_units", 0)
        set(v) = prefs.edit().putInt("morning_units", v).apply()

    var noonEnabled: Boolean
        get() = prefs.getBoolean("noon_enabled", false)
        set(v) = prefs.edit().putBoolean("noon_enabled", v).apply()
    var noonHour: Int
        get() = prefs.getInt("noon_hour", 12)
        set(v) = prefs.edit().putInt("noon_hour", v).apply()
    var noonMinute: Int
        get() = prefs.getInt("noon_minute", 0)
        set(v) = prefs.edit().putInt("noon_minute", v).apply()
    var noonUnits: Int
        get() = prefs.getInt("noon_units", 0)
        set(v) = prefs.edit().putInt("noon_units", v).apply()

    var eveningEnabled: Boolean
        get() = prefs.getBoolean("evening_enabled", false)
        set(v) = prefs.edit().putBoolean("evening_enabled", v).apply()
    var eveningHour: Int
        get() = prefs.getInt("evening_hour", 19)
        set(v) = prefs.edit().putInt("evening_hour", v).apply()
    var eveningMinute: Int
        get() = prefs.getInt("evening_minute", 0)
        set(v) = prefs.edit().putInt("evening_minute", v).apply()
    var eveningUnits: Int
        get() = prefs.getInt("evening_units", 0)
        set(v) = prefs.edit().putInt("evening_units", v).apply()

    var glucoseLowThreshold: Int
        get() = prefs.getInt("glucose_low", 70)
        set(v) = prefs.edit().putInt("glucose_low", v).apply()
    var glucoseHighThreshold: Int
        get() = prefs.getInt("glucose_high", 180)
        set(v) = prefs.edit().putInt("glucose_high", v).apply()
}