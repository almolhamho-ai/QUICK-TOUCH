package com.example.quickgestures.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.services.edge.EdgeGestureService
import com.example.quickgestures.services.edge.EdgeGestureShape

@Composable
fun EdgeGestureScreen(prefs: AppPreferences) {
    val context = LocalContext.current
    var mapping by remember { mutableStateOf(prefs.edgeGestureActionMapping) }
    var enabled by remember { mutableStateOf(prefs.edgeGestureEnabled) }

    fun applyEnabledState(newEnabled: Boolean) {
        prefs.edgeGestureEnabled = newEnabled
        val intent = Intent(context, EdgeGestureService::class.java)
        if (newEnabled) {
            if (!Settings.canDrawOverlays(context)) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
                return
            }
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }

    val shapes = listOf(
        Triple(EdgeGestureShape.STRAIGHT_LINE, "خط مستقيم", "اسحب إصبعك من حافة الشاشة للداخل بخط مستقيم واحد بدون توقف."),
        Triple(EdgeGestureShape.L_CORNER, "زاوية L", "اسحب من الحافة للداخل، وبمنتصف الطريق غيّر الاتجاه فجأة (فوق أو تحت) بزاوية حادة."),
        Triple(EdgeGestureShape.HALF_CIRCLE, "نص دائرة", "اسحب من الحافة برسم منحنى نص دائرة وترجع قريب من نقطة البداية.")
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollStateCompat())) {
        Text("إيماءات الحافة", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تفعيل إيماءات الحافة", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = enabled,
                enabled = mapping.isNotEmpty(),
                onCheckedChange = { checked -> enabled = checked; applyEnabledState(checked) }
            )
        }

        Spacer(Modifier.height(20.dp))

        shapes.forEach { (shape, label, description) ->
            var expanded by remember { mutableStateOf(false) }
            val selectedActionId = mapping[shape.name]
            val selectedLabel = GestureActionCatalog.byId(selectedActionId ?: "")?.displayLabel ?: "بدون ربط"

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EdgeGestureDemo(shape = shape)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(description, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Box {
                        TextButton(onClick = { expanded = true }) { Text("الإجراء: $selectedLabel") }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            GestureActionCatalog.all.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.displayLabel) },
                                    onClick = {
                                        mapping = mapping + (shape.name to action.id)
                                        prefs.edgeGestureActionMapping = mapping
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ديمو حركي بسيط يعيد رسم شكل الإيماءة بشكل متكرر ليفهم المستخدم حركة اليد المطلوبة عملياً */
@Composable
private fun EdgeGestureDemo(shape: EdgeGestureShape) {
    val infiniteTransition = rememberInfiniteTransition(label = "edgeDemo")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edgeDemoProgress"
    )

    Canvas(modifier = Modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        val strokeColor = Color(0xFF6C4DFF)

        val fullPath = when (shape) {
            EdgeGestureShape.STRAIGHT_LINE -> listOf(Offset(0f, h / 2f), Offset(w, h / 2f))
            EdgeGestureShape.L_CORNER -> listOf(Offset(0f, h * 0.2f), Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.9f))
            EdgeGestureShape.HALF_CIRCLE -> (0..20).map { i ->
                val t = i / 20f
                val angle = Math.PI * t
                Offset(
                    (w * 0.1f + (w * 0.8f) * (kotlin.math.sin(angle).toFloat())),
                    h * 0.1f + h * 0.8f * (1f - kotlin.math.cos(angle).toFloat()) / 2f
                )
            }
        }

        val visibleCount = (fullPath.size * progress).toInt().coerceIn(1, fullPath.size)
        val visiblePath = fullPath.take(visibleCount)

        for (i in 0 until visiblePath.size - 1) {
            drawLine(
                color = strokeColor,
                start = visiblePath[i],
                end = visiblePath[i + 1],
                strokeWidth = 6f
            )
        }
        if (visiblePath.isNotEmpty()) {
            drawCircle(color = strokeColor, radius = 8f, center = visiblePath.last())
        }
    }
}

@Composable
private fun rememberScrollStateCompat() = androidx.compose.foundation.rememberScrollState()
