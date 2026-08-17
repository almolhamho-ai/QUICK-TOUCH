package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.*
import java.util.UUID

@Composable
fun RoutinesScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    var routines by remember { mutableStateOf(prefs.routines) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, contentDescription = "إضافة روتين") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("الروتينات (شروط + إجراءات تلقائية)", style = MaterialTheme.typography.titleLarge) }
            if (routines.isEmpty()) {
                item { Text("ما في روتينات لسا. دوس + لإضافة واحد.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(routines) { routine ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(routine.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "المُشغّل: ${triggerLabel(routine.trigger.type)} · ${routine.actions.size} إجراء",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = routine.enabled, onCheckedChange = { checked ->
                            prefs.addOrUpdateRoutine(routine.copy(enabled = checked))
                            routines = prefs.routines
                            onStateChanged()
                        })
                        IconButton(onClick = {
                            prefs.deleteRoutine(routine.id)
                            routines = prefs.routines
                            onStateChanged()
                        }) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
                    }
                }
            }
        }
    }

    if (showDialog) {
        NewRoutineDialog(
            onDismiss = { showDialog = false },
            onCreate = { routine ->
                prefs.addOrUpdateRoutine(routine)
                routines = prefs.routines
                onStateChanged()
                showDialog = false
            }
        )
    }
}

private fun triggerLabel(type: TriggerType) = when (type) {
    TriggerType.SHAKE -> "هز الهاتف"
    TriggerType.EDGE_GESTURE -> "إيماءة حافة"
    TriggerType.APP_OPENED -> "فتح تطبيق"
    TriggerType.TIME_OF_DAY -> "وقت محدد"
    TriggerType.MANUAL -> "يدوي"
}

@Composable
private fun NewRoutineDialog(onDismiss: () -> Unit, onCreate: (Routine) -> Unit) {
    var name by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf(TriggerType.SHAKE) }
    var selectedActions by remember { mutableStateOf(setOf<GestureAction>()) }
    var triggerExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("روتين جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الروتين") })

                Box {
                    OutlinedButton(onClick = { triggerExpanded = true }) { Text("المُشغّل: ${triggerLabel(triggerType)}") }
                    DropdownMenu(expanded = triggerExpanded, onDismissRequest = { triggerExpanded = false }) {
                        TriggerType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(triggerLabel(t)) }, onClick = { triggerType = t; triggerExpanded = false })
                        }
                    }
                }

                Text("الإجراءات:", style = MaterialTheme.typography.bodyMedium)
                GestureAction.entries.forEach { action ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = action in selectedActions,
                            onCheckedChange = { checked ->
                                selectedActions = if (checked) selectedActions + action else selectedActions - action
                            }
                        )
                        Text(action.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && selectedActions.isNotEmpty()) {
                    onCreate(
                        Routine(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            trigger = RoutineTrigger(triggerType),
                            actions = selectedActions.map { GestureActionRef(it) }
                        )
                    )
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
