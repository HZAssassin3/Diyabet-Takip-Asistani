package com.example.insulinneedlereminder.ui.glucose

import androidx.lifecycle.*
import com.example.insulinneedlereminder.data.entity.GlucoseRecord
import com.example.insulinneedlereminder.data.repository.GlucoseRepository
import com.example.insulinneedlereminder.util.GlucoseStatus
import kotlinx.coroutines.launch

class GlucoseViewModel(private val repository: GlucoseRepository) : ViewModel() {

    val allRecords: LiveData<List<GlucoseRecord>> =
        repository.getAll().asLiveData()

    val todayRecords: LiveData<List<GlucoseRecord>> =
        repository.getTodayRecords().asLiveData()

    val lastRecords: LiveData<List<GlucoseRecord>> =
        repository.getLastN(50).asLiveData()

    private var lowThreshold = 70
    private var highThreshold = 180

    fun setThresholds(low: Int, high: Int) {
        lowThreshold = low
        highThreshold = high
    }

    fun getStatus(value: Int): GlucoseStatus =
        GlucoseStatus.from(value, lowThreshold, highThreshold)

    fun insert(value: Int, mealStatus: String, note: String = "") = viewModelScope.launch {
        repository.insert(
            GlucoseRecord(
                value = value,
                mealStatus = mealStatus,
                note = note
            )
        )
    }

    fun delete(record: GlucoseRecord) = viewModelScope.launch {
        repository.delete(record)
    }
}

class GlucoseViewModelFactory(
    private val repository: GlucoseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlucoseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GlucoseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}