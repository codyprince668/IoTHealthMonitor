// HealthData.kt
package com.example.iothealthmonitor.data.model

data class HealthData(
    val timestamp: Long,
    val heartRate: Int,
    val spO2: Int,
    // 模拟后端AI模型(Autoencoder)判断的结果 [cite: 30, 34]
    val isAnomaly: Boolean,
    // 辅助字段，用于UI显示具体是什么异常 (可选)
    val anomalyType: AnomalyType = AnomalyType.NONE
)

enum class AnomalyType {
    NONE, HR_ANOMALY, SPO2_ANOMALY, BOTH_ANOMALY
}