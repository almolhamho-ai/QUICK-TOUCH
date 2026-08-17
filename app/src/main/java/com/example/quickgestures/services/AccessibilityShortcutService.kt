package com.example.quickgestures.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.ui.AppLockActivity

/**
 * الخدمة المركزية: بتنفذ الأزرار العامة (رجوع/هوم/الأخيرة)، وبتراقب أي تطبيق
 * صار بالمقدمة عشان تطبّق قفل التطبيقات، وبتنفذ وضعية اليد الواحدة.
 */
class AccessibilityShortcutService : AccessibilityService() {

    private lateinit var prefs: AppPreferences

    // كاش بسيط: التطبيقات يلي انفتحت وانفتح قفلها بهالجلسة (لغاية ما تروح للخلفية)
    private val unlockedThisSession = mutableSetOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = AppPreferences(this)
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return // ما نقفل نفس تطبيقنا وهو يعرض شاشة القفل

        if (packageName in prefs.lockedPackages && packageName !in unlockedThisSession) {
            val lockIntent = Intent(this, AppLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AppLockActivity.EXTRA_TARGET_PACKAGE, packageName)
            }
            startActivity(lockIntent)
        }
    }

    fun markUnlocked(packageName: String) {
        unlockedThisSession.add(packageName)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    companion object {
        private var instance: AccessibilityShortcutService? = null

        fun isServiceEnabled(): Boolean = instance != null

        fun performBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun performHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun performRecents() {
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun performLockScreen() {
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun toggleOneHanded() {
            // ملاحظة: أندرويد ما بيوفر API عام لتصغير/تحريك كامل الشاشة بدون صلاحيات نظام (root)
            // أو دعم مباشر من الشركة المصنّعة. الحل هون هو لوحة وصول مصغّرة قريبة من الإبهام
            // بدل تحريك الشاشة فعلياً بالكامل.
            instance?.let {
                val intent = Intent("com.example.quickgestures.TOGGLE_REACHABILITY_PANEL")
                it.sendBroadcast(intent)
            }
        }
    }
}
