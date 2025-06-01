package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.data_temp
import com.example.weather2.Repository.DataTempRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataTempViewModel(application: Application): AndroidViewModel(application) {

    private val repository = DataTempRepository()
    private val _dataTempList = MutableStateFlow<List<data_temp>>(emptyList())
    val dataTempList: StateFlow<List<data_temp>> =
        _dataTempList.asStateFlow()

    private val _latestDataTemp = MutableStateFlow<data_temp?>(null)
    val latestDataTemp: StateFlow<data_temp?> =
        _latestDataTemp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeDataTempList()
        observeLatestDataTemp()
    }

    private fun observeDataTempList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getDataTempList().collect { list ->
                    _dataTempList.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun observeLatestDataTemp() {
        viewModelScope.launch {
            try {
                repository.getLatestDataTemp().collect { latest ->
                    _latestDataTemp.value = latest
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Xóa dữ liệu nhiệt độ theo khoảng thời gian
     */
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            _isLoading.value = true
            val result = repository.deleteDataByDateRange(startDate, endDate)
            _isLoading.value = false

            if (result) {
                // Firebase sẽ tự động cập nhật UI qua listeners
                android.util.Log.d(
                    "TempViewModel",
                    "Xóa dữ liệu nhiệt độ thành công từ $startDate đến $endDate"
                )
            }

            result
        } catch (e: Exception) {
            _error.value = e.message
            _isLoading.value = false
            android.util.Log.e("TempViewModel", "Lỗi xóa dữ liệu nhiệt độ", e)
            false
        }
    }

    /**
     * Lấy dữ liệu nhiệt độ theo khoảng thời gian để xuất
     */
    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_temp> {
        return try {
            android.util.Log.d("TempViewModel", "Lấy dữ liệu nhiệt độ từ $startDate đến $endDate")
            repository.getDataByDateRange(startDate, endDate)
        } catch (e: Exception) {
            _error.value = e.message
            android.util.Log.e("TempViewModel", "Lỗi lấy dữ liệu nhiệt độ theo ngày", e)
            emptyList()
        }
    }

    fun clearError() {
        _error.value = null
    }
}