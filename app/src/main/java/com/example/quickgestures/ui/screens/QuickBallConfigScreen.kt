package com.example.quickgestures.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureAction

@Composable
fun QuickBallConfigScreen(prefs: AppPreferences, onStateChanged: () -> Unit) {
    var selected by remember { mutableStateOf(prefs.quickBallActions.toSet()) }
    var outsideApp by remember { mutableStateOf(prefs.quickBallWorksOutsideApp) }
    var enabled by remember { mutableStateOf(prefs.quickBallEnabled) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("إعدادات الكرة العائمة", style = MaterialTheme.typography.titleLarge) }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تفعيل الكرة العائمة")
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it; prefs.quickBallEnabled = it; onStateChanged()
                })
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("تعمل خارج التطبيق أيضاً")
                    Text("تحتاج صلاحية الظهور فوق التطبيقات الأخرى", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = outsideApp, onCheckedChange = {
                    outsideApp = it; prefs.quickBallWorksOutsideApp = it; onStateChanged()
                })
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("اختر الإجراءات يلي بدك تظهر بالكرة (فيك تختار أي عدد، وبتدوّر بينهن بسحبة على الكرة نفسها):",
                style = MaterialTheme.typography.bodyMedium)
        }

        items(GestureAction.entries) { action ->
            val isChecked = action in selected
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action.label)
                Checkbox(checked = isChecked, onCheckedChange = { checked ->
                    selected = if (checked) selected + action else selected - action
                    prefs.quickBallActions = selected.toList()
                    onStateChanged()
                })
            }
        }
    }
}
