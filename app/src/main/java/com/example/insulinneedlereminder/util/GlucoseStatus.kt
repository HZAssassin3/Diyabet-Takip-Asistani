package com.example.insulinneedlereminder.util

enum class GlucoseStatus {
    LOW,
    NORMAL,
    HIGH;

    companion object {
        fun from(value: Int, low: Int = 70, high: Int = 180): GlucoseStatus = when {
            value < low  -> LOW
            value > high -> HIGH
            else         -> NORMAL
        }
    }
}