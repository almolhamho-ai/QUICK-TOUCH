package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences

private val triggerOptions = listOf(
    "quick_ball" to "من الكرة العائمة",
    "shake" to "هز الهاتف",
    "edge_gesture" to "إيماءة حافة",
    "notification" to "زر بالإشعار"
)

@Composable
fun RecordingSettingsScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    var triggers by remember { mutableStateOf(prefs.recordingTriggers) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("التسجيل الصوتي", style = MaterialTheme.typography.titleLarge)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("⚠️ التسجيل شفاف بالكامل", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "بيظهر إشعار دائم على الشاشة طول فترة التسجيل، وما فيك تخفيه. " +
                        "جودة عالية (AAC 128kbps)، ويشتغل حتى لو الشاشة مقفولة. " +
                        "التسجيلات محفوظة بمجلد خاص محمي بقفل التطبيق دائماً.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("اختر طرق تفعيل التسجيل:", style = MaterialTheme.typography.bodyMedium)
        triggerOptions.forEach { (key, label) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label)
                Checkbox(
                    checked = key in triggers,
                    onCheckedChange = { checked ->
                        triggers = if (checked) triggers + key else triggers - key
                        prefs.recordingTriggers = triggers
                        onStateChanged()
                    }
                )
            }
        }
    }
}
