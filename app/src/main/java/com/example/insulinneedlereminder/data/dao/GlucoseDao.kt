package com.example.insulinneedlereminder.data.dao

import androidx.room.*
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {

    @Query("SELECT * FROM glucose_records ORDER BY date DESC")
    fun getAll(): Flow<List<GlucoseRecord>>

    @Query("SELECT * FROM glucose_records WHERE date >= :startOfDay ORDER BY date DESC")
    fun getTodayRecords(startOfDay: Long): Flow<List<GlucoseRecord>>

    @Query("SELECT * FROM glucose_records WHERE date >= :from AND date <= :to ORDER BY date ASC")
    fun getByDateRange(from: Long, to: Long): Flow<List<GlucoseRecord>>

    @Query("SELECT * FROM glucose_records ORDER BY date DESC LIMIT :limit")
    fun getLastN(limit: Int): Flow<List<GlucoseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GlucoseRecord)

    @Delete
    suspend fun delete(record: GlucoseRecord)

    @Query("SELECT * FROM glucose_records ORDER BY id DESC LIMIT 3")
    suspend fun getLastThreeDirect(): List<GlucoseRecord>
}