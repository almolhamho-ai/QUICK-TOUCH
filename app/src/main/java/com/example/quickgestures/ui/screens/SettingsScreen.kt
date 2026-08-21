package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences

@Composable
fun SettingsScreen(prefs: AppPreferences) {
    var theme by remember { mutableStateOf(prefs.themeMode) }
    var vibrationEnabled by remember { mutableStateOf(prefs.flashVibrationEnabled) }
    var vibrationMs by remember { mutableIntStateOf(prefs.flashConfirmVibrationMs) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("الإعدادات", style = MaterialTheme.typography.headlineSmall)

        Column {
            Text("المظهر", style = MaterialTheme.typography.titleMedium)
            val options = listOf(
                AppPreferences.ThemeMode.LIGHT to "فاتح",
                AppPreferences.ThemeMode.DARK to "غامق",
                AppPreferences.ThemeMode.SYSTEM to "مطابق للنظام"
            )
            options.forEach { (value, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = theme == value,
                        onClick = { theme = value; prefs.themeMode = value }
                    )
                    Text(label)
                }
            }
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تفعيل الاهتزاز", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it; prefs.flashVibrationEnabled = it }
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = vibrationMs.toFloat(),
                onValueChange = { vibrationMs = it.toInt() },
                onValueChangeFinished = { prefs.flashConfirmVibrationMs = vibrationMs },
                valueRange = 0f..3000f,
                steps = 11,
                enabled = vibrationEnabled
            )
        }
    }
}
