package com.example.iothealthmonitor

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.iothealthmonitor.data.model.HealthData
import com.example.iothealthmonitor.data.repository.MockHealthRepository
import com.example.iothealthmonitor.data.repository.MqttHealthRepository
import com.example.iothealthmonitor.ui.viewmodel.HealthViewModel
import com.example.iothealthmonitor.ui.viewmodel.HealthViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // 1. get ViewModel (using Factory)
//    private val viewModel: HealthViewModel by viewModels {
//        HealthViewModelFactory(MockHealthRepository())
//    }
    // ✅ [检查点 1] viewModel 必须定义在这里（类成员变量），不能在 onCreate 里面！
    private val viewModel: HealthViewModel by viewModels {
        // 注意：这里需要传入 Repository 实例，现在你用的是 MqttHealthRepository
        HealthViewModelFactory(MqttHealthRepository())
    }
    // UI components
    private lateinit var chartHr: LineChart
    private lateinit var chartSpo2: LineChart
    private lateinit var tvStatus: TextView
    private lateinit var tvHr: TextView
    private lateinit var tvSpo2: TextView

    // counter (用于图表X轴的递增)
    private var timeIndex = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化控件
        chartHr = findViewById(R.id.chartHeartRate)
        chartSpo2 = findViewById(R.id.chartSpo2)
        tvStatus = findViewById(R.id.tvStatus)
        tvHr = findViewById(R.id.tvHrValue)
        tvSpo2 = findViewById(R.id.tvSpo2Value)

        // 2. 初始化图表配置
        setupChart(chartHr, "Heart Rate (BPM)", Color.RED, 40f, 180f)
        setupChart(chartSpo2, "Blood Oxygen Saturation (%)", Color.BLUE, 80f, 100f)

        // 3. 开始监听数据
        observeData()
    }

    private fun setupChart(chart: LineChart, label: String, color: Int, min: Float, max: Float) {
        chart.apply {
            description = Description().apply { text = "" } // 去掉右下角描述
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setDrawGridBackground(false)

            // X轴配置
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)

            // Y轴配置 (左侧)
            axisLeft.apply {
                axisMinimum = min
                axisMaximum = max
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false // 隐藏右侧Y轴

            // 初始化空数据
            val dataSet = LineDataSet(ArrayList(), label).apply {
                this.color = color
                setDrawCircles(true)
                setCircleColor(color)
                circleRadius = 3f
                lineWidth = 2f
                setDrawValues(false) // 不在点上显示数值，太乱
                mode = LineDataSet.Mode.CUBIC_BEZIER // 曲线平滑
            }
            data = LineData(dataSet)
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.healthState.collect { data ->
                data?.let {
                    updateUI(it)
                }
            }
        }
    }

    private fun updateUI(data: HealthData) {
        // 1. 更新文本显示
        tvHr.text = "HR: ${data.heartRate} BPM"
        tvSpo2.text = "SpO2: ${data.spO2} %"

        // 2. 处理异常报警 (UI推送)
        if (data.isAnomaly) {
            tvStatus.text = "⚠️ Warning: Anamoly Detected!"
            tvStatus.setBackgroundColor(Color.RED)
            tvStatus.setTextColor(Color.WHITE)
            Toast.makeText(this, "Waring: Vital signs anomaly detected！", Toast.LENGTH_SHORT).show()
        } else {
            tvStatus.text = "System Status: Monitoring (Normal)"
            tvStatus.setBackgroundColor(Color.parseColor("#4CAF50")) // 绿色
            tvStatus.setTextColor(Color.WHITE)
        }

        // 3. 动态添加数据点到图表
        addEntryToChart(chartHr, data.heartRate.toFloat())
        addEntryToChart(chartSpo2, data.spO2.toFloat())

        // X轴时间递增
        timeIndex++
    }

    private fun addEntryToChart(chart: LineChart, value: Float) {
        val data = chart.data
        if (data != null) {
            var set = data.getDataSetByIndex(0)
            if (set == null) {
                set = LineDataSet(null, "Data")
                data.addDataSet(set)
            }

            // 添加新点 (X = timeIndex, Y = value)
            data.addEntry(Entry(timeIndex, value), 0)

            // 通知数据更新
            data.notifyDataChanged()
            chart.notifyDataSetChanged()

            // 限制屏幕显示的点的数量 (例如只显示最近20个点)
            chart.setVisibleXRangeMaximum(20f)

            // 自动移动到最新的点
            chart.moveViewToX(data.entryCount.toFloat())
        }
    }
}