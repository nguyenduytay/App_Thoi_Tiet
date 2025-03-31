package com.example.weather2.Repository

import androidx.lifecycle.LiveData
import com.example.weather2.Model.Dao.TimerDao
import com.example.weather2.Model.Entity.Timer
import kotlinx.coroutines.flow.Flow

class TimerRepository(private val timerDao: TimerDao) {

    val allTimers: LiveData<List<Timer>> = timerDao.getAllTimers()

    suspend fun insert(timer: Timer) {
        timerDao.insertTimer(timer)
    }

    suspend fun delete(id: Int) {
        timerDao.deleteTimer(id)
    }

     fun getTimer(id : Int) : Flow<Timer> {
        return timerDao.getTimer(id)
    }
    suspend fun updateStatus(id: Int, status: Boolean) {
        timerDao.updateStatus(id, status)
    }
    suspend fun updateTimer(timer: Timer) {
        timerDao.updateTimer(timer)
    }
}

