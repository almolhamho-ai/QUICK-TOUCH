package com.example.quickgestures.utils

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.MediaStore
import com.example.quickgestures.data.GestureAction
import com.example.quickgestures.data.GestureActionRef
import com.example.quickgestures.services.AccessibilityShortcutService
import com.example.quickgestures.services.recording.QuickRecorderService

/**
 * نقطة تنفيذ واحدة لكل الإجراءات، تستخدمها الكرة العائمة وإيماءات الحافة والروتينات
 * حتى ما يتكرر منطق التنفيذ بأكتر من مكان.
 */
object ActionExecutor {

    private var flashOn = false

    fun execute(context: Context, ref: GestureActionRef) {
        when (ref.action) {
            GestureAction.GO_BACK -> AccessibilityShortcutService.performBack()
            GestureAction.GO_HOME -> AccessibilityShortcutService.performHome()
            GestureAction.RECENT_APPS -> AccessibilityShortcutService.performRecents()
            GestureAction.LOCK_SCREEN -> AccessibilityShortcutService.performLockScreen()
            GestureAction.TOGGLE_FLASHLIGHT -> toggleFlashlight(context)
            GestureAction.OPEN_CAMERA -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            GestureAction.MUTE_VOLUME -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            }
            GestureAction.VOLUME_UP -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            GestureAction.VOLUME_DOWN -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            GestureAction.TOGGLE_WIFI_PANEL -> {
                val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            GestureAction.START_STOP_RECORDING -> QuickRecorderService.toggle(context)
            GestureAction.TOGGLE_ONE_HANDED -> AccessibilityShortcutService.toggleOneHanded()
            GestureAction.OPEN_APP -> {
                ref.extra?.let { pkg ->
                    context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(it)
                    }
                }
            }
            GestureAction.RUN_ROUTINE -> {
                ref.extra?.let { routineId ->
                    com.example.quickgestures.services.routine.RoutineEngine.runRoutineById(context, routineId)
                }
            }
        }
    }

    private fun toggleFlashlight(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return
            flashOn = !flashOn
            cameraManager.setTorchMode(cameraId, flashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
