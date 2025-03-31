package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.*
import com.example.weather2.Database.AppDatabase
import com.example.weather2.Model.Entity.Timer
import com.example.weather2.Model.Entity.Warning
import com.example.weather2.Repository.TimerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TimerRepository
    private val allTimers: LiveData<List<Timer>>
    init {
        val timerDao = AppDatabase.getDatabase(application).timerDao()
        repository = TimerRepository(timerDao)
        allTimers = repository.allTimers
    }
    fun insert(timer: Timer) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(timer)
        }
    }
    fun delete(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(id)
        }
    }
    fun updateStatus(id: Int, status: Boolean)
    {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(id, status)
        }
    }
    fun updateTimer(timer: Timer)
    {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTimer(timer)
        }
    }
     fun getTimer(id: Int): Flow<Timer> {
        return repository.getTimer(id)
    }
}
