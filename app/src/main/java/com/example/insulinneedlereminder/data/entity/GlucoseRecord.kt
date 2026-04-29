package com.example.insulinneedlereminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_records")
data class GlucoseRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val value: Int,
    val mealStatus: String,
    val note: String = ""
)