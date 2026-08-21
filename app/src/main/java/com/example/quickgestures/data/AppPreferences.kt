package com.example.quickgestures.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quick_touch_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        const val SENSITIVITY_MIN = 1
        const val SENSITIVITY_MAX = 10
        const val SENSITIVITY_DEFAULT = 5

        private const val THRESHOLD_HARDEST = 22f
        private const val THRESHOLD_EASIEST = 8f

        fun sensitivityToThreshold(level: Int): Float {
            val clamped = level.coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX)
            val fraction = (clamped - SENSITIVITY_MIN).toFloat() / (SENSITIVITY_MAX - SENSITIVITY_MIN)
            return THRESHOLD_HARDEST - (THRESHOLD_HARDEST - THRESHOLD_EASIEST) * fraction
        }
    }

    // ---- حساسية الاهتزاز 1..10 ----
    var shakeSensitivityLevel: Int
        get() = prefs.getInt("shake_sensitivity_level", SENSITIVITY_DEFAULT).coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX)
        set(value) = prefs.edit().putInt("shake_sensitivity_level", value.coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX)).apply()

    fun currentShakeThreshold(): Float = sensitivityToThreshold(shakeSensitivityLevel)

    // ---- اهتزاز التأكيد: مفعّل أو لا + المدة ----
    var flashVibrationEnabled: Boolean
        get() = prefs.getBoolean("flash_vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("flash_vibration_enabled", value).apply()

    var flashConfirmVibrationMs: Int
        get() = prefs.getInt("flash_confirm_vibration_ms", 300).coerceIn(0, 3000)
        set(value) = prefs.edit().putInt("flash_confirm_vibration_ms", value.coerceIn(0, 3000)).apply()

    // ---- حساس التقارب (تجاهل الجيب) ----
    var proximityPocketGuardEnabled: Boolean
        get() = prefs.getBoolean("proximity_pocket_guard", true)
        set(value) = prefs.edit().putBoolean("proximity_pocket_guard", value).apply()

    // ---- إيماءات الحافة ----
    var edgeGestureActionMapping: Map<String, String>
        get() {
            val raw = prefs.getString("edge_gesture_mapping", null) ?: return emptyMap()
            return try { json.decodeFromString(raw) } catch (e: Exception) { emptyMap() }
        }
        set(value) = prefs.edit().putString("edge_gesture_mapping", json.encodeToString(value)).apply()

    var edgeGestureEnabled: Boolean
        get() = prefs.getBoolean("edge_gesture_enabled", false)
        set(value) = prefs.edit().putBoolean("edge_gesture_enabled", value).apply()

    // ---- كشف الاهتزاز: تفعيل + الإجراء المرتبط ----
    var shakeDetectorEnabled: Boolean
        get() = prefs.getBoolean("shake_detector_enabled", false)
        set(value) = prefs.edit().putBoolean("shake_detector_enabled", value).apply()

    var shakeTargetActionId: String
        get() = prefs.getString("shake_target_action_id", "flashlight_toggle") ?: "flashlight_toggle"
        set(value) = prefs.edit().putString("shake_target_action_id", value).apply()

    // ---- النقر على ظهر الهاتف (مرتين/ثلاث) ----
    var backTapEnabled: Boolean
        get() = prefs.getBoolean("back_tap_enabled", false)
        set(value) = prefs.edit().putBoolean("back_tap_enabled", value).apply()

    var backTapDoubleActionId: String?
        get() = prefs.getString("back_tap_double_action_id", null)
        set(value) = prefs.edit().putString("back_tap_double_action_id", value).apply()

    var backTapTripleActionId: String?
        get() = prefs.getString("back_tap_triple_action_id", null)
        set(value) = prefs.edit().putString("back_tap_triple_action_id", value).apply()

    // ---- بلاطات مركز التحكم (Quick Settings Tiles) القابلة للتخصيص ----
    var quickTileAction1Id: String?
        get() = prefs.getString("quick_tile_action_1", "flashlight_toggle")
        set(value) = prefs.edit().putString("quick_tile_action_1", value).apply()

    var quickTileAction2Id: String?
        get() = prefs.getString("quick_tile_action_2", null)
        set(value) = prefs.edit().putString("quick_tile_action_2", value).apply()

    // ---- مراقب سرعة الإنترنت ----
    var networkSpeedEnabled: Boolean
        get() = prefs.getBoolean("network_speed_enabled", false)
        set(value) = prefs.edit().putBoolean("network_speed_enabled", value).apply()

    var networkSpeedDisplayMode: NetworkSpeedDisplayMode
        get() = try {
            NetworkSpeedDisplayMode.valueOf(prefs.getString("network_speed_mode", NetworkSpeedDisplayMode.BOTH.name)!!)
        } catch (e: Exception) {
            NetworkSpeedDisplayMode.BOTH
        }
        set(value) = prefs.edit().putString("network_speed_mode", value.name).apply()

    // ---- الكرة العائمة ----
    enum class QuickBallMode { IN_APP_ONLY, SYSTEM_WIDE_OVERLAY }

    var quickBallEnabled: Boolean
        get() = prefs.getBoolean("quick_ball_enabled", false)
        set(value) = prefs.edit().putBoolean("quick_ball_enabled", value).apply()

    var quickBallMode: QuickBallMode
        get() = try {
            QuickBallMode.valueOf(prefs.getString("quick_ball_mode", QuickBallMode.IN_APP_ONLY.name)!!)
        } catch (e: Exception) {
            QuickBallMode.IN_APP_ONLY
        }
        set(value) = prefs.edit().putString("quick_ball_mode", value.name).apply()

    var quickBallRadialConfig: QuickBallRadialConfig
        get() {
            val raw = prefs.getString("quick_ball_radial_config", null) ?: return QuickBallRadialConfig.default()
            return try { json.decodeFromString(raw) } catch (e: Exception) { QuickBallRadialConfig.default() }
        }
        set(value) = prefs.edit().putString("quick_ball_radial_config", json.encodeToString(value)).apply()

    // ---- المظهر ----
    enum class ThemeMode { LIGHT, DARK, SYSTEM }

    var themeMode: ThemeMode
        get() = try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()
}

@Serializable
data class QuickBallRadialConfig(
    val selectedActionIds: List<String>,
    val itemsPerRing: Int = 6,
    val rotationOffsetDegrees: Float = 0f,
    val collapsedSizeDp: Int = 28,
    val centerBubbleSizeDp: Int = 56,
    val satelliteBubbleSizeDp: Int = 56
) {
    companion object {
        fun default() = QuickBallRadialConfig(selectedActionIds = emptyList(), itemsPerRing = 6, rotationOffsetDegrees = 0f)
    }
}
