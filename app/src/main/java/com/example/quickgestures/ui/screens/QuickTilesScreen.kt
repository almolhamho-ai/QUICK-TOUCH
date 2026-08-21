package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog

@Composable
fun QuickTilesScreen(prefs: AppPreferences) {
    var tile1 by remember { mutableStateOf(prefs.quickTileAction1Id) }
    var tile2 by remember { mutableStateOf(prefs.quickTileAction2Id) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("بلاطات مركز التحكم", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "بعد ما تحدد الإجراء تحت، أضف البلاطة يدوياً مرة وحدة من قائمة تعديل مركز التحكم بالهاتف.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(20.dp))

        TilePickerRow(label = "البلاطة الأولى", selectedActionId = tile1, onSelect = { tile1 = it; prefs.quickTileAction1Id = it })
        Spacer(Modifier.height(16.dp))
        TilePickerRow(label = "البلاطة الثانية", selectedActionId = tile2, onSelect = { tile2 = it; prefs.quickTileAction2Id = it })
    }
}

@Composable
private fun TilePickerRow(label: String, selectedActionId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = GestureActionCatalog.byId(selectedActionId ?: "")?.displayLabel ?: "بدون ربط"

    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
