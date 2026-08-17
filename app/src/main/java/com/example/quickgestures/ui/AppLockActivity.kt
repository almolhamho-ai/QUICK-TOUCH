package com.example.quickgestures.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.utils.AppLockManager

/**
 * شاشة القفل الموحّدة: بتظهر قبل فتح أي تطبيق محدد بقائمة القفل، أو قبل الدخول على التسجيلات.
 */
class AppLockActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val lockManager = AppLockManager(this)
        val prefs = AppPreferences(this)

        setContent {
            MaterialTheme {
                var pinInput by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }

                fun onUnlocked() {
                    // إغلاق شاشة القفل يكفي للسماح للمستخدم بمتابعة فتح التطبيق المستهدف
                    finish()
                }

                LaunchedEffect(Unit) {
                    if (prefs.useDeviceLock && lockManager.deviceLockAvailable()) {
                        lockManager.authenticateWithDeviceLock(
                            this@AppLockActivity,
                            onSuccess = { onUnlocked() },
                            onError = { error = it }
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("هذا التطبيق مقفول", style = MaterialTheme.typography.titleLarge)

                    if (!prefs.useDeviceLock || !lockManager.deviceLockAvailable()) {
                        Spacer(Modifier.height(24.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("أدخل الرمز") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            if (lockManager.verifyCustomPin(pinInput)) onUnlocked()
                            else error = "رمز غير صحيح"
                        }) { Text("فتح") }
                    }

                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(24.dp))
                    TextButton(onClick = { finish(); moveTaskToBack(true) }) {
                        Text("رجوع")
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }
}
