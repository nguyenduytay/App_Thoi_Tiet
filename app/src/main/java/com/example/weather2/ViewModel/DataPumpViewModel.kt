package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.data_pump
import com.example.weather2.Repository.DataPumpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataPumpViewModel(application: Application): AndroidViewModel(application)  {

    private val repository = DataPumpRepository()
    private val _dataPumpList = MutableStateFlow<List<data_pump>>(emptyList())
    val dataPumpList: StateFlow<List<data_pump>> = _dataPumpList.asStateFlow()

    private val _latestDataPumpStatus = MutableStateFlow<data_pump?>(null)
    val latestDataPumpStatus: StateFlow<data_pump?> = _latestDataPumpStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeDataPumpList()
        observeLatestDataPumpStatus()
    }

    private fun observeDataPumpList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getDataPumpList().collect { list ->
                    _dataPumpList.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun observeLatestDataPumpStatus() {
        viewModelScope.launch {
            try {
                repository.getLatestDataPumpStatus().collect { latest ->
                    _latestDataPumpStatus.value = latest
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // Thêm vào cuối class DataPumpViewModel, trước dấu }

    /**
     * Xóa dữ liệu máy bơm theo khoảng thời gian
     */
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            _isLoading.value = true
            val result = repository.deleteDataByDateRange(startDate, endDate)
            _isLoading.value = false

            if (result) {
                android.util.Log.d("PumpViewModel", "Xóa dữ liệu bơm thành công từ $startDate đến $endDate")
            }

            result
        } catch (e: Exception) {
            _error.value = e.message
            _isLoading.value = false
            android.util.Log.e("PumpViewModel", "Lỗi xóa dữ liệu bơm", e)
            false
        }
    }

    /**
     * Lấy dữ liệu máy bơm theo khoảng thời gian để xuất
     */
    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_pump> {
        return try {
            android.util.Log.d("PumpViewModel", "Lấy dữ liệu bơm từ $startDate đến $endDate")
            repository.getDataByDateRange(startDate, endDate)
        } catch (e: Exception) {
            _error.value = e.message
            android.util.Log.e("PumpViewModel", "Lỗi lấy dữ liệu bơm theo ngày", e)
            emptyList()
        }
    }

    fun clearError() {
        _error.value = null
    }
}