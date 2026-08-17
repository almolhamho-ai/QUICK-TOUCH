package com.example.quickgestures.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import org.json.JSONObject

@Composable
fun ProfilesScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(prefs.exportProfile().toString(2).toByteArray())
                }
                message = "تم تصدير البروفايل بنجاح"
            }.onFailure { message = "فشل التصدير: ${it.message}" }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val text = input.readBytes().decodeToString()
                    prefs.importProfile(JSONObject(text))
                }
                onStateChanged()
                message = "تم استيراد البروفايل بنجاح"
            }.onFailure { message = "فشل الاستيراد: ${it.message}" }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("البروفايلات", style = MaterialTheme.typography.titleLarge)
        Text(
            "صدّر كل إعداداتك (الكرة، إيماءات الحافة، الروتينات، القفل...) كملف واحد لمشاركته أو الاحتفاظ فيه، أو استورد ملف من صديق.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(onClick = { exportLauncher.launch("quick_touch_profile.json") }, modifier = Modifier.fillMaxWidth()) {
            Text("تصدير الإعدادات الحالية")
        }

        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) {
            Text("استيراد ملف بروفايل")
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
