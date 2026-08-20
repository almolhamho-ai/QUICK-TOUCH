package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.*

@Composable
fun RoutinesScreen(
    routines: List<Routine>,
    onSave: (Routine) -> Unit,
    onDelete: (Routine) -> Unit
) {
    var editing by remember { mutableStateOf<Routine?>(null) }

    if (editing != null) {
        RoutineEditor(
            routine = editing!!,
            onCancel = { editing = null },
            onSave = { saved -> onSave(saved); editing = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الروتينات", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = {
                editing = Routine(trigger = RoutineTrigger.Shake(), actionSteps = emptyList())
            }) { Text("روتين جديد") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(routines) { routine ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(routine.resolvedName(GestureActionCatalog::byId))
                            Text("${routine.actionSteps.size} إجراء", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = routine.enabled, onCheckedChange = { onSave(routine.copy(enabled = it)) })
                        TextButton(onClick = { editing = routine }) { Text("تعديل") }
                        TextButton(onClick = { onDelete(routine) }) { Text("حذف") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineEditor(routine: Routine, onCancel: () -> Unit, onSave: (Routine) -> Unit) {
    var name by remember { mutableStateOf(routine.userGivenName ?: "") }
    var steps by remember { mutableStateOf(routine.actionSteps) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم الروتين (اختياري — إذا تركته فاضي رح يتسمى تلقائي)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("الإجراءات", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(steps) { step ->
                ActionStepEditor(
                    step = step,
                    onChange = { updated -> steps = steps.map { if (it.id == updated.id) updated else it } },
                    onRemove = { steps = steps.filterNot { it.id == step.id } }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = {
                steps = steps + ActionStep(actionId = GestureActionCatalog.all.first().id, order = steps.size)
            }) { Text("+ إضافة إجراء") }

            Row {
                TextButton(onClick = onCancel) { Text("إلغاء") }
                Button(onClick = {
                    onSave(routine.copy(userGivenName = name.ifBlank { null }, actionSteps = steps))
                }) { Text("حفظ") }
            }
        }
    }
}

@Composable
private fun ActionStepEditor(step: ActionStep, onChange: (ActionStep) -> Unit, onRemove: () -> Unit) {
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var addConditionMenuExpanded by remember { mutableStateOf(false) }
    val selectedAction = GestureActionCatalog.byId(step.actionId)

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    TextButton(onClick = { actionMenuExpanded = true }) {
                        Text(selectedAction?.displayLabel ?: "اختر إجراء")
                    }
                    DropdownMenu(expanded = actionMenuExpanded, onDismissRequest = { actionMenuExpanded = false }) {
                        GestureActionCatalog.all.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.displayLabel) },
                                onClick = { onChange(step.copy(actionId = action.id)); actionMenuExpanded = false }
                            )
                        }
                    }
                }
                TextButton(onClick = onRemove) { Text("حذف الإجراء") }
            }

            Text("شروط هذا الإجراء (لازم تتحقق كلها):", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))

            step.conditions.forEachIndexed { index, condition ->
                ConditionEditorRow(
                    condition = condition,
                    onChange = { updated ->
                        onChange(step.copy(conditions = step.conditions.toMutableList().also { it[index] = updated }))
                    },
                    onRemove = {
                        onChange(step.copy(conditions = step.conditions.filterIndexed { i, _ -> i != index }))
                    }
                )
            }

            Box {
                TextButton(onClick = { addConditionMenuExpanded = true }) { Text("+ إضافة شرط") }
                DropdownMenu(expanded = addConditionMenuExpanded, onDismissRequest = { addConditionMenuExpanded = false }) {
                    conditionTemplates().forEach { (label, factory) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onChange(step.copy(conditions = step.conditions + factory()))
                                addConditionMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun conditionTemplates(): List<Pair<String, () -> RoutineCondition>> = listOf(
    "الوقت" to { RoutineCondition.TimeRange(20 * 60, 7 * 60) },
    "الواي فاي" to { RoutineCondition.WifiState(connected = true) },
    "نسبة البطارية" to { RoutineCondition.BatteryLevel(CompareOp.LESS_THAN, 30) },
    "حالة الشحن" to { RoutineCondition.ChargingState(isCharging = true) },
    "أيام الأسبوع" to { RoutineCondition.DayOfWeek(emptySet()) },
    "وضعية الصوت" to { RoutineCondition.RingerModeState(RingerModeOption.NORMAL) },
    "السماعة" to { RoutineCondition.HeadsetState(connected = true) },
    "البلوتوث" to { RoutineCondition.BluetoothState(connected = true) },
    "اتجاه الشاشة" to { RoutineCondition.ScreenOrientationState(OrientationOption.PORTRAIT) }
)

/** محرر كل نوع شرط بقيمه الفعلية القابلة للتخصيص الكامل، مش قيم ثابتة */
@Composable
private fun ConditionEditorRow(condition: RoutineCondition, onChange: (RoutineCondition) -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(condition.shortLabel(), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onRemove) { Text("إزالة") }
            }

            when (condition) {
                is RoutineCondition.TimeRange -> TimeRangeEditor(condition, onChange)
                is RoutineCondition.WifiState -> WifiStateEditor(condition, onChange)
                is RoutineCondition.BatteryLevel -> BatteryLevelEditor(condition, onChange)
                is RoutineCondition.ChargingState -> BooleanEditor(condition.isCharging, "شاحن موصول", "شاحن غير موصول") {
                    onChange(condition.copy(isCharging = it))
                }
                is RoutineCondition.DayOfWeek -> DayOfWeekEditor(condition, onChange)
                is RoutineCondition.RingerModeState -> RingerModeEditor(condition, onChange)
                is RoutineCondition.HeadsetState -> BooleanEditor(condition.connected, "موصولة", "غير موصولة") {
                    onChange(condition.copy(connected = it))
                }
                is RoutineCondition.BluetoothState -> BooleanEditor(condition.connected, "موصول", "غير موصول") {
                    onChange(condition.copy(connected = it))
                }
                is RoutineCondition.ScreenOrientationState -> OrientationEditor(condition, onChange)
            }
        }
    }
}

