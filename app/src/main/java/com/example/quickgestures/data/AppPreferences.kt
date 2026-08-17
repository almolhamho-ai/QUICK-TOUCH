package com.example.quickgestures.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * مخزن إعدادات مركزي واحد لكل التطبيق (SharedPreferences + JSON).
 * كل الميزات (الكرة، إيماءات الحافة، الروتينات، القفل، التسجيل...) بتقرأ/بتكتب من هون.
 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("quick_touch_prefs", Context.MODE_PRIVATE)

    // ---------- الكرة العائمة ----------
    var quickBallEnabled: Boolean
        get() = prefs.getBoolean("quick_ball_enabled", true)
        set(v) = prefs.edit().putBoolean("quick_ball_enabled", v).apply()

    var quickBallWorksOutsideApp: Boolean
        get() = prefs.getBoolean("quick_ball_outside", true)
        set(v) = prefs.edit().putBoolean("quick_ball_outside", v).apply()

    var quickBallActions: List<GestureAction>
        get() {
            val raw = prefs.getString("quick_ball_actions", null) ?: return listOf(
                GestureAction.GO_BACK, GestureAction.GO_HOME, GestureAction.TOGGLE_FLASHLIGHT,
                GestureAction.OPEN_CAMERA, GestureAction.MUTE_VOLUME, GestureAction.LOCK_SCREEN
            )
            val arr = JSONArray(raw)
            return (0 until arr.length()).mapNotNull { GestureAction.fromId(arr.getString(it)) }
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(it.id) }
            prefs.edit().putString("quick_ball_actions", arr.toString()).apply()
        }

    // ---------- إيماءات الحافة ----------
    var edgeGestureEnabled: Boolean
        get() = prefs.getBoolean("edge_gesture_enabled", true)
        set(v) = prefs.edit().putBoolean("edge_gesture_enabled", v).apply()

    fun setEdgeMapping(shape: EdgeShape, action: GestureAction) {
        prefs.edit().putString("edge_${shape.name}", action.id).apply()
    }

    fun getEdgeMapping(shape: EdgeShape): GestureAction? {
        val id = prefs.getString("edge_${shape.name}", null) ?: return when (shape) {
            EdgeShape.LINE -> GestureAction.GO_BACK
            EdgeShape.CORNER_L -> GestureAction.RECENT_APPS
            EdgeShape.HALF_CIRCLE -> GestureAction.TOGGLE_ONE_HANDED
        }
        return GestureAction.fromId(id)
    }

    // ---------- الروتينات ----------
    var routines: List<Routine>
        get() {
            val raw = prefs.getString("routines", "[]")!!
            val arr = JSONArray(raw)
            return (0 until arr.length()).mapNotNull { Routine.fromJson(arr.getJSONObject(it)) }
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(it.toJson()) }
            prefs.edit().putString("routines", arr.toString()).apply()
        }

    fun addOrUpdateRoutine(routine: Routine) {
        routines = routines.filterNot { it.id == routine.id } + routine
    }

    fun deleteRoutine(id: String) {
        routines = routines.filterNot { it.id == id }
    }

    // ---------- التسجيل الشفاف ----------
    /** طرق تفعيل التسجيل يلي فعّلها المستخدم من بين الخيارات المتاحة */
    var recordingTriggers: Set<String>
        get() = prefs.getStringSet("recording_triggers", setOf("quick_ball")) ?: setOf("quick_ball")
        set(v) = prefs.edit().putStringSet("recording_triggers", v).apply()

    // ---------- المعايرة التكيّفية ----------
    var shakeSensitivity: Float
        get() = prefs.getFloat("shake_sensitivity", 12f)
        set(v) = prefs.edit().putFloat("shake_sensitivity", v).apply()

    var adaptiveCalibrationEnabled: Boolean
        get() = prefs.getBoolean("adaptive_calibration", true)
        set(v) = prefs.edit().putBoolean("adaptive_calibration", v).apply()

    var proximityGuardEnabled: Boolean
        get() = prefs.getBoolean("proximity_guard", true)
        set(v) = prefs.edit().putBoolean("proximity_guard", v).apply()

    // ---------- وضعية اليد الواحدة ----------
    var oneHandedModeEnabled: Boolean
        get() = prefs.getBoolean("one_handed_enabled", false)
        set(v) = prefs.edit().putBoolean("one_handed_enabled", v).apply()

    // ---------- قفل التطبيقات ----------
    var lockedPackages: Set<String>
        get() = prefs.getStringSet("locked_packages", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("locked_packages", v).apply()

    /** true = استخدام قفل الجهاز نفسو (بصمة/نمط/رقم النظام)، false = رمز داخلي خاص بالتطبيق */
    var useDeviceLock: Boolean
        get() = prefs.getBoolean("use_device_lock", true)
        set(v) = prefs.edit().putBoolean("use_device_lock", v).apply()

    var customPinHash: String?
        get() = prefs.getString("custom_pin_hash", null)
        set(v) = prefs.edit().putString("custom_pin_hash", v).apply()

    // ---------- تصدير/استيراد كامل البروفايل ----------
    fun exportProfile(): JSONObject = JSONObject().apply {
        put("quickBallActions", JSONArray(quickBallActions.map { it.id }))
        put("routines", JSONArray(routines.map { it.toJson() }))
        put("shakeSensitivity", shakeSensitivity)
        put("recordingTriggers", JSONArray(recordingTriggers.toList()))
        put("lockedPackages", JSONArray(lockedPackages.toList()))
        put("oneHandedModeEnabled", oneHandedModeEnabled)
    }

    fun importProfile(json: JSONObject) {
        json.optJSONArray("quickBallActions")?.let { arr ->
            quickBallActions = (0 until arr.length()).mapNotNull { GestureAction.fromId(arr.getString(it)) }
        }
        json.optJSONArray("routines")?.let { arr ->
            routines = (0 until arr.length()).mapNotNull { Routine.fromJson(arr.getJSONObject(it)) }
        }
        if (json.has("shakeSensitivity")) shakeSensitivity = json.getDouble("shakeSensitivity").toFloat()
        json.optJSONArray("recordingTriggers")?.let { arr ->
            recordingTriggers = (0 until arr.length()).map { arr.getString(it) }.toSet()
        }
        json.optJSONArray("lockedPackages")?.let { arr ->
            lockedPackages = (0 until arr.length()).map { arr.getString(it) }.toSet()
        }
        if (json.has("oneHandedModeEnabled")) oneHandedModeEnabled = json.getBoolean("oneHandedModeEnabled")
    }
}
