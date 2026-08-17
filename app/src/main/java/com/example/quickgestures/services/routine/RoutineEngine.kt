package com.example.quickgestures.services.routine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.example.quickgestures.data.*
import com.example.quickgestures.utils.ActionExecutor
import java.text.SimpleDateFormat
import java.util.*

/**
 * محرك الروتينات بنمط "Samsung Routines": مُشغّل واحد + عدة شروط لازم تتحقق كلها + عدة إجراءات تنفذ بالتتابع.
 */
object RoutineEngine {

    fun onShakeTriggered(context: Context) {
        runMatching(context, TriggerType.SHAKE, null)
    }

    fun onEdgeGestureTriggered(context: Context, shape: EdgeShape) {
        runMatching(context, TriggerType.EDGE_GESTURE, shape.name)
    }

    fun onAppOpened(context: Context, packageName: String) {
        runMatching(context, TriggerType.APP_OPENED, packageName)
    }

    fun runRoutineById(context: Context, id: String) {
        val prefs = AppPreferences(context)
        prefs.routines.firstOrNull { it.id == id && it.enabled }?.let { execute(context, it) }
    }

    private fun runMatching(context: Context, type: TriggerType, value: String?) {
        val prefs = AppPreferences(context)
        prefs.routines
            .filter { it.enabled && it.trigger.type == type && (value == null || it.trigger.value == value) }
            .forEach { routine ->
                if (conditionsMet(context, routine.conditions)) execute(context, routine)
            }
    }

    private fun execute(context: Context, routine: Routine) {
        routine.actions.forEach { ref -> ActionExecutor.execute(context, ref) }
    }

    private fun conditionsMet(context: Context, conditions: List<RoutineCondition>): Boolean {
        return conditions.all { condition ->
            when (condition.type) {
                ConditionType.TIME_RANGE -> isWithinTimeRange(condition.value)
                ConditionType.WIFI_CONNECTED -> isWifiConnected(context)
                ConditionType.BATTERY_BELOW -> isBatteryBelow(context, condition.value?.toIntOrNull() ?: 100)
                ConditionType.MOTION_STATE -> true // بيتحدد فعلياً من ShakeDetectorService لحظة الإطلاق
                ConditionType.PROXIMITY_COVERED -> true
            }
        }
    }

    private fun isWithinTimeRange(range: String?): Boolean {
        if (range == null || !range.contains("-")) return true
        val (startStr, endStr) = range.split("-").map { it.trim() }
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val start = Calendar.getInstance().apply { time = fmt.parse(startStr) ?: return true }
        val end = Calendar.getInstance().apply { time = fmt.parse(endStr) ?: return true }
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = start.get(Calendar.HOUR_OF_DAY) * 60 + start.get(Calendar.MINUTE)
        val endMinutes = end.get(Calendar.HOUR_OF_DAY) * 60 + end.get(Calendar.MINUTE)
        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes..endMinutes
        } else { // مدى يعبر منتصف الليل
            nowMinutes >= startMinutes || nowMinutes <= endMinutes
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isBatteryBelow(context: Context, percent: Int): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level < percent
    }
}
