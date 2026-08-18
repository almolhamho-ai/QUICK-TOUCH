package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class SettingsEntry(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

@Composable
fun GestureSettingsScreen(onNavigate: (String) -> Unit) {
    val entries = listOf(
        SettingsEntry("الكرة العائمة", "خصص الإجراءات، شغّلها جوا وبرا التطبيق", Icons.Default.RadioButtonChecked, "quick_ball"),
        SettingsEntry("إيماءات الحافة", "اربط شكل السحبة من الحافة بإجراء", Icons.Default.Gesture, "edge_gestures"),
        SettingsEntry("الروتينات", "شروط + إجراءات تلقائية بنمط Samsung Routines", Icons.Default.AutoAwesome, "routines"),
        SettingsEntry("التسجيل الصوتي", "جودة عالية، طرق تفعيل متعددة، إشعار دائم", Icons.Default.Mic, "recording"),
        SettingsEntry("قفل التطبيقات", "قفل موحّد لأي تطبيقات تختارها", Icons.Default.Lock, "app_lock"),
        SettingsEntry("المعايرة والحركة", "حساسية تكيّفية + حساس التقارب + اليد الواحدة", Icons.Default.Vibration, "calibration"),
        SettingsEntry("البروفايلات", "تصدير/استيراد كل الإعدادات كملف واحد", Icons.Default.ImportExport, "profiles"),
        SettingsEntry("مراقب سرعة الإنترنت", "سرعة الداونلود والأبلود بشريط الإشعارات", Icons.Default.NetworkCheck, "network_speed"),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("إعدادات لمسة سريعة", style = MaterialTheme.typography.titleLarge) }
        items(entries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate(entry.route) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                        Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}
