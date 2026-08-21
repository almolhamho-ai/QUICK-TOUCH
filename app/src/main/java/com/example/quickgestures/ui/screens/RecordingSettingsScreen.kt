package com.example.quickgestures.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.quickgestures.services.recording.QuickRecorderService
import com.example.quickgestures.utils.AppLockManager

@Composable
fun RecordingSettingsScreen() {
    val context = LocalContext.current
    val lockManager = remember { AppLockManager(context.applicationContext) }
    var pinSet by remember { mutableStateOf(lockManager.hasInternalPinSet()) }
    var isRecording by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("التسجيل الصوتي", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("تسجيل صوت بجودة عالية.", style = MaterialTheme.typography.bodyMedium)
        Text("المجلد: محفوظ التسجيلات", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(24.dp))

        if (!pinSet) {
            PinSetupSection(lockManager) { pinSet = true }
        } else {
            Button(onClick = {
                if (isRecording) {
                    context.stopService(Intent(context, QuickRecorderService::class.java))
                } else {
                    context.startForegroundService(Intent(context, QuickRecorderService::class.java))
                }
                isRecording = !isRecording
            }) {
                Text(if (isRecording) "إيقاف التسجيل" else "بدء التسجيل الآن")
            }
        }
    }
}

@Composable
private fun PinSetupSection(lockManager: AppLockManager, onDone: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it; error = false },
            label = { Text("رمز PIN") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it; error = false },
            label = { Text("تأكيد الرمز") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (error) {
            Text("الرمزين مش متطابقين", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            if (pin.isNotBlank() && pin == confirmPin) {
                lockManager.setInternalPin(pin)
                onDone()
            } else {
                error = true
            }
        }) { Text("حفظ") }
    }
}
