package com.example.quickgestures.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quickgestures.utils.AppLockManager

data class InstalledAppInfo(val packageName: String, val label: String)

@Composable
fun AppLockScreen(lockManager: AppLockManager, installedApps: List<InstalledAppInfo>, isAccessibilityEnabled: Boolean) {
    val context = LocalContext.current
    var lockedPackages by remember { mutableStateOf(lockManager.lockedPackages) }
    var lockMethod by remember { mutableStateOf(lockManager.lockMethod) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("قفل التطبيقات", style = MaterialTheme.typography.headlineSmall)

        if (!isAccessibilityEnabled) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text("تفعيل خدمة تسهيل الاستخدام") }
        }

        Spacer(Modifier.height(16.dp))
        Text("طريقة القفل", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = lockMethod == AppLockManager.LockMethod.DEVICE_BIOMETRIC,
                onClick = { lockMethod = AppLockManager.LockMethod.DEVICE_BIOMETRIC; lockManager.lockMethod = lockMethod }
            )
            Text("قفل الجهاز (بصمة/نمط/رقم)")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = lockMethod == AppLockManager.LockMethod.INTERNAL_PIN,
                onClick = { lockMethod = AppLockManager.LockMethod.INTERNAL_PIN; lockManager.lockMethod = lockMethod }
            )
            Text("رمز PIN داخلي مخصص")
        }

        Spacer(Modifier.height(16.dp))
        Text("التطبيقات المقفولة", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(installedApps) { app ->
                val checked = app.packageName in lockedPackages
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            lockedPackages = if (isChecked) lockedPackages + app.packageName else lockedPackages - app.packageName
                            lockManager.lockedPackages = lockedPackages
                        }
                    )
                    Text(app.label)
                }
            }
        }
    }
}
