package com.example.quickgestures.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.quickgestures.data.AppPreferences
import java.security.MessageDigest

/**
 * مدير قفل موحّد لكل التطبيقات يلي يختارها المستخدم (شامل هاد التطبيق نفسو وشاشة التسجيلات).
 * طريقة القفل خيارية: نفس قفل الجهاز (بصمة/نمط/رقم النظام) أو رمز PIN داخلي خاص بالتطبيق.
 */
class AppLockManager(private val context: Context) {

    private val prefs = AppPreferences(context)

    fun isLocked(packageName: String): Boolean = packageName in prefs.lockedPackages

    fun lockApp(packageName: String) {
        prefs.lockedPackages = prefs.lockedPackages + packageName
    }

    fun unlockAppFromList(packageName: String) {
        prefs.lockedPackages = prefs.lockedPackages - packageName
    }

    fun deviceLockAvailable(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /** استخدام قفل الجهاز نفسه (بصمة أو رمز/نمط النظام) للتحقق */
    fun authenticateWithDeviceLock(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("فتح القفل")
            .setSubtitle("استخدم بصمتك أو رمز/نمط قفل الجهاز")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // ---------- رمز PIN داخلي بديل (إذا اختار المستخدم قفل خاص بالتطبيق بدل قفل الجهاز) ----------
    fun setCustomPin(pin: String) {
        prefs.customPinHash = hash(pin)
    }

    fun verifyCustomPin(pin: String): Boolean = prefs.customPinHash == hash(pin)

    fun hasCustomPin(): Boolean = prefs.customPinHash != null

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
