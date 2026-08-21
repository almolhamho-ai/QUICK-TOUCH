package com.example.quickgestures.services

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.utils.ActionExecutor
import com.example.quickgestures.utils.buildAndStartSilentForegroundNotification
import kotlin.math.sqrt

class ShakeDetectorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    private lateinit var prefs: AppPreferences
    private lateinit var vibrator: Vibrator
    private lateinit var actionExecutor: ActionExecutor

    @Volatile private var isCoveredByProximity = false

    private var lastAccel = floatArrayOf(0f, 0f, 0f)
    private var lastUpdateTime = 0L
    private var lastShakeTriggeredAt = 0L

    override fun onCreate() {
        super.onCreate()
        buildAndStartSilentForegroundNotification(
            notificationId = 4205,
            channelId = "quick_touch_shake_detector_channel",
            channelName = "Quick Touch - كشف الاهتزاز",
            contentText = "كشف الاهتزاز يعمل"
        )
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        prefs = AppPreferences(applicationContext)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        actionExecutor = ActionExecutor(applicationContext)

        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        if (prefs.proximityPocketGuardEnabled) {
            proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = proximitySensor?.maximumRange ?: 5f
                isCoveredByProximity = event.values[0] < maxRange
            }
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        if (prefs.proximityPocketGuardEnabled && isCoveredByProximity) return

        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < 60) return
        val dt = (now - lastUpdateTime).coerceAtLeast(1)
        lastUpdateTime = now

        val (x, y, z) = event.values
        val deltaX = x - lastAccel[0]
        val deltaY = y - lastAccel[1]
        val deltaZ = z - lastAccel[2]
        lastAccel = floatArrayOf(x, y, z)

        val jerk = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * (1000f / dt).coerceAtMost(60f)
        val threshold = prefs.currentShakeThreshold()

        if (jerk > threshold && now - lastShakeTriggeredAt > 1200) {
            lastShakeTriggeredAt = now
            onShakeDetected()
        }
    }

    /** 6) الهزة صارت تنفّذ أي إجراء يختاره المستخدم (افتراضياً الفلاش)، مش بس الفلاش دايماً */
    private fun onShakeDetected() {
        GestureActionCatalog.byId(prefs.shakeTargetActionId)?.let { actionExecutor.execute(it) }
        triggerConfirmVibrationIfEnabled()
    }

    private fun triggerConfirmVibrationIfEnabled() {
        if (!prefs.flashVibrationEnabled) return
        val durationMs = prefs.flashConfirmVibrationMs
        if (durationMs <= 0) return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
