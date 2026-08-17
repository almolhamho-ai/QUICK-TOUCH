package com.example.quickgestures.data

/**
 * كل الإجراءات المتاحة يلي فيك تربطها بالكرة العائمة / إيماءات الحافة / الروتينات.
 * أضف أي إجراء جديد هون وبيصير متاح تلقائياً بكل الأماكن التلاتة.
 */
enum class GestureAction(val id: String, val label: String, val icon: String) {
    GO_BACK("go_back", "رجوع", "arrow_back"),
    GO_HOME("go_home", "الشاشة الرئيسية", "home"),
    RECENT_APPS("recent_apps", "التطبيقات الأخيرة", "apps"),
    LOCK_SCREEN("lock_screen", "قفل الشاشة", "lock"),
    TOGGLE_FLASHLIGHT("toggle_flashlight", "الفلاش", "flash_on"),
    OPEN_CAMERA("open_camera", "فتح الكاميرا", "photo_camera"),
    MUTE_VOLUME("mute_volume", "كتم الصوت", "volume_off"),
    VOLUME_UP("volume_up", "رفع الصوت", "volume_up"),
    VOLUME_DOWN("volume_down", "خفض الصوت", "volume_down"),
    TOGGLE_WIFI_PANEL("toggle_wifi_panel", "إعدادات الواي فاي", "wifi"),
    START_STOP_RECORDING("start_stop_recording", "تسجيل / إيقاف تسجيل", "mic"),
    TOGGLE_ONE_HANDED("toggle_one_handed", "وضعية اليد الواحدة", "pan_tool"),
    OPEN_APP("open_app", "فتح تطبيق محدد", "launch"),
    RUN_ROUTINE("run_routine", "تشغيل روتين", "auto_awesome");

    companion object {
        fun fromId(id: String): GestureAction? = entries.find { it.id == id }
    }
}

/** إشارة إجراء فعلي مربوط بقيمة إضافية (مثلاً اسم حزمة التطبيق أو معرّف الروتين). */
data class GestureActionRef(
    val action: GestureAction,
    val extra: String? = null
)
