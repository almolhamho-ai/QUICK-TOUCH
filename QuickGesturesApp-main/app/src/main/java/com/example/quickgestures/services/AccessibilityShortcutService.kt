package com.example.quickgestures.services

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.content.Intent

class AccessibilityShortcutService : AccessibilityService() {

    private var lastVolumeDownTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // فحص قفل التطبيقات عند فتح أي تطبيق جديد
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            // يمكنك هنا تنفيذ التحقق من قائمة التطبيقات المغلقة
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        // منع الاختصارات عند ظهور شريط ضبط الصوت على الشاشة
        if (isVolumeUiVisible()) return false

        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastVolumeDownTime < 300) { // نقر مزدوج
                triggerSecretRecording()
                return true
            }
            lastVolumeDownTime = currentTime
        }
        return super.onKeyEvent(event)
    }

    private fun isVolumeUiVisible(): Boolean {
        // منطق التحقق من شريط الصوت
        return false
    }

    private fun triggerSecretRecording() {
        val intent = Intent(this, SecretRecorderService::class.java)
        startService(intent)
    }

    override fun onInterrupt() {}
}
