// HealthRepository.kt
package com.example.iothealthmonitor.data.repository

import com.example.iothealthmonitor.data.model.HealthData
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    /**
     * 返回一个无限的数据流，每2秒发射一次数据
     */
    fun getHealthDataStream(): Flow<HealthData>
}