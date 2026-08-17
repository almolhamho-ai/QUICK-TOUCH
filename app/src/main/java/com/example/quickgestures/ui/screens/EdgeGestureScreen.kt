package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.EdgeShape
import com.example.quickgestures.data.GestureAction

@Composable
fun EdgeGestureScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    var enabled by remember { mutableStateOf(prefs.edgeGestureEnabled) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("إيماءات الحافة", style = MaterialTheme.typography.titleLarge)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تفعيل إيماءات الحافة")
            Switch(checked = enabled, onCheckedChange = {
                enabled = it; prefs.edgeGestureEnabled = it; onStateChanged()
            })
        }

        Text("اسحب من حافة الشاشة اليمين أو اليسار وارسم أحد الأشكال التالية، وحدد الإجراء المرتبط فيه:",
            style = MaterialTheme.typography.bodyMedium)

        EdgeShape.entries.forEach { shape ->
            var expanded by remember { mutableStateOf(false) }
            var current by remember { mutableStateOf(prefs.getEdgeMapping(shape)) }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(shapeLabel(shape), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(current?.label ?: "اختر إجراء")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            GestureAction.entries.forEach { action ->
                                DropdownMenuItem(text = { Text(action.label) }, onClick = {
                                    current = action
                                    prefs.setEdgeMapping(shape, action)
                                    onStateChanged()
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shapeLabel(shape: EdgeShape) = when (shape) {
    EdgeShape.LINE -> "خط مستقيم"
    EdgeShape.CORNER_L -> "زاوية (L)"
    EdgeShape.HALF_CIRCLE -> "نص دائرة"
}
