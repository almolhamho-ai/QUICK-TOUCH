package com.example.quickgestures.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.utils.AppLockManager

@Composable
fun AppLockScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    val context = LocalContext.current
    val lockManager = remember { AppLockManager(context) }
    var useDeviceLock by remember { mutableStateOf(prefs.useDeviceLock) }
    var locked by remember { mutableStateOf(prefs.lockedPackages) }
    var pinInput by remember { mutableStateOf("") }

    val installedApps = remember {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("قفل التطبيقات", style = MaterialTheme.typography.titleLarge)
        Text(
            "قفل موحّد لأي تطبيقات تختارها (شامل هذا التطبيق نفسه). التسجيلات الصوتية مقفولة دائماً بغض النظر عن هالإعداد.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("استخدم قفل الجهاز (بصمة/رمز النظام)")
            Switch(checked = useDeviceLock, onCheckedChange = {
                useDeviceLock = it; prefs.useDeviceLock = it; onStateChanged()
            })
        }

        if (!useDeviceLock) {
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it },
                label = { Text("عيّن رمز داخلي جديد") },
                singleLine = true
            )
            Button(onClick = {
                if (pinInput.length >= 4) {
                    lockManager.setCustomPin(pinInput)
                    pinInput = ""
                    onStateChanged()
                }
            }) { Text("حفظ الرمز") }
        }

        Text("اختر التطبيقات المقفولة:", style = MaterialTheme.typography.bodyMedium)

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(installedApps) { (pkg, label) ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label)
                    Checkbox(
                        checked = pkg in locked,
                        onCheckedChange = { checked ->
                            locked = if (checked) locked + pkg else locked - pkg
                            prefs.lockedPackages = locked
                            onStateChanged()
                        }
                    )
                }
            }
        }
    }
}
