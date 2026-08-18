package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import android.provider.Settings
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.NetworkSpeedDisplayMode
import com.example.quickgestures.services.network.NetworkSpeedService

private data class ModeOption(val mode: NetworkSpeedDisplayMode, val title: String, val subtitle: String)

private val options = listOf(
    ModeOption(NetworkSpeedDisplayMode.BOTH, "الداونلود والأبلود معاً", "↓12KB/s ↑3KB/s"),
    ModeOption(NetworkSpeedDisplayMode.DOWNLOAD_ONLY, "الداونلود فقط", "↓12KB/s"),
    ModeOption(NetworkSpeedDisplayMode.UPLOAD_ONLY, "الأبلود فقط", "↑3KB/s"),
    ModeOption(NetworkSpeedDisplayMode.OFF, "إلغاء المؤشر", "لا يظهر شي"),
)

@Composable
fun NetworkSpeedScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(prefs.networkSpeedDisplayMode) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("مراقب سرعة الإنترنت", style = MaterialTheme.typography.titleLarge)
        Text(
            "بيظهر مؤشر صغير فوق منطقة شريط الحالة، جنب الساعة والبطارية والواي فاي، ويتحدّث كل ثانية.",
            style = MaterialTheme.typography.bodyMedium
        )

        options.forEach { option ->
            val selected = currentMode == option.mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (option.mode != NetworkSpeedDisplayMode.OFF && !Settings.canDrawOverlays(context)) {
                        context.startActivity(
                            android.content.Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                        return@Card
                    }
                    currentMode = option.mode
                    NetworkSpeedService.applyMode(context, option.mode)
                    onStateChanged()
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(option.title, style = MaterialTheme.typography.bodyLarge)
                        Text(option.subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selected) Icon(Icons.Default.CheckCircle, contentDescription = "مختار")
                }
            }
        }

        Text(
            "ملاحظة: لأسباب تقنية بأندرويد، ما فينا نحط أيقونة داخل شريط الحالة النظامي نفسه " +
                "بدون Root — هاد المؤشر نافذة عائمة ترتسم فوق نفس المنطقة فبتبين وكأنها جزء منو.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
