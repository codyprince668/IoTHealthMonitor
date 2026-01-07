package com.example.iothealthmonitor.data.model

import com.google.gson.annotations.SerializedName

// 这是一个数据传输对象 (DTO)，专门用于匹配服务器发来的 JSON 格式
data class MqttMessageDto(
    @SerializedName("timestamp") val timestampStr: String?,
    @SerializedName("hr") val hr: Int,
    @SerializedName("spo2") val spo2: Int,
    @SerializedName("device_id") val deviceId: String?,
    @SerializedName("is_anomaly") val isAnomaly: Boolean,
    @SerializedName("anomaly_type") val anomalyType: String? // 可能为 null
)