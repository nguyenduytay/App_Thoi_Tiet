package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weather2.Database.AppDatabase
import com.example.weather2.Model.Entity.Warning
import com.example.weather2.Repository.WarningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WarningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WarningRepository
    private val _allWarnings = MutableLiveData<List<Warning>>()
    val allWarnings: LiveData<List<Warning>> get() = _allWarnings

    init {
        val warningDao = AppDatabase.getDatabase(application).warningDao()
        repository = WarningRepository(warningDao)
        fetchAllWarnings()
    }

    private fun fetchAllWarnings() {
        viewModelScope.launch(Dispatchers.IO) {
            val warnings = repository.getAllWarnings()
            _allWarnings.postValue(warnings)
        }
    }

    fun insert(warning: Warning) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(warning)
            fetchAllWarnings()
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(id)
            fetchAllWarnings()
        }
    }
    fun updateWarning(warning : Warning)
    {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWarning(warning)
        }
    }
     fun getWarning(id: Int): Flow<Warning> {
        return repository.getWarning(id)
    }

}
