// MockHealthRepository.kt
package com.example.iothealthmonitor.data.repository

import com.example.iothealthmonitor.data.model.AnomalyType
import com.example.iothealthmonitor.data.model.HealthData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class MockHealthRepository : HealthRepository {

    // 正常范围定义
    private val NORMAL_HR_RANGE = 60..100
    private val NORMAL_SPO2_RANGE = 95..100

    // 异常范围定义 (用于触发报警)
    private val ABNORMAL_HR_HIGH = 110..150
    private val ABNORMAL_SPO2_LOW = 80..92

    // 控制异常生成的计数器
    private var anomalySequenceCounter = 0
    private var currentAnomalyType = AnomalyType.NONE

    override fun getHealthDataStream(): Flow<HealthData> = flow {
        while (true) {
            // 1. 生成数据
            val data = generateNextData()

            // 2. 发射数据到 ViewModel
            emit(data)

            // 3. 根据文档要求，每2秒生成一次数据
            delay(2000)
        }
    }

    private fun generateNextData(): HealthData {
        // 如果当前不在异常序列中，随机决定是否开始一段异常 (10% 概率触发演示)
        if (anomalySequenceCounter == 0) {
            if (Random.nextInt(100) < 10) {
                startAnomalySequence()
            }
        }

        val hr: Int
        val spo2: Int
        val isAnomaly: Boolean
        val type: AnomalyType

        if (anomalySequenceCounter > 0) {
            // --- 生成异常数据模式 ---
            // 模拟 sensor_sim.py 的逻辑，确保异常能被快速触发

            type = currentAnomalyType
            isAnomaly = true

            // 根据当前选定的异常类型生成数值
            hr = when (type) {
                AnomalyType.HR_ANOMALY, AnomalyType.BOTH_ANOMALY -> ABNORMAL_HR_HIGH.random()
                else -> NORMAL_HR_RANGE.random()
            }

            spo2 = when (type) {
                AnomalyType.SPO2_ANOMALY, AnomalyType.BOTH_ANOMALY -> ABNORMAL_SPO2_LOW.random()
                else -> NORMAL_SPO2_RANGE.random()
            }

            // 递减计数器 (模拟"连续3次"的逻辑)
            anomalySequenceCounter--
        } else {
            // --- 生成正常数据 ---
            // 对应文档中的 "clean dataset" 概念 [cite: 26]
            hr = NORMAL_HR_RANGE.random()
            spo2 = NORMAL_SPO2_RANGE.random()
            isAnomaly = false
            type = AnomalyType.NONE
        }

        return HealthData(
            timestamp = System.currentTimeMillis(),
            heartRate = hr,
            spO2 = spo2,
            isAnomaly = isAnomaly,
            anomalyType = type
        )
    }

    /**
     * 随机选择文档中描述的三种异常模式之一
     * 1. 连续心率异常
     * 2. 连续血氧异常
     * 3. 同时异常
     */
    private fun startAnomalySequence() {
        // 设置连续 3 次异常，以符合"alert logic" [cite: 37]
        anomalySequenceCounter = 3

        // 随机选择一种异常类型
        val types = listOf(
            AnomalyType.HR_ANOMALY,
            AnomalyType.SPO2_ANOMALY,
            AnomalyType.BOTH_ANOMALY
        )
        currentAnomalyType = types.random()
    }
}