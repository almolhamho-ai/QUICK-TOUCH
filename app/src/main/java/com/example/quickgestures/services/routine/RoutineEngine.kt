package com.example.quickgestures.services.routine

import com.example.quickgestures.data.CompareOp
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.data.OrientationOption
import com.example.quickgestures.data.RingerModeOption
import com.example.quickgestures.data.Routine
import com.example.quickgestures.data.RoutineCondition
import com.example.quickgestures.utils.ActionExecutor
import java.util.Calendar

class RoutineEngine(
    private val actionExecutor: ActionExecutor,
    private val liveStateProvider: LiveStateProvider
) {

    fun onTriggerFired(routine: Routine) {
        if (!routine.enabled) return

        val stepsToRun = routine.actionSteps
            .sortedBy { it.order }
            .filter { step -> step.conditionsSatisfied(::evaluateCondition) }

        stepsToRun.forEach { step ->
            GestureActionCatalog.byId(step.actionId)?.let { action ->
                actionExecutor.execute(action)
            }
        }
    }

    private fun evaluateCondition(condition: RoutineCondition): Boolean = when (condition) {
        is RoutineCondition.TimeRange -> {
            val nowMinutes = currentMinuteOfDay()
            if (condition.startMinuteOfDay <= condition.endMinuteOfDay) {
                nowMinutes in condition.startMinuteOfDay..condition.endMinuteOfDay
            } else {
                nowMinutes >= condition.startMinuteOfDay || nowMinutes <= condition.endMinuteOfDay
            }
        }
        is RoutineCondition.WifiState -> {
            val connected = liveStateProvider.isWifiConnected()
            if (connected != condition.connected) return@evaluateCondition false
            if (condition.specificSsid.isNullOrBlank()) true
            else liveStateProvider.currentWifiSsid() == condition.specificSsid
        }
        is RoutineCondition.BatteryLevel -> {
            val current = liveStateProvider.currentBatteryPercent()
            when (condition.op) {
                CompareOp.LESS_THAN -> current < condition.percent
                CompareOp.GREATER_THAN -> current > condition.percent
                CompareOp.EQUALS -> current == condition.percent
            }
        }
        is RoutineCondition.ChargingState -> liveStateProvider.isCharging() == condition.isCharging
        is RoutineCondition.DayOfWeek -> {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            today in condition.days
        }
        is RoutineCondition.RingerModeState -> liveStateProvider.currentRingerMode() == condition.mode
        is RoutineCondition.HeadsetState -> liveStateProvider.isHeadsetConnected() == condition.connected
        is RoutineCondition.BluetoothState -> liveStateProvider.isBluetoothConnected() == condition.connected
        is RoutineCondition.ScreenOrientationState -> liveStateProvider.currentOrientation() == condition.orientation
    }

    private fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}

/** مصدر قراءة الحالة اللحظية للجهاز، يُمرَّر كتبعية لسهولة الاختبار والتوسعة المستقبلية */
interface LiveStateProvider {
    fun isWifiConnected(): Boolean
    fun currentWifiSsid(): String?
    fun currentBatteryPercent(): Int
    fun isCharging(): Boolean
    fun currentRingerMode(): RingerModeOption
    fun isHeadsetConnected(): Boolean
    fun isBluetoothConnected(): Boolean
    fun currentOrientation(): OrientationOption
}
