package com.example.iothealthmonitor.ui.viewmodel // 记得改成你的包名

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.iothealthmonitor.data.model.HealthData // 确保导入正确
import com.example.iothealthmonitor.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthViewModel(private val repository: HealthRepository) : ViewModel() {

    // UI 状态流
    private val _healthState = MutableStateFlow<HealthData?>(null)
    val healthState: StateFlow<HealthData?> = _healthState

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            repository.getHealthDataStream().collect { data ->
                _healthState.value = data
                // 这里处理报警逻辑，例如发送 LiveData 给 Activity 弹窗
            }
        }
    }
}

// === 必须添加这个 Factory 类，否则 Activity 无法初始化带参数的 ViewModel ===
class HealthViewModelFactory(private val repository: HealthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}