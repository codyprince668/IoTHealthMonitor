package com.example.iothealthmonitor.data.repository

import android.util.Log
import com.example.iothealthmonitor.data.model.AnomalyType
import com.example.iothealthmonitor.data.model.HealthData
import com.example.iothealthmonitor.data.model.MqttMessageDto
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

class MqttHealthRepository : HealthRepository {

    // 配置参数
    private val BROKER_URL = "tcp://broker.hivemq.com:1883"
    private val TOPIC = "topic/iot_group2jzu_2025_health_monitor_anomaly"
    private val CLIENT_ID = "AndroidApp_" + UUID.randomUUID().toString()

    private val gson = Gson()

    override fun getHealthDataStream(): Flow<HealthData> = callbackFlow {
        // 1. 初始化 MQTT 客户端
        val client = MqttAsyncClient(BROKER_URL, CLIENT_ID, MemoryPersistence())
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        // 2. 定义回调监听器
        val mqttCallback = object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.e("MQTT", "连接断开: ${cause?.message}")
                // 可以在这里处理重连逻辑，或者由 Paho 自动重连
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                try {
                    val payload = String(message!!.payload)
                    Log.d("MQTT", "收到消息: $payload")

                    // A. 解析 JSON 到 DTO
                    val dto = gson.fromJson(payload, MqttMessageDto::class.java)

                    // B. 将 DTO 转换为 App 内部使用的 HealthData
                    val healthData = mapToHealthData(dto)

                    // C. 发送到 Flow (trySend 是非阻塞的)
                    trySend(healthData)

                } catch (e: Exception) {
                    Log.e("MQTT", "解析错误: ${e.message}")
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // 我们只订阅，不发布，所以这里不需要处理
            }
        }

        client.setCallback(mqttCallback)

        // 3. 连接并订阅
        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "连接成功，正在订阅...")
                    client.subscribe(TOPIC, 1) // QoS 1
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "连接失败: ${exception?.message}")
                    // 关闭流以通知 UI 错误 (可选)
                    close(exception)
                }
            })
        } catch (e: Exception) {
            Log.e("MQTT", "客户端错误: ${e.message}")
            close(e)
        }

        // 4. 当 Flow 被取消时 (例如页面关闭)，断开连接
        awaitClose {
            Log.d("MQTT", "资源释放: 断开连接")
            try {
                if (client.isConnected) {
                    client.disconnect()
                }
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 辅助函数：将 DTO 转换为 HealthData
    private fun mapToHealthData(dto: MqttMessageDto): HealthData {
        // 映射异常类型字符串到枚举
        val type = when (dto.anomalyType) {
            "HeartRate" -> AnomalyType.HR_ANOMALY
            "SpO2" -> AnomalyType.SPO2_ANOMALY
            "Both" -> AnomalyType.BOTH_ANOMALY
            else -> AnomalyType.NONE
        }

        return HealthData(
            // 这里为了简化，直接使用接收时的系统时间。
            // 如果需要精确使用 JSON 里的 "2026-01-07T..."，需要写日期解析逻辑。
            timestamp = System.currentTimeMillis(),
            heartRate = dto.hr,
            spO2 = dto.spo2,
            isAnomaly = dto.isAnomaly,
            anomalyType = type
        )
    }
}