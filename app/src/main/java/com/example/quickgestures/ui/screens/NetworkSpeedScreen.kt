package com.example.quickgestures.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.NetworkSpeedDisplayMode
import com.example.quickgestures.services.network.NetworkSpeedService

@Composable
fun NetworkSpeedScreen(prefs: AppPreferences) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(prefs.networkSpeedEnabled) }
    var mode by remember { mutableStateOf(prefs.networkSpeedDisplayMode) }

    fun applyState(newEnabled: Boolean, newMode: NetworkSpeedDisplayMode) {
        prefs.networkSpeedEnabled = newEnabled
        prefs.networkSpeedDisplayMode = newMode
        val intent = Intent(context, NetworkSpeedService::class.java)
        if (newEnabled) {
            if (!Settings.canDrawOverlays(context)) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
                return
            }
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("مراقب سرعة الإنترنت", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تفعيل", style = MaterialTheme.typography.titleMedium)
            Switch(checked = enabled, onCheckedChange = { checked -> enabled = checked; applyState(checked, mode) })
        }

        Spacer(Modifier.height(16.dp))

        val options = listOf(
            NetworkSpeedDisplayMode.DOWNLOAD_ONLY to "تنزيل",
            NetworkSpeedDisplayMode.UPLOAD_ONLY to "رفع",
            NetworkSpeedDisplayMode.BOTH to "تنزيل ورفع"
        )

        options.forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = mode == value,
                    enabled = enabled,
                    onClick = { mode = value; applyState(enabled, value) }
                )
                Text(label)
            }
        }
    }
}
