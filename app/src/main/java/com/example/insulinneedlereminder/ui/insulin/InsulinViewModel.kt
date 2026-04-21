package com.example.insulinneedlereminder.ui.insulin

import androidx.lifecycle.*
import com.example.insulinneedlereminder.data.entity.InsulinRecord
import com.example.insulinneedlereminder.data.repository.InsulinRepository
import kotlinx.coroutines.launch

class InsulinViewModel(private val repository: InsulinRepository) : ViewModel() {

    val allRecords: LiveData<List<InsulinRecord>> =
        repository.getAll().asLiveData()

    val todayRecords: LiveData<List<InsulinRecord>> =
        repository.getTodayRecords().asLiveData()

    fun insert(record: InsulinRecord) = viewModelScope.launch {
        repository.insert(record)
    }

    fun markDone(record: InsulinRecord) = viewModelScope.launch {
        repository.update(record.copy(isDone = true))
    }

    fun markUndone(record: InsulinRecord) = viewModelScope.launch {
        repository.update(record.copy(isDone = false))
    }

    fun delete(record: InsulinRecord) = viewModelScope.launch {
        repository.delete(record)
    }

    fun addRecord(timeLabel: String, units: Int, note: String = "") {
        insert(
            InsulinRecord(
                timeLabel = timeLabel,
                units = units,
                note = note,
                isDone = true
            )
        )
    }
}

class InsulinViewModelFactory(
    private val repository: InsulinRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsulinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsulinViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}