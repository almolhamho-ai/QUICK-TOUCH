package com.example.quickgestures.data

import org.json.JSONArray
import org.json.JSONObject

enum class TriggerType { SHAKE, EDGE_GESTURE, APP_OPENED, TIME_OF_DAY, MANUAL }
enum class EdgeShape { LINE, CORNER_L, HALF_CIRCLE }
enum class ConditionType { TIME_RANGE, WIFI_CONNECTED, BATTERY_BELOW, MOTION_STATE, PROXIMITY_COVERED }
enum class MotionState { STILL, WALKING, VEHICLE }

data class RoutineTrigger(
    val type: TriggerType,
    /** حسب النوع: شكل الحافة، اسم حزمة التطبيق، أو "HH:mm" لوقت اليوم */
    val value: String? = null
)

data class RoutineCondition(
    val type: ConditionType,
    val value: String? = null // مثال: "22:00-06:00" لوقت، "30" لنسبة بطارية
)

data class Routine(
    val id: String,
    val name: String,
    val trigger: RoutineTrigger,
    val conditions: List<RoutineCondition> = emptyList(),
    val actions: List<GestureActionRef>,
    val enabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("enabled", enabled)
        put("trigger", JSONObject().apply {
            put("type", trigger.type.name)
            put("value", trigger.value ?: "")
        })
        put("conditions", JSONArray().apply {
            conditions.forEach {
                put(JSONObject().apply {
                    put("type", it.type.name)
                    put("value", it.value ?: "")
                })
            }
        })
        put("actions", JSONArray().apply {
            actions.forEach {
                put(JSONObject().apply {
                    put("action", it.action.id)
                    put("extra", it.extra ?: "")
                })
            }
        })
    }

    companion object {
        fun fromJson(o: JSONObject): Routine? {
            val actionArr = o.optJSONArray("actions") ?: return null
            val actions = (0 until actionArr.length()).mapNotNull { i ->
                val a = actionArr.getJSONObject(i)
                GestureAction.fromId(a.getString("action"))?.let { act ->
                    GestureActionRef(act, a.optString("extra").ifBlank { null })
                }
            }
            val condArr = o.optJSONArray("conditions")
            val conditions = condArr?.let { arr ->
                (0 until arr.length()).map { i ->
                    val c = arr.getJSONObject(i)
                    RoutineCondition(ConditionType.valueOf(c.getString("type")), c.optString("value").ifBlank { null })
                }
            } ?: emptyList()
            val t = o.getJSONObject("trigger")
            return Routine(
                id = o.getString("id"),
                name = o.getString("name"),
                trigger = RoutineTrigger(TriggerType.valueOf(t.getString("type")), t.optString("value").ifBlank { null }),
                conditions = conditions,
                actions = actions,
                enabled = o.optBoolean("enabled", true)
            )
        }
    }
}
