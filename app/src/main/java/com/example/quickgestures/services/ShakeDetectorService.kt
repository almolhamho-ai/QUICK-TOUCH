package com.example.quickgestures.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.MotionState
import com.example.quickgestures.services.routine.RoutineEngine
import kotlin.math.sqrt

/**
 * كشف الاهتزاز + معايرة تكيّفية حسب نمط الحركة (ثابت / مشي / سيارة) باستخدام
 * تباين تسارع متحرك، وحساس التقارب لتفادي التفعيل الخاطئ لما الهاتف بالجيب.
 */
class ShakeDetectorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var prefs: AppPreferences

    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var smoothedDelta = 0f
    private var lastShakeTime = 0L

    // نافذة متحركة لتصنيف نمط الحركة
    private val varianceWindow = ArrayDeque<Float>()
    private val windowSize = 40

    private var isNearProximity = false

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = event.sensor.maximumRange
                isNearProximity = event.values[0] < maxRange
            }
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        lastAcceleration = currentAcceleration
        smoothedDelta = smoothedDelta * 0.9f + delta

        // نحدّث نافذة التباين لتصنيف نمط الحركة
        varianceWindow.addLast(delta)
        if (varianceWindow.size > windowSize) varianceWindow.removeFirst()

        val threshold = effectiveThreshold()

        // حماية الجيب: لو حساس التقارب مغطى وميزة الحماية مفعّلة، تجاهل الاهتزاز
        if (prefs.proximityGuardEnabled && isNearProximity) return

        if (smoothedDelta > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > 600) { // منع تكرار سريع
                lastShakeTime = now
                onShakeDetected()
            }
        }
    }

    private fun currentMotionState(): MotionState {
        if (varianceWindow.isEmpty()) return MotionState.STILL
        val avgVariance = varianceWindow.map { kotlin.math.abs(it) }.average()
        return when {
            avgVariance < 0.5 -> MotionState.STILL
            avgVariance < 2.5 -> MotionState.VEHICLE // اهتزاز منخفض ومستمر يشبه حركة السيارة
            else -> MotionState.WALKING
        }
    }

    /** الحساسية الأساسية يلي حددها المستخدم، مع رفعها تلقائياً وقت المشي/السيارة لتقليل التفعيل الخاطئ */
    private fun effectiveThreshold(): Float {
        val base = prefs.shakeSensitivity
        if (!prefs.adaptiveCalibrationEnabled) return base
        return when (currentMotionState()) {
            MotionState.STILL -> base
            MotionState.VEHICLE -> base * 1.3f
            MotionState.WALKING -> base * 1.6f
        }
    }

    private fun onShakeDetected() {
        vibrate()
        RoutineEngine.onShakeTriggered(this)
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
