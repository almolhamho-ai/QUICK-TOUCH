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
import com.example.quickgestures.services.backtap.BackTapDetectorService

@Composable
fun BackTapScreen(prefs: AppPreferences) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(prefs.backTapEnabled) }
    var doubleAction by remember { mutableStateOf(prefs.backTapDoubleActionId) }
    var tripleAction by remember { mutableStateOf(prefs.backTapTripleActionId) }

    fun applyEnabled(newEnabled: Boolean) {
        prefs.backTapEnabled = newEnabled
        val intent = Intent(context, BackTapDetectorService::class.java)
        if (newEnabled) context.startForegroundService(intent) else context.stopService(intent)
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("النقر على ظهر الهاتف", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تفعيل", style = MaterialTheme.typography.titleMedium)
            Switch(checked = enabled, onCheckedChange = { enabled = it; applyEnabled(it) })
        }

        Spacer(Modifier.height(20.dp))
        ActionPickerRow(
            label = "النقر مرتين",
            selectedActionId = doubleAction,
            onSelect = { doubleAction = it; prefs.backTapDoubleActionId = it }
        )
        Spacer(Modifier.height(12.dp))
        ActionPickerRow(
            label = "النقر ثلاث مرات",
            selectedActionId = tripleAction,
            onSelect = { tripleAction = it; prefs.backTapTripleActionId = it }
        )
    }
}

@Composable
private fun ActionPickerRow(label: String, selectedActionId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = GestureActionCatalog.byId(selectedActionId ?: "")?.displayLabel ?: "بدون ربط"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
        Box {
            TextButton(onClick = { expanded = true }) { Text(selectedLabel) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                GestureActionCatalog.all.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.displayLabel) },
                        onClick = { onSelect(action.id); expanded = false }
                    )
                }
            }
        }
    }
}
