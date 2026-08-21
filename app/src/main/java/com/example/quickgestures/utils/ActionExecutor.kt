package com.example.quickgestures.utils

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import com.example.quickgestures.data.GestureAction
import com.example.quickgestures.services.AccessibilityShortcutService
import com.example.quickgestures.services.recording.QuickRecorderService

/** منفّذ مركزي لكل الإجراءات المتاحة بالكتالوج، يُستدعى من الكرة، إيماءات الحافة، والروتينات. */
class ActionExecutor(private val context: Context) {

    fun execute(action: GestureAction) {
        when (action.id) {
            "flashlight_toggle" -> toggleFlashlight()
            "screenshot" -> AccessibilityShortcutService.instance?.performScreenshot()
            "back" -> AccessibilityShortcutService.instance?.performBack()
            "home" -> AccessibilityShortcutService.instance?.performHome()
            "recents" -> AccessibilityShortcutService.instance?.performRecents()
            "volume_mute" -> toggleMute()
            "media_play_pause" -> sendMediaKey()
            "wifi_toggle" -> openQuickPanel(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            "bt_toggle" -> context.startActivity(
                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "dnd_toggle" -> toggleDoNotDisturb()
            "start_recording" -> startTransparentRecording()
            "open_app" -> { /* يحتاج اختيار تطبيق محدد من واجهة الإعدادات */ }
            else -> { /* إجراء غير معروف - تجاهل بأمان */ }
        }
    }

    private var torchOn = false

    private fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            cameraManager.setTorchMode(cameraId, torchOn)
        } catch (e: Exception) {
            // بعض الأجهزة برفض تشغيل الفلاش إذا الكاميرا مستخدمة بتطبيق تاني بنفس اللحظة
        }
    }

    private fun toggleMute() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (current > 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } else {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max / 2, 0)
        }
    }

    private fun sendMediaKey() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        )
    }

    private fun openQuickPanel(panelAction: String) {
        try {
            context.startActivity(Intent(panelAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun toggleDoNotDisturb() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val newFilter = if (notificationManager.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
            android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            android.app.NotificationManager.INTERRUPTION_FILTER_ALL
        }
        notificationManager.setInterruptionFilter(newFilter)
    }

    private fun startTransparentRecording() {
        val lockManager = AppLockManager(context)
        if (!lockManager.hasInternalPinSet()) return // لازم رمز PIN معد مسبقاً قبل ما تشتغل الميزة
        val intent = Intent(context, QuickRecorderService::class.java)
        context.startForegroundService(intent)
    }
}