@Composable
private fun TimeRangeEditor(condition: RoutineCondition.TimeRange, onChange: (RoutineCondition) -> Unit) {
    fun format(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
    Text("من ${format(condition.startMinuteOfDay)} إلى ${format(condition.endMinuteOfDay)}")
    Text("بداية", style = MaterialTheme.typography.labelSmall)
    Slider(
        value = condition.startMinuteOfDay.toFloat(),
        onValueChange = { onChange(condition.copy(startMinuteOfDay = it.toInt())) },
        valueRange = 0f..1439f
    )
    Text("نهاية", style = MaterialTheme.typography.labelSmall)
    Slider(
        value = condition.endMinuteOfDay.toFloat(),
        onValueChange = { onChange(condition.copy(endMinuteOfDay = it.toInt())) },
        valueRange = 0f..1439f
    )
}

@Composable
private fun WifiStateEditor(condition: RoutineCondition.WifiState, onChange: (RoutineCondition) -> Unit) {
    BooleanEditor(condition.connected, "متصل", "غير متصل") { onChange(condition.copy(connected = it)) }
    OutlinedTextField(
        value = condition.specificSsid ?: "",
        onValueChange = { onChange(condition.copy(specificSsid = it.ifBlank { null })) },
        label = { Text("اسم شبكة محدد (اختياري)") },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
}

@Composable
private fun BatteryLevelEditor(condition: RoutineCondition.BatteryLevel, onChange: (RoutineCondition) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val ops = listOf(CompareOp.LESS_THAN to "أقل من", CompareOp.GREATER_THAN to "أكثر من", CompareOp.EQUALS to "يساوي")
        ops.forEach { (op, label) ->
            FilterChip(
                selected = condition.op == op,
                onClick = { onChange(condition.copy(op = op)) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
    Text("${condition.percent}%")
    Slider(
        value = condition.percent.toFloat(),
        onValueChange = { onChange(condition.copy(percent = it.toInt())) },
        valueRange = 0f..100f
    )
}

@Composable
private fun DayOfWeekEditor(condition: RoutineCondition.DayOfWeek, onChange: (RoutineCondition) -> Unit) {
    val dayLabels = listOf(1 to "أحد", 2 to "اثنين", 3 to "ثلاثاء", 4 to "أربعاء", 5 to "خميس", 6 to "جمعة", 7 to "سبت")
    Row(modifier = Modifier.fillMaxWidth()) {
        dayLabels.forEach { (dayValue, label) ->
            val selected = dayValue in condition.days
            FilterChip(
                selected = selected,
                onClick = {
                    val newDays = if (selected) condition.days - dayValue else condition.days + dayValue
                    onChange(condition.copy(days = newDays))
                },
                label = { Text(label) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun RingerModeEditor(condition: RoutineCondition.RingerModeState, onChange: (RoutineCondition) -> Unit) {
    val options = listOf(RingerModeOption.SILENT to "صامت", RingerModeOption.VIBRATE to "اهتزاز", RingerModeOption.NORMAL to "عادي")
    Row {
        options.forEach { (mode, label) ->
            FilterChip(
                selected = condition.mode == mode,
                onClick = { onChange(condition.copy(mode = mode)) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun OrientationEditor(condition: RoutineCondition.ScreenOrientationState, onChange: (RoutineCondition) -> Unit) {
    val options = listOf(OrientationOption.PORTRAIT to "عمودي", OrientationOption.LANDSCAPE to "أفقي")
    Row {
        options.forEach { (orientation, label) ->
            FilterChip(
                selected = condition.orientation == orientation,
                onClick = { onChange(condition.copy(orientation = orientation)) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun BooleanEditor(value: Boolean, trueLabel: String, falseLabel: String, onChange: (Boolean) -> Unit) {
    Row {
        FilterChip(selected = value, onClick = { onChange(true) }, label = { Text(trueLabel) }, modifier = Modifier.padding(end = 4.dp))
        FilterChip(selected = !value, onClick = { onChange(false) }, label = { Text(falseLabel) })
    }
}
