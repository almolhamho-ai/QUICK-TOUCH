package com.example.quickgestures.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.services.ShakeDetectorService

@Composable
fun AutoCalibrationScreen(prefs: AppPreferences) {
    val context = LocalContext.current
    var shakeEnabled by remember { mutableStateOf(prefs.shakeDetectorEnabled) }
    var sensitivity by remember { mutableIntStateOf(prefs.shakeSensitivityLevel) }
    var proximityGuard by remember { mutableStateOf(prefs.proximityPocketGuardEnabled) }
    var shakeAction by remember { mutableStateOf(prefs.shakeTargetActionId) }
    var actionMenuExpanded by remember { mutableStateOf(false) }

    fun applyShakeEnabled(newEnabled: Boolean) {
        prefs.shakeDetectorEnabled = newEnabled
        val intent = Intent(context, ShakeDetectorService::class.java)
        if (newEnabled) context.startForegroundService(intent) else context.stopService(intent)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("الهزة", style = MaterialTheme.typography.headlineSmall)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تفعيل", style = MaterialTheme.typography.titleMedium)
            Switch(checked = shakeEnabled, onCheckedChange = { checked -> shakeEnabled = checked; applyShakeEnabled(checked) })
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("الإجراء")
            Box {
                TextButton(onClick = { actionMenuExpanded = true }) {
                    Text(GestureActionCatalog.byId(shakeAction)?.displayLabel ?: "اختر")
                }
                DropdownMenu(expanded = actionMenuExpanded, onDismissRequest = { actionMenuExpanded = false }) {
                    GestureActionCatalog.all.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.displayLabel) },
                            onClick = {
                                shakeAction = action.id
                                prefs.shakeTargetActionId = action.id
                                actionMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Column {
            Text("الحساسية: $sensitivity / 10")
            Slider(
                value = sensitivity.toFloat(),
                onValueChange = { sensitivity = it.toInt() },
                onValueChangeFinished = { prefs.shakeSensitivityLevel = sensitivity },
                valueRange = AppPreferences.SENSITIVITY_MIN.toFloat()..AppPreferences.SENSITIVITY_MAX.toFloat(),
                steps = (AppPreferences.SENSITIVITY_MAX - AppPreferences.SENSITIVITY_MIN) - 1
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تجاهل الهز بالجيب")
            Switch(
                checked = proximityGuard,
                onCheckedChange = { proximityGuard = it; prefs.proximityPocketGuardEnabled = it }
            )
        }
    }
}
