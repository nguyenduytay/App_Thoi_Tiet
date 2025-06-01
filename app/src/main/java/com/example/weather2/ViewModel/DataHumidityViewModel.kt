package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.data_humidity
import com.example.weather2.Repository.DataHumidityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataHumidityViewModel(application: Application): AndroidViewModel(application) {

    private val repository = DataHumidityRepository()
    private val _dataHumidityList = MutableStateFlow<List<data_humidity>>(emptyList())
    val dataHumidityList: StateFlow<List<data_humidity>> =
        _dataHumidityList.asStateFlow()

    private val _latestDataHumidity = MutableStateFlow<data_humidity?>(null)
    val latestDataHumidity: StateFlow<data_humidity?> =
        _latestDataHumidity.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeDataHumidityList()
        observeLatestDataHumidity()
    }

    private fun observeDataHumidityList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getDataHumidityList().collect { list ->
                    _dataHumidityList.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun observeLatestDataHumidity() {
        viewModelScope.launch {
            try {
                repository.getLatestDataHumidity().collect { latest ->
                    _latestDataHumidity.value = latest
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Xóa dữ liệu độ ẩm không khí theo khoảng thời gian
     */
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            _isLoading.value = true
            val result = repository.deleteDataByDateRange(startDate, endDate)
            _isLoading.value = false

            if (result) {
                // Firebase sẽ tự động cập nhật UI qua listeners
                android.util.Log.d(
                    "HumidityViewModel",
                    "Xóa dữ liệu độ ẩm không khí thành công từ $startDate đến $endDate"
                )
            }

            result
        } catch (e: Exception) {
            _error.value = e.message
            _isLoading.value = false
            android.util.Log.e("HumidityViewModel", "Lỗi xóa dữ liệu độ ẩm không khí", e)
            false
        }
    }

    /**
     * Lấy dữ liệu độ ẩm không khí theo khoảng thời gian để xuất
     */
    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_humidity> {
        return try {
            android.util.Log.d("HumidityViewModel", "Lấy dữ liệu độ ẩm không khí từ $startDate đến $endDate")
            repository.getDataByDateRange(startDate, endDate)
        } catch (e: Exception) {
            _error.value = e.message
            android.util.Log.e("HumidityViewModel", "Lỗi lấy dữ liệu độ ẩm không khí theo ngày", e)
            emptyList()
        }
    }

    fun clearError() {
        _error.value = null
    }
}