package com.example.insulinneedlereminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insulin_records")
data class InsulinRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val timeLabel: String,
    val units: Int,
    val isDone: Boolean = false,
    val note: String = ""
)