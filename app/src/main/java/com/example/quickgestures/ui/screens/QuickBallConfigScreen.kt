package com.example.quickgestures.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.data.QuickBallRadialConfig
import com.example.quickgestures.services.floating.FloatingBallService

@Composable
fun QuickBallConfigScreen(prefs: AppPreferences) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(prefs.quickBallRadialConfig) }
    var enabled by remember { mutableStateOf(prefs.quickBallEnabled) }
    var mode by remember { mutableStateOf(prefs.quickBallMode) }

    fun update(newConfig: QuickBallRadialConfig) {
        config = newConfig
        prefs.quickBallRadialConfig = newConfig
    }

    fun applyEnabledState(newEnabled: Boolean, newMode: AppPreferences.QuickBallMode) {
        prefs.quickBallEnabled = newEnabled
        prefs.quickBallMode = newMode

        val intent = Intent(context, FloatingBallService::class.java)
        if (newEnabled && newMode == AppPreferences.QuickBallMode.SYSTEM_WIDE_OVERLAY) {
            if (!Settings.canDrawOverlays(context)) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
                return
            }
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("الكرة العائمة", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text("تفعيل الكرة", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (config.selectedActionIds.isEmpty())
                        "اختر اختصار واحد على الأقل تحت قبل التفعيل"
                    else "الكرة بتشتغل مباشرة بعد التفعيل",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = enabled,
                enabled = config.selectedActionIds.isNotEmpty(),
                onCheckedChange = { checked ->
                    enabled = checked
                    applyEnabledState(checked, mode)
                }
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("وضعية العمل", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = mode == AppPreferences.QuickBallMode.IN_APP_ONLY,
                onClick = {
                    mode = AppPreferences.QuickBallMode.IN_APP_ONLY
                    if (enabled) applyEnabledState(true, mode) else prefs.quickBallMode = mode
                }
            )
            Text("جوا التطبيق بس")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = mode == AppPreferences.QuickBallMode.SYSTEM_WIDE_OVERLAY,
                onClick = {
                    mode = AppPreferences.QuickBallMode.SYSTEM_WIDE_OVERLAY
                    if (enabled) applyEnabledState(true, mode) else prefs.quickBallMode = mode
                }
            )
            Text("تعمل برا التطبيق كمان (Overlay)")
        }

        Spacer(Modifier.height(16.dp))
        Text("اختر الاختصارات اللي بدك تظهر بالكرة الدائرية", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "بتترتب كفقاعات صغيرة حول دائرة مركزية بنفس المقاس، وإذا زاد العدد عن ${config.itemsPerRing} بتقدر تدوّرها بسحبة على المركز.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(GestureActionCatalog.all) { action ->
                val selected = action.id in config.selectedActionIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked ->
                            val newIds = if (checked) {
                                config.selectedActionIds + action.id
                            } else {
                                config.selectedActionIds - action.id
                            }
                            update(config.copy(selectedActionIds = newIds))
                        }
                    )
                    Text(action.displayLabel)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("عدد الاختصارات بالحلقة الواحدة قبل التدوير: ${config.itemsPerRing}")
        Slider(
            value = config.itemsPerRing.toFloat(),
            onValueChange = { update(config.copy(itemsPerRing = it.toInt())) },
            valueRange = 3f..10f,
            steps = 6
        )
    }
}
