package com.example.quickgestures.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed class RoutineTrigger {
    @Serializable data class Shake(val placeholder: Boolean = true) : RoutineTrigger()
    @Serializable data class EdgeGesture(val shape: String) : RoutineTrigger()
    @Serializable data class AppOpened(val packageName: String) : RoutineTrigger()

    fun shortLabel(): String = when (this) {
        is Shake -> "هزة"
        is EdgeGesture -> "إيماءة حافة"
        is AppOpened -> "فتح تطبيق"
    }
}

/**
 * شرط واحد. كل إجراء (ActionStep) يحمل قائمة شروطه الخاصة به، ويسمح باختيار أكثر من شرط
 * بنفس الوقت (تُقيَّم كلها بمنطق AND). تمت إضافة أنواع أكتر + قيم قابلة للتخصيص الكامل
 * (مش قيم ثابتة متل قبل).
 */
@Serializable
sealed class RoutineCondition {
    @Serializable data class TimeRange(val startMinuteOfDay: Int, val endMinuteOfDay: Int) : RoutineCondition()
    @Serializable data class WifiState(val connected: Boolean, val specificSsid: String? = null) : RoutineCondition()
    @Serializable data class BatteryLevel(val op: CompareOp, val percent: Int) : RoutineCondition()
    @Serializable data class ChargingState(val isCharging: Boolean) : RoutineCondition()
    @Serializable data class DayOfWeek(val days: Set<Int>) : RoutineCondition() // 1=الأحد..7=السبت
    @Serializable data class RingerModeState(val mode: RingerModeOption) : RoutineCondition()
    @Serializable data class HeadsetState(val connected: Boolean) : RoutineCondition()
    @Serializable data class BluetoothState(val connected: Boolean) : RoutineCondition()
    @Serializable data class ScreenOrientationState(val orientation: OrientationOption) : RoutineCondition()

    fun shortLabel(): String = when (this) {
        is TimeRange -> "الوقت"
        is WifiState -> "الواي فاي"
        is BatteryLevel -> "نسبة البطارية"
        is ChargingState -> "حالة الشحن"
        is DayOfWeek -> "أيام الأسبوع"
        is RingerModeState -> "وضعية الصوت"
        is HeadsetState -> "السماعة"
        is BluetoothState -> "البلوتوث"
        is ScreenOrientationState -> "اتجاه الشاشة"
    }
}

@Serializable
enum class CompareOp { LESS_THAN, GREATER_THAN, EQUALS }

@Serializable
enum class RingerModeOption { SILENT, VIBRATE, NORMAL }

@Serializable
enum class OrientationOption { PORTRAIT, LANDSCAPE }

@Serializable
data class ActionStep(
    val id: String = UUID.randomUUID().toString(),
    val actionId: String,
    val conditions: List<RoutineCondition> = emptyList(),
    val order: Int = 0
) {
    fun conditionsSatisfied(evaluator: (RoutineCondition) -> Boolean): Boolean =
        conditions.all(evaluator)
}

@Serializable
data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val userGivenName: String? = null,
    val trigger: RoutineTrigger,
    val actionSteps: List<ActionStep>,
    val enabled: Boolean = true
) {
    fun resolvedName(actionLookup: (String) -> GestureAction?): String {
        if (!userGivenName.isNullOrBlank()) return userGivenName

        val triggerPart = trigger.shortLabel()
        val actionNames = actionSteps
            .sortedBy { it.order }
            .mapNotNull { actionLookup(it.actionId)?.displayLabel }

        return when {
            actionNames.isEmpty() -> "روتين ($triggerPart)"
            actionNames.size == 1 -> "$triggerPart ← ${actionNames.first()}"
            else -> "$triggerPart ← ${actionNames.take(2).joinToString(" + ")}" +
                    if (actionNames.size > 2) " (+${actionNames.size - 2})" else ""
        }
    }
}
