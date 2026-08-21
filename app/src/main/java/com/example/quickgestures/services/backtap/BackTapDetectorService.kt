package com.example.quickgestures.services.backtap

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.utils.ActionExecutor
import com.example.quickgestures.utils.buildAndStartSilentForegroundNotification
import kotlin.math.abs

/**
 * كشف نقر مرتين/ثلاث على ظهر الهاتف: يعتمد على مراقبة تسارع محور Z (العمودي على ظهر الجهاز)
 * وكشف قفزات قصيرة وحادة (Peaks) تشبه "طقّة" سريعة، ثم عدّ كم قفزة صارت خلال نافذة زمنية قصيرة.
 * هاي طريقة تقريبية (Heuristic) عامة تشتغل على أي جهاز بدون حاجة لدعم خاص من الشركة المصنّعة،
 * ودقتها معقولة بس مش مثالية 100% متل ميزات مدمجة بالنظام على أجهزة معينة.
 */
class BackTapDetectorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var prefs: AppPreferences
    private lateinit var actionExecutor: ActionExecutor

    private var lastZ = 0f
    private var lastPeakTime = 0L
    private val tapTimestamps = mutableListOf<Long>()
    private var evaluationScheduled = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        buildAndStartSilentForegroundNotification(
            notificationId = 4206,
            channelId = "quick_touch_back_tap_channel",
            channelName = "Quick Touch - النقر على الظهر",
            contentText = "كشف النقر على الظهر يعمل"
        )
        prefs = AppPreferences(applicationContext)
        actionExecutor = ActionExecutor(applicationContext)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val z = event.values[2]
        val delta = abs(z - lastZ)
        lastZ = z

        val now = System.currentTimeMillis()
        // قفزة حادة وقصيرة بمحور Z = "طقّة" محتملة على الظهر، بشرط فاصل زمني معقول عن آخر طقة
        if (delta > TAP_PEAK_THRESHOLD && now - lastPeakTime > MIN_GAP_BETWEEN_TAPS_MS) {
            lastPeakTime = now
            tapTimestamps.add(now)
            tapTimestamps.removeAll { now - it > EVALUATION_WINDOW_MS }
            scheduleEvaluation()
        }
    }

    private fun scheduleEvaluation() {
        if (evaluationScheduled) return
        evaluationScheduled = true
        handler.postDelayed({
            evaluationScheduled = false
            when (tapTimestamps.size) {
                2 -> triggerAction(prefs.backTapDoubleActionId)
                in 3..Int.MAX_VALUE -> triggerAction(prefs.backTapTripleActionId)
            }
            tapTimestamps.clear()
        }, EVALUATION_WINDOW_MS)
    }

    private fun triggerAction(actionId: String?) {
        val id = actionId ?: return
        GestureActionCatalog.byId(id)?.let { actionExecutor.execute(it) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAP_PEAK_THRESHOLD = 6.5f
        private const val MIN_GAP_BETWEEN_TAPS_MS = 80L
        private const val EVALUATION_WINDOW_MS = 450L
    }
}
