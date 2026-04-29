package com.example.insulinneedlereminder.data.repository

import com.example.insulinneedlereminder.data.dao.GlucoseDao
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class GlucoseRepository(private val dao: GlucoseDao) {

    fun getAll(): Flow<List<GlucoseRecord>> = dao.getAll()

    fun getTodayRecords(): Flow<List<GlucoseRecord>> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dao.getTodayRecords(startOfDay)
    }

    fun getByDateRange(from: Long, to: Long): Flow<List<GlucoseRecord>> =
        dao.getByDateRange(from, to)

    fun getLastN(limit: Int): Flow<List<GlucoseRecord>> = dao.getLastN(limit)

    suspend fun insert(record: GlucoseRecord) = dao.insert(record)
    suspend fun delete(record: GlucoseRecord) = dao.delete(record)
}