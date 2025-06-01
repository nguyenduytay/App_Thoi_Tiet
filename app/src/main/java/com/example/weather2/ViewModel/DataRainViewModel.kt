package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.data_rain
import com.example.weather2.Repository.DataRainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataRainViewModel(application: Application): AndroidViewModel(application) {

    private val repository = DataRainRepository()
    private val _dataRainList = MutableStateFlow<List<data_rain>>(emptyList())
    val dataRainList: StateFlow<List<data_rain>> = _dataRainList.asStateFlow()

    private val _latestDataRain = MutableStateFlow<data_rain?>(null)
    val latestDataRain: StateFlow<data_rain?> = _latestDataRain.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeDataRainList()
        observeLatestRainData()
    }

    private fun observeDataRainList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getDataRainList().collect { list ->
                    _dataRainList.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun observeLatestRainData() {
        viewModelScope.launch {
            try {
                repository.getLatestDataRain().collect { latest ->
                    _latestDataRain.value = latest
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    // Thêm vào cuối class DataRainViewModel, trước dấu }

    /**
     * Xóa dữ liệu mưa theo khoảng thời gian
     */
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            _isLoading.value = true
            val result = repository.deleteDataByDateRange(startDate, endDate)
            _isLoading.value = false

            if (result) {
                android.util.Log.d("RainViewModel", "Xóa dữ liệu mưa thành công từ $startDate đến $endDate")
            }

            result
        } catch (e: Exception) {
            _error.value = e.message
            _isLoading.value = false
            android.util.Log.e("RainViewModel", "Lỗi xóa dữ liệu mưa", e)
            false
        }
    }

    /**
     * Lấy dữ liệu mưa theo khoảng thời gian để xuất
     */
    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_rain> {
        return try {
            android.util.Log.d("RainViewModel", "Lấy dữ liệu mưa từ $startDate đến $endDate")
            repository.getDataByDateRange(startDate, endDate)
        } catch (e: Exception) {
            _error.value = e.message
            android.util.Log.e("RainViewModel", "Lỗi lấy dữ liệu mưa theo ngày", e)
            emptyList()
        }
    }
    fun clearError() {
        _error.value = null
    }
}