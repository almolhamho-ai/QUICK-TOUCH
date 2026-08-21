package com.example.quickgestures.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.example.quickgestures.ui.AppLockActivity
import com.example.quickgestures.utils.AppLockManager

/**
 * ينفّذ الأزرار العامة (رجوع/هوم/التطبيقات الأخيرة/لقطة شاشة) المطلوبة من ActionExecutor،
 * ويراقب أي تطبيق يُفتح لإنفاذ قفل التطبيقات إذا كان محددًا بقائمة AppLockManager.lockedPackages.
 *
 * لازم المستخدم يفعّلها يدوياً من: الإعدادات > تسهيل الاستخدام (Accessibility) — هاد قيد
 * أمني إجباري من أندرويد نفسو، ما فيه طريقة لتفعيلها تلقائياً من داخل أي تطبيق عادي.
 */
class AccessibilityShortcutService : AccessibilityService() {

    private lateinit var lockManager: AppLockManager
    private var lastUnlockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        lockManager = AppLockManager(applicationContext)
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (packageName == this.packageName) return

        if (lockManager.isPackageLocked(packageName) && packageName != lastUnlockedPackage) {
            val intent = Intent(this, AppLockActivity::class.java).apply {
                putExtra(AppLockActivity.EXTRA_TARGET_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    fun performBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun performScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
    }

    companion object {
        /** مرجع للخدمة الشغالة حالياً، يستخدمه ActionExecutor لتنفيذ الأزرار العامة مباشرة */
        var instance: AccessibilityShortcutService? = null
            private set
    }
}
