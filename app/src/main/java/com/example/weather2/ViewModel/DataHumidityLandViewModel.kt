package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.data_humidity_land
import com.example.weather2.Repository.DataHumidityLandRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataHumidityLandViewModel (application: Application): AndroidViewModel(application) {

    private val repository = DataHumidityLandRepository()
    private val _dataHumidityLandList = MutableStateFlow<List<data_humidity_land>>(emptyList())
    val dataHumidityLandList: StateFlow<List<data_humidity_land>> =
        _dataHumidityLandList.asStateFlow()

    private val _latestDataHumidityLand = MutableStateFlow<data_humidity_land?>(null)
    val latestDataHumidityLand: StateFlow<data_humidity_land?> =
        _latestDataHumidityLand.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeDataHumidityLandList()
        observeLatestDataHumidityLand()
    }

    private fun observeDataHumidityLandList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getDataHumidityLandList().collect { list ->
                    _dataHumidityLandList.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun observeLatestDataHumidityLand() {
        viewModelScope.launch {
            try {
                repository.getLatestDataHumidityLand().collect { latest ->
                    _latestDataHumidityLand.value = latest
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // Thêm vào cuối class DataHumidityLandViewModel, trước dấu }

    /**
     * Xóa dữ liệu độ ẩm đất theo khoảng thời gian
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
                    "Xóa dữ liệu thành công từ $startDate đến $endDate"
                )
            }

            result
        } catch (e: Exception) {
            _error.value = e.message
            _isLoading.value = false
            android.util.Log.e("HumidityViewModel", "Lỗi xóa dữ liệu", e)
            false
        }
    }

    /**
     * Lấy dữ liệu độ ẩm đất theo khoảng thời gian để xuất
     */
    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_humidity_land> {
        return try {
            android.util.Log.d("HumidityViewModel", "Lấy dữ liệu từ $startDate đến $endDate")
            repository.getDataByDateRange(startDate, endDate)
        } catch (e: Exception) {
            _error.value = e.message
            android.util.Log.e("HumidityViewModel", "Lỗi lấy dữ liệu theo ngày", e)
            emptyList()
        }
    }

    fun clearError() {
        _error.value = null
    }

}