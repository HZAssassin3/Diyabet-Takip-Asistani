package com.example.insulinneedlereminder.data.dao

import androidx.room.*
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinDao {

    @Query("SELECT * FROM insulin_records ORDER BY date DESC")
    fun getAll(): Flow<List<InsulinRecord>>

    @Query("SELECT * FROM insulin_records WHERE date >= :startOfDay ORDER BY date ASC")
    fun getTodayRecords(startOfDay: Long): Flow<List<InsulinRecord>>

    @Query("SELECT * FROM insulin_records WHERE date >= :from AND date <= :to ORDER BY date DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<InsulinRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InsulinRecord)

    @Update
    suspend fun update(record: InsulinRecord)

    @Delete
    suspend fun delete(record: InsulinRecord)
}