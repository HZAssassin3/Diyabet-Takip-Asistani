package com.example.insulinneedlereminder.data.repository

import com.example.insulinneedlereminder.data.dao.InsulinDao
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class InsulinRepository(private val dao: InsulinDao) {

    fun getAll(): Flow<List<InsulinRecord>> = dao.getAll()

    fun getTodayRecords(): Flow<List<InsulinRecord>> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dao.getTodayRecords(startOfDay)
    }

    fun getByDateRange(from: Long, to: Long): Flow<List<InsulinRecord>> =
        dao.getByDateRange(from, to)

    suspend fun insert(record: InsulinRecord) = dao.insert(record)
    suspend fun update(record: InsulinRecord) = dao.update(record)
    suspend fun delete(record: InsulinRecord) = dao.delete(record)
}