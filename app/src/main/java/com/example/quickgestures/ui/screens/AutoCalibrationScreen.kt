package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences

@Composable
fun AutoCalibrationScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    var sensitivity by remember { mutableFloatStateOf(prefs.shakeSensitivity) }
    var adaptive by remember { mutableStateOf(prefs.adaptiveCalibrationEnabled) }
    var proximityGuard by remember { mutableStateOf(prefs.proximityGuardEnabled) }
    var oneHanded by remember { mutableStateOf(prefs.oneHandedModeEnabled) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("المعايرة والحركة", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("حساسية هز الهاتف الأساسية: ${"%.1f".format(sensitivity)}")
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        sensitivity = it; prefs.shakeSensitivity = it; onStateChanged()
                    },
                    valueRange = 5f..25f
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("معايرة تكيّفية تلقائية")
                Text("ترفع الحساسية تلقائياً وقت المشي أو بالسيارة لتقليل التفعيل الخاطئ", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = adaptive, onCheckedChange = { adaptive = it; prefs.adaptiveCalibrationEnabled = it; onStateChanged() })
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("حماية حساس التقارب")
                Text("يمنع التفعيل الخاطئ لما الهاتف بالجيب أو مغطى", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = proximityGuard, onCheckedChange = { proximityGuard = it; prefs.proximityGuardEnabled = it; onStateChanged() })
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("وضعية اليد الواحدة")
                Text(
                    "ملاحظة: أندرويد ما بيسمح لتطبيق عادي (بدون صلاحيات نظام) يحرّك كامل الشاشة. " +
                        "هاي الميزة بتفتح لوحة إجراءات مصغّرة قريبة من الإبهام بدل تحريك الشاشة كلها.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = oneHanded, onCheckedChange = { oneHanded = it; prefs.oneHandedModeEnabled = it; onStateChanged() })
        }
    }
}
